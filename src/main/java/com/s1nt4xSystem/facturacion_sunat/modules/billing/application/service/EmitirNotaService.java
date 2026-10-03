package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.validation.DocumentValidator;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in.EmitirNotaUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.CatalogoFiscalPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.CorrelativoServicePort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.DocumentoRepositoryPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.service.CalculadoraFiscalSunat;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.service.CalculadoraFiscalSunat.CalculoLineaInput;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.service.CalculadoraFiscalSunat.ResultadoCalculo;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.service.ValidadorReglasFiscales;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.*;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaConfigRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.CertificadoDigitalService;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.validation.CompanyValidator;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EmitirNotaService implements EmitirNotaUseCase {

    private final DocumentoRepositoryPort documentoRepositoryPort;
    private final CorrelativoServicePort correlativoServicePort;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final SerieRepository serieRepository;
    private final CatalogoFiscalPort catalogoFiscalPort;
    private final ProcesadorComprobanteElectronicoService procesadorComprobanteElectronicoService;
    private final EmpresaConfigRepository empresaConfigRepository;
    private final CertificadoDigitalService certificadoDigitalService;
    private final CompanyValidator companyValidator;
    private final DocumentValidator documentValidator;

    private final CalculadoraFiscalSunat calculadoraFiscalSunat = new CalculadoraFiscalSunat();
    private final ValidadorReglasFiscales validadorReglasFiscales = new ValidadorReglasFiscales();

    @Autowired
    public EmitirNotaService(
            DocumentoRepositoryPort documentoRepositoryPort,
            CorrelativoServicePort correlativoServicePort,
            EmpresaRepository empresaRepository,
            SucursalRepository sucursalRepository,
            SerieRepository serieRepository,
            CatalogoFiscalPort catalogoFiscalPort,
            ProcesadorComprobanteElectronicoService procesadorComprobanteElectronicoService,
            EmpresaConfigRepository empresaConfigRepository,
            CertificadoDigitalService certificadoDigitalService,
            CompanyValidator companyValidator,
            DocumentValidator documentValidator) {
        this.documentoRepositoryPort = documentoRepositoryPort;
        this.correlativoServicePort = correlativoServicePort;
        this.empresaRepository = empresaRepository;
        this.sucursalRepository = sucursalRepository;
        this.serieRepository = serieRepository;
        this.catalogoFiscalPort = catalogoFiscalPort;
        this.procesadorComprobanteElectronicoService = procesadorComprobanteElectronicoService;
        this.empresaConfigRepository = empresaConfigRepository;
        this.certificadoDigitalService = certificadoDigitalService;
        this.companyValidator = companyValidator;
        this.documentValidator = documentValidator;
    }

    public EmitirNotaService(
            DocumentoRepositoryPort documentoRepositoryPort,
            CorrelativoServicePort correlativoServicePort,
            EmpresaRepository empresaRepository,
            SucursalRepository sucursalRepository,
            SerieRepository serieRepository,
            CatalogoFiscalPort catalogoFiscalPort,
            ProcesadorComprobanteElectronicoService procesadorComprobanteElectronicoService,
            EmpresaConfigRepository empresaConfigRepository,
            CertificadoDigitalService certificadoDigitalService,
            CompanyValidator companyValidator) {
        this(documentoRepositoryPort, correlativoServicePort, empresaRepository, sucursalRepository,
                serieRepository, catalogoFiscalPort, procesadorComprobanteElectronicoService,
                empresaConfigRepository, certificadoDigitalService, companyValidator,
                new DocumentValidator(empresaRepository, empresaConfigRepository, sucursalRepository, serieRepository, companyValidator));
    }

    @Override
    @Transactional
    public ComprobanteFiscal emitirNota(CrearNotaRequest request) {
        NotaDataRequest notaReq = request.getNota();
        DocumentoReferenciaRequest refReq = request.getDocumentoReferencia();

        // 1. Idempotencia
        if (notaReq.getClaveIdempotencia() != null && !notaReq.getClaveIdempotencia().trim().isEmpty()) {
            Optional<ComprobanteFiscal> previo = documentoRepositoryPort.buscarPorClaveIdempotencia(notaReq.getClaveIdempotencia().trim());
            if (previo.isPresent()) {
                log.info("Idempotencia detectada en nota para clave '{}'", notaReq.getClaveIdempotencia());
                return previo.get();
            }
        }

        // 2. Obtener Empresa del Tenant actual
        Empresa empresa = documentValidator.validateAndGetEmpresaTenant();
        documentValidator.validateEmisorRucCoincide(
                request.getEmisor() != null ? request.getEmisor().getRuc() : null, empresa);

        // 3. Tipo de nota (07 = NC, 08 = ND)
        String tipoNota = notaReq.getTipo();
        documentValidator.validateTipoNota(tipoNota);
        TipoComprobante tipoEnum = TipoComprobante.fromCodigo(tipoNota);
        if (notaReq.getSerie() != null && !notaReq.getSerie().isBlank()) {
            documentValidator.validateSerieFormato(tipoEnum, notaReq.getSerie());
        }
        documentValidator.validateRequisitosFiscales(empresa, tipoEnum);

        // 4. Resolver Serie y Sucursal
        String codSucursalEmisor = request.getEmisor() != null ? request.getEmisor().getCodigoSucursal() : null;
        Serie serieEntity = documentValidator.validateAndResolveSerieNota(tipoNota, notaReq.getSerie(), codSucursalEmisor);
        Sucursal sucursal = serieEntity.getSucursal();

        // 5. Resolver Comprobante de Referencia
        Optional<ComprobanteFiscal> docRefInternoOpt = Optional.empty();
        if (refReq.getDocumentoReferenciadoId() != null) {
            docRefInternoOpt = documentoRepositoryPort.buscarPorId(refReq.getDocumentoReferenciadoId());
            if (docRefInternoOpt.isEmpty()) {
                throw new BusinessException(ErrorCode.DOCUMENT_REF_NOT_FOUND, refReq.getDocumentoReferenciadoId());
            }
        }

        // 6. Resolver Datos del Cliente
        ClienteDto clienteDto = request.getCliente();
        if (clienteDto == null && docRefInternoOpt.isPresent()) {
            ComprobanteFiscal docOrig = docRefInternoOpt.get();
            clienteDto = ClienteDto.builder()
                    .tipoDocumento(docOrig.getClienteTipoDoc())
                    .numeroDocumento(docOrig.getClienteNumeroDoc())
                    .nombreRazonSocial(docOrig.getClienteNombre())
                    .direccion(docOrig.getClienteDireccion())
                    .build();
        }
        documentValidator.validateClientePresent(clienteDto);

        // 7. Resolver Moneda
        String moneda = notaReq.getMoneda();
        if (moneda == null && docRefInternoOpt.isPresent()) {
            moneda = docRefInternoOpt.get().getMoneda();
        }
        if (moneda == null || moneda.isBlank()) {
            moneda = refReq.getMonedaRef() != null ? refReq.getMonedaRef() : "PEN";
        }

        // 8. Resolver Ítems
        boolean empresaIncluidoTributo = Boolean.TRUE.equals(empresa.getIncluidoTributo());
        List<CalculoLineaInput> inputs = new ArrayList<>();
        int itemIndex = 1;

        if ((request.getItems() == null || request.getItems().isEmpty()) && docRefInternoOpt.isPresent()) {
            // Nota de Crédito TOTAL sobre documento interno: Se clonan los ítems originales
            ComprobanteFiscal docOrig = docRefInternoOpt.get();
            for (LineaComprobante origLine : docOrig.getDetalles()) {
                inputs.add(new CalculoLineaInput(
                        itemIndex++,
                        origLine.getCodigoProducto(),
                        origLine.getDescripcion(),
                        origLine.getCantidad(),
                        origLine.getUnidadMedida(),
                        origLine.getValorUnitario(),
                        origLine.getPrecioUnitario(),
                        origLine.getTipoAfectacion(),
                        origLine.getDescuento(),
                        origLine.getTributos().stream()
                                .filter(t -> "7152".equals(t.getCodigoTributo()))
                                .map(t -> t.getCantidadBase() != null ? t.getCantidadBase().intValue() : 0)
                                .findFirst().orElse(0)
                ));
            }
        } else if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (ItemComprobanteRequest itemReq : request.getItems()) {
                TipoAfectacionIgv afectacion = TipoAfectacionIgv.fromCodigo(itemReq.getCodigoAfectacionSunat());

                BigDecimal valorUnitario = null;
                BigDecimal precioUnitario = null;
                BigDecimal valor = itemReq.getValor();
                documentValidator.validateItemValor(valor, itemReq.getDescripcion());

                if (empresaIncluidoTributo) {
                    precioUnitario = valor;
                } else {
                    valorUnitario = valor;
                }

                inputs.add(new CalculoLineaInput(
                        itemIndex++,
                        itemReq.getCodigoProducto(),
                        itemReq.getDescripcion(),
                        itemReq.getCantidad(),
                        itemReq.getUnidadMedida(),
                        valorUnitario,
                        precioUnitario,
                        afectacion,
                        itemReq.getDescuento(),
                        Boolean.TRUE.equals(itemReq.getIcbper()) ? itemReq.getCantidad().intValue() : 0
                ));
            }
        } else {
            throw new BusinessException(ErrorCode.DOCUMENT_ITEMS_EMPTY);
        }

        // 9. Motor de Cálculo Tributario
        BigDecimal tasaIgv = catalogoFiscalPort.determinarTasaIgvPorSucursal(sucursal);
        BigDecimal tasaIcbper = catalogoFiscalPort.obtenerTasaIcbperVigente();
        CalculadoraFiscalSunat.ConfiguracionTasas configTasas = new CalculadoraFiscalSunat.ConfiguracionTasas(tasaIgv, tasaIcbper);

        ResultadoCalculo calculo = calculadoraFiscalSunat.calcular(inputs, BigDecimal.ZERO, configTasas);

        // 10. Armar ReferenciaComprobante
        ReferenciaComprobante referencia;
        if (docRefInternoOpt.isPresent()) {
            ComprobanteFiscal docOrig = docRefInternoOpt.get();
            referencia = ReferenciaComprobante.builder()
                    .tipoRelacion(refReq.getTipoRelacion() != null ? refReq.getTipoRelacion() : "afecta")
                    .documentoReferenciadoId(docOrig.getId())
                    .tipoDocumentoRef(docOrig.getTipoComprobante().getCodigo())
                    .serieRef(docOrig.getSerie())
                    .numeroRef(docOrig.getNumero())
                    .motivoCodigo(notaReq.getMotivoCodigo())
                    .motivoDescripcion(notaReq.getMotivoDescripcion())
                    .fechaEmisionRef(docOrig.getFechaEmision() != null ? docOrig.getFechaEmision().toLocalDate() : null)
                    .build();
        } else {
            referencia = ReferenciaComprobante.builder()
                    .tipoRelacion(refReq.getTipoRelacion() != null ? refReq.getTipoRelacion() : "afecta")
                    .documentoReferenciadoId(null)
                    .tipoDocumentoRef(refReq.getTipoDocumentoRef())
                    .serieRef(refReq.getSerieRef())
                    .numeroRef(refReq.getNumeroRef())
                    .motivoCodigo(notaReq.getMotivoCodigo())
                    .motivoDescripcion(notaReq.getMotivoDescripcion())
                    .fechaEmisionRef(refReq.getFechaEmisionRef())
                    .build();
        }

        // 11. Asignar Correlativo
        Integer correlativo;
        if (notaReq.getNumero() != null && notaReq.getNumero() > 0) {
            correlativo = notaReq.getNumero();
            Serie serieBloqueada = serieRepository.findForUpdate(sucursal.getId(), tipoNota, serieEntity.getSerie())
                    .orElse(serieEntity);
            if (serieBloqueada.getCorrelativo() == null || correlativo > serieBloqueada.getCorrelativo()) {
                serieBloqueada.setCorrelativo(correlativo);
                serieRepository.save(serieBloqueada);
            }
        } else {
            correlativo = correlativoServicePort.obtenerSiguienteCorrelativo(
                    sucursal.getId(), tipoNota, serieEntity.getSerie());
        }

        LocalDateTime fechaEmision = notaReq.getFechaEmision() != null
                ? notaReq.getFechaEmision() : LocalDateTime.now();

        // 12. Construir Aggregate Root de la Nota
        ComprobanteFiscal nota = ComprobanteFiscal.builder()
                .claveIdempotencia(notaReq.getClaveIdempotencia())
                .empresaId(empresa.getId())
                .sucursalId(sucursal.getId())
                .codigoSucursal(sucursal.getCodigo())
                .serieId(serieEntity.getId())
                .tipoComprobante(tipoEnum)
                .serie(serieEntity.getSerie())
                .numero(correlativo)
                .fechaEmision(fechaEmision)
                .moneda(moneda)
                .tipoOperacion("0101")
                .clienteTipoDoc(clienteDto.getTipoDocumento())
                .clienteNumeroDoc(clienteDto.getNumeroDocumento())
                .clienteNombre(clienteDto.getNombreRazonSocial())
                .clienteDireccion(clienteDto.getDireccion())
                .formaPago("CONTADO")
                .totalOtrosCargos(BigDecimal.ZERO)
                .totalTributos(calculo.totalTributos())
                .subtotal(calculo.totalValorVenta())
                .total(calculo.importeTotal())
                .totalDescuentos(calculo.totalDescuentos())
                .estadoInterno("REGISTRADO")
                .payloadEntrada(request.getRawPayload())
                .detalles(calculo.lineas())
                .totalesAfectacion(calculo.totalesAfectacion())
                .tributosGlobales(calculo.tributosGlobales())
                .cuotas(List.of())
                .detraccion(null)
                .referencias(List.of(referencia))
                .build();

        // Validar reglas fiscales de la nota (referencias, etc.)
        validadorReglasFiscales.validar(nota);

        ComprobanteFiscal guardado = documentoRepositoryPort.guardar(nota);

        // Fase electrónica: Generación XML UBL, Firma XMLDSig y Envío SUNAT
        ComprobanteFiscal finalizado = procesadorComprobanteElectronicoService.procesarFirmaYEnvio(guardado, empresa, sucursal);
        log.info("Nota electrónica emitida: {} - {} (ID: {})",
                finalizado.getSerie(), finalizado.getNumero(), finalizado.getId());
        return finalizado;
    }
}
