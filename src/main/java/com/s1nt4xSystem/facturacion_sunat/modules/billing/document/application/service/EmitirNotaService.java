package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in.EmitirNotaUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.CatalogoFiscalPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.CorrelativoServicePort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.DocumentoRepositoryPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service.CalculadoraFiscalSunat;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service.CalculadoraFiscalSunat.CalculoLineaInput;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service.CalculadoraFiscalSunat.ResultadoCalculo;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.ResourceNotFoundException;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaConfigRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.CertificadoDigitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

    private final CalculadoraFiscalSunat calculadoraFiscalSunat = new CalculadoraFiscalSunat();

    private void prevalidarRequisitosFiscales(Empresa empresa, TipoComprobante tipoComprobante) {
        if (!tipoComprobante.isEsElectronico()) {
            return;
        }

        if (empresa.getRuc() == null || empresa.getRuc().isBlank()
                || empresa.getRazonSocial() == null || empresa.getRazonSocial().isBlank()) {
            throw new DomainException("Faltan datos de configuración de la empresa");
        }

        Optional<EmpresaConfig> configOpt = empresaConfigRepository.findByEmpresaId(empresa.getId());
        if (configOpt.isEmpty()) {
            throw new DomainException("Faltan datos de configuración de la empresa");
        }
        EmpresaConfig config = configOpt.get();

        if (config.getUserSol() == null || config.getUserSol().isBlank()
                || config.getPassSol() == null || config.getPassSol().isBlank()) {
            throw new DomainException("Faltan datos de configuración de la empresa");
        }

        if (config.getCertificado() == null || config.getCertificado().isBlank()
                || !certificadoDigitalService.existeCertificado(config.getCertificado())) {
            throw new DomainException("Falta certificado para firmar el comprobante");
        }
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
                log.info("Idempotencia en nota '{}': Devolviendo nota existente {} - {}",
                        notaReq.getClaveIdempotencia(), previo.get().getSerie(), previo.get().getNumero());
                return previo.get();
            }
        }

        // 2. Obtener Empresa del Tenant actual
        Empresa empresa = empresaRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe ninguna empresa configurada en el tenant"));

        if (request.getEmisor() != null && request.getEmisor().getRuc() != null && !request.getEmisor().getRuc().isBlank()) {
            if (!empresa.getRuc().equalsIgnoreCase(request.getEmisor().getRuc().trim())) {
                throw new DomainException(String.format("El RUC emisor enviado (%s) no coincide con el RUC de la empresa (%s)",
                        request.getEmisor().getRuc(), empresa.getRuc()));
            }
        }

        // 3. Tipo de nota (07 = NC, 08 = ND)
        String tipoNota = notaReq.getTipo();
        if (!"07".equals(tipoNota) && !"08".equals(tipoNota)) {
            throw new DomainException("El tipo de nota debe ser 07 (Nota de Crédito) u 08 (Nota de Débito)");
        }
        TipoComprobante tipoEnum = TipoComprobante.fromCodigo(tipoNota);
        if (notaReq.getSerie() != null && !notaReq.getSerie().isBlank()) {
            tipoEnum.validarSerie(notaReq.getSerie().trim());
        }
        prevalidarRequisitosFiscales(empresa, tipoEnum);

        // 4. Resolver Serie y Sucursal
        Serie serieEntity;
        if (notaReq.getSerie() != null && !notaReq.getSerie().isBlank()) {
            serieEntity = serieRepository.findByTipoComprobanteAndSerie(tipoNota, notaReq.getSerie().trim())
                    .orElseThrow(() -> new DomainException(
                            String.format("La serie '%s' para tipo de nota '%s' no está configurada en la empresa",
                                    notaReq.getSerie(), tipoNota)));
        } else {
            String codSucursal = (request.getEmisor() != null && request.getEmisor().getCodigoSucursal() != null)
                    ? request.getEmisor().getCodigoSucursal().trim() : "0000";
            Sucursal sucursalTemp = sucursalRepository.findByCodigo(codSucursal)
                    .orElseGet(() -> sucursalRepository.findByActivoTrue().stream().findFirst()
                            .orElseThrow(() -> new DomainException("No se encontró sucursal activa con código: " + codSucursal)));
            serieEntity = serieRepository.findFirstBySucursalIdAndTipoComprobante(sucursalTemp.getId(), tipoNota)
                    .orElseThrow(() -> new DomainException(
                            String.format("No existe ninguna serie activa configurada para el tipo de nota '%s'", tipoNota)));
            tipoEnum.validarSerie(serieEntity.getSerie());
        }
        Sucursal sucursal = serieEntity.getSucursal();

        // 6. Resolver Comprobante de Referencia
        Optional<ComprobanteFiscal> docRefInternoOpt = Optional.empty();
        if (refReq.getDocumentoReferenciadoId() != null) {
            docRefInternoOpt = documentoRepositoryPort.buscarPorId(refReq.getDocumentoReferenciadoId());
            if (docRefInternoOpt.isEmpty()) {
                throw new ResourceNotFoundException("No se encontró el documento original con ID: " + refReq.getDocumentoReferenciadoId());
            }
        }

        // 7. Resolver Datos del Cliente
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
        if (clienteDto == null) {
            throw new DomainException("Debe proporcionar los datos del cliente para la nota");
        }

        // 8. Resolver Moneda
        String moneda = notaReq.getMoneda();
        if (moneda == null && docRefInternoOpt.isPresent()) {
            moneda = docRefInternoOpt.get().getMoneda();
        }
        if (moneda == null || moneda.isBlank()) {
            moneda = refReq.getMonedaRef() != null ? refReq.getMonedaRef() : "PEN";
        }

        // 9. Resolver Ítems (Anulación Total vs Parcial vs Externa)
        boolean empresaIncluidoTributo = Boolean.TRUE.equals(empresa.getIncluidoTributo());
        List<CalculoLineaInput> inputs = new ArrayList<>();
        int itemIndex = 1;

        if ((request.getItems() == null || request.getItems().isEmpty()) && docRefInternoOpt.isPresent()) {
            // Caso: Nota de Crédito TOTAL sobre documento interno: Se clonan los ítems originales
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
            // Caso: Ítems enviados explícitamente (Parcial o Externa)
            for (ItemComprobanteRequest itemReq : request.getItems()) {
                TipoAfectacionIgv afectacion = TipoAfectacionIgv.fromCodigo(itemReq.getCodigoAfectacionSunat());

                BigDecimal valorUnitario = null;
                BigDecimal precioUnitario = null;
                BigDecimal valor = itemReq.getValor();

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
            throw new DomainException("Debe proporcionar al menos un ítem para la nota");
        }

        // 10. Motor de Cálculo Tributario
        BigDecimal tasaIgv = catalogoFiscalPort.determinarTasaIgvPorSucursal(sucursal);
        BigDecimal tasaIcbper = catalogoFiscalPort.obtenerTasaIcbperVigente();
        CalculadoraFiscalSunat.ConfiguracionTasas configTasas = new CalculadoraFiscalSunat.ConfiguracionTasas(tasaIgv, tasaIcbper);

        ResultadoCalculo calculo = calculadoraFiscalSunat.calcular(inputs, BigDecimal.ZERO, configTasas);

        // 11. Armar ReferenciaComprobante
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
                    .monedaRef(docOrig.getMoneda())
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
                    .monedaRef(refReq.getMonedaRef() != null ? refReq.getMonedaRef() : moneda)
                    .build();
        }

        // 12. Obtener correlativo
        Integer numeroCorrelativo;
        if (notaReq.getNumero() != null && notaReq.getNumero() > 0) {
            numeroCorrelativo = notaReq.getNumero();
            Serie serieBloqueada = serieRepository.findForUpdate(sucursal.getId(), tipoNota, serieEntity.getSerie())
                    .orElse(serieEntity);
            if (serieBloqueada.getCorrelativo() == null || numeroCorrelativo > serieBloqueada.getCorrelativo()) {
                serieBloqueada.setCorrelativo(numeroCorrelativo);
                serieRepository.save(serieBloqueada);
            }
        } else {
            numeroCorrelativo = correlativoServicePort.obtenerSiguienteCorrelativo(
                    sucursal.getId(), tipoNota, serieEntity.getSerie());
        }

        LocalDateTime fechaEmision = notaReq.getFechaEmision() != null
                ? notaReq.getFechaEmision() : LocalDateTime.now();

        // 13. Construir entidad ComprobanteFiscal (Nota)
        ComprobanteFiscal notaFiscal = ComprobanteFiscal.builder()
                .claveIdempotencia(notaReq.getClaveIdempotencia())
                .empresaId(empresa.getId())
                .sucursalId(sucursal.getId())
                .codigoSucursal(sucursal.getCodigo())
                .serieId(serieEntity.getId())
                .tipoComprobante(TipoComprobante.fromCodigo(tipoNota))
                .serie(serieEntity.getSerie())
                .numero(numeroCorrelativo)
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
                .estadoInterno("GENERADO")
                .detalles(calculo.lineas())
                .totalesAfectacion(calculo.totalesAfectacion())
                .tributosGlobales(calculo.tributosGlobales())
                .referencias(List.of(referencia))
                .cuotas(List.of())
                .build();

        ComprobanteFiscal guardado = documentoRepositoryPort.guardar(notaFiscal);

        // Fase electrónica: Generación UBL 2.1, Firma XMLDSig, Almacenamiento y Envío SUNAT
        ComprobanteFiscal finalizado = procesadorComprobanteElectronicoService.procesarFirmaYEnvio(guardado, empresa, sucursal);

        log.info("Nota electrónica emitida exitosamente: {} - {} (ID: {})",
                finalizado.getSerie(), finalizado.getNumero(), finalizado.getId());

        return finalizado;
    }
}
