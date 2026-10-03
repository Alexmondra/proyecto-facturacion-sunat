package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.dto.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.validation.DocumentValidator;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in.ConsultarComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in.EmitirComprobanteUseCase;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EmitirComprobanteService implements EmitirComprobanteUseCase, ConsultarComprobanteUseCase {

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
    private final ClienteIdentidadResolver clienteIdentidadResolver;

    private final CalculadoraFiscalSunat calculadoraFiscalSunat = new CalculadoraFiscalSunat();
    private final ValidadorReglasFiscales validadorReglasFiscales = new ValidadorReglasFiscales();

    @Autowired
    public EmitirComprobanteService(
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
            DocumentValidator documentValidator,
            ClienteIdentidadResolver clienteIdentidadResolver) {
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
        this.clienteIdentidadResolver = clienteIdentidadResolver != null
                ? clienteIdentidadResolver
                : new ClienteIdentidadResolver((tipo, num) -> Optional.empty());
    }

    public EmitirComprobanteService(
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
                new DocumentValidator(empresaRepository, empresaConfigRepository, sucursalRepository, serieRepository, companyValidator),
                new ClienteIdentidadResolver((tipo, num) -> Optional.empty()));
    }

    @Override
    @Transactional
    public ComprobanteFiscal emitir(EmitirComprobanteCommand command) {
        // 1. Control de Idempotencia: Si ya fue procesado este payload con esta clave, retornamos el comprobante previo
        if (command.getClaveIdempotencia() != null && !command.getClaveIdempotencia().trim().isEmpty()) {
            Optional<ComprobanteFiscal> previo = documentoRepositoryPort.buscarPorClaveIdempotencia(command.getClaveIdempotencia().trim());
            if (previo.isPresent()) {
                log.info("Idempotencia detectada para clave '{}'. Devolviendo comprobante existente {} - {}",
                        command.getClaveIdempotencia(), previo.get().getSerie(), previo.get().getNumero());
                return previo.get();
            }
        }

        // 2. Obtener Empresa del Tenant actual
        Empresa empresa = documentValidator.validateAndGetEmpresaTenant();

        // 3. Validar Serie y Requisitos Fiscales (Fail-Fast)
        TipoComprobante tipoEnum = TipoComprobante.fromCodigo(command.getTipoComprobante());
        if (command.getSerie() != null && !command.getSerie().isBlank()) {
            documentValidator.validateSerieFormato(tipoEnum, command.getSerie());
        }
        documentValidator.validateRequisitosFiscales(empresa, tipoEnum);

        // 4. Resolver Serie y Sucursal asociada (la serie determina unívocamente la sucursal)
        Serie serieEntity = documentValidator.validateAndResolveSerie(command);
        Sucursal sucursal = serieEntity.getSucursal();

        // 5. Mapear líneas de entrada y ejecutar motor de cálculo tributario SUNAT con tasas dinámicas
        BigDecimal tasaIgv = catalogoFiscalPort.determinarTasaIgvPorSucursal(sucursal);
        BigDecimal tasaIcbper = catalogoFiscalPort.obtenerTasaIcbperVigente();
        boolean empresaIncluidoTributo = Boolean.TRUE.equals(empresa.getIncluidoTributo());

        List<CalculoLineaInput> inputs = new ArrayList<>();
        int itemIndex = 1;
        for (ComprobanteItemCommand itemCmd : command.getItems()) {
            TipoAfectacionIgv afectacion = TipoAfectacionIgv.fromCodigo(itemCmd.getTipoAfectacion());

            BigDecimal valorUnitario = itemCmd.getValorUnitario();
            BigDecimal precioUnitario = itemCmd.getPrecioUnitario();

            // Si envió el campo genérico 'precio', asignarlo según la política de la empresa
            if (itemCmd.getPrecio() != null) {
                if (empresaIncluidoTributo) {
                    if (precioUnitario == null) precioUnitario = itemCmd.getPrecio();
                } else {
                    if (valorUnitario == null) valorUnitario = itemCmd.getPrecio();
                }
            }

            // Validar existencia de precio
            documentValidator.validateItemPrecios(itemCmd, valorUnitario, precioUnitario);

            int bolsasIcbper = itemCmd.getCantidadBolsasIcbper() != null ? itemCmd.getCantidadBolsasIcbper() : 0;
            if (empresaIncluidoTributo && bolsasIcbper > 0) {
                BigDecimal precioTotal = precioUnitario != null ? precioUnitario : valorUnitario;
                documentValidator.validatePrecioBolsaIcbper(afectacion, precioTotal, tasaIcbper);
                if (afectacion.isGravaIgv()) {
                    precioUnitario = precioTotal.subtract(tasaIcbper);
                    valorUnitario = null;
                } else if (afectacion.isGratuito()) {
                    precioUnitario = BigDecimal.ZERO;
                    valorUnitario = BigDecimal.ZERO;
                }
            }

            inputs.add(new CalculoLineaInput(
                    itemIndex++,
                    itemCmd.getCodigoProducto(),
                    itemCmd.getDescripcion(),
                    itemCmd.getCantidad(),
                    itemCmd.getUnidadMedida(),
                    valorUnitario,
                    precioUnitario,
                    afectacion,
                    itemCmd.getDescuento(),
                    bolsasIcbper
            ));
        }

        CalculadoraFiscalSunat.ConfiguracionTasas configTasas = new CalculadoraFiscalSunat.ConfiguracionTasas(tasaIgv, tasaIcbper);
        BigDecimal descuentoGlobal = command.getDescuentoGlobal() != null ? command.getDescuentoGlobal() : BigDecimal.ZERO;
        ResultadoCalculo calculo = calculadoraFiscalSunat.calcular(inputs, descuentoGlobal, configTasas);

        // 6. Procesar Detracción Fiscal si aplica
        DetraccionFiscal detraccionFiscal = null;
        if (command.getDetraccion() != null) {
            DetraccionCommand dc = command.getDetraccion();
            BigDecimal montoDetraccion = dc.getMontoDetraccion() != null
                    ? dc.getMontoDetraccion()
                    : calculo.importeTotal()
                    .multiply(dc.getPorcentaje())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            detraccionFiscal = DetraccionFiscal.builder()
                    .codigoBienServicio(dc.getCodigoBienServicio() != null ? dc.getCodigoBienServicio() : "01")
                    .porcentaje(dc.getPorcentaje())
                    .montoDetraccion(montoDetraccion)
                    .numeroCuentaDetraccion(dc.getNumeroCuentaDetraccion())
                    .build();
        }

        // 7. Procesar Cuotas de Crédito si aplica
        List<CuotaPago> cuotasPago = new ArrayList<>();
        if (command.getCuotas() != null) {
            for (int i = 0; i < command.getCuotas().size(); i++) {
                CuotaCommand cc = command.getCuotas().get(i);
                cuotasPago.add(CuotaPago.builder()
                        .numeroCuota(cc.getNumeroCuota() != null ? cc.getNumeroCuota() : i + 1)
                        .fechaVencimiento(cc.getFechaVencimiento())
                        .monto(cc.getMonto())
                        .moneda(cc.getMoneda() != null ? cc.getMoneda() : "PEN")
                        .build());
            }
        }

        // 8. Procesar Referencias a documentos previos si aplica (NC / ND)
        List<ReferenciaComprobante> referencias = new ArrayList<>();
        if (command.getReferencias() != null) {
            for (ReferenciaCommand rc : command.getReferencias()) {
                referencias.add(ReferenciaComprobante.builder()
                        .tipoRelacion(rc.getTipoRelacion())
                        .documentoReferenciadoId(rc.getDocumentoReferenciadoId())
                        .tipoDocumentoRef(rc.getTipoDocumentoRef())
                        .serieRef(rc.getSerieRef())
                        .numeroRef(rc.getNumeroRef())
                        .motivoCodigo(rc.getMotivoCodigo())
                        .motivoDescripcion(rc.getMotivoDescripcion())
                        .fechaEmisionRef(rc.getFechaEmisionRef())
                        .build());
            }
        }

        // 9. Asignar correlativo
        Integer siguienteNumero;
        if (command.getNumero() != null && command.getNumero() > 0) {
            siguienteNumero = command.getNumero();
            Serie serieBloqueada = serieRepository.findForUpdate(sucursal.getId(), command.getTipoComprobante(), command.getSerie())
                    .orElse(serieEntity);
            if (serieBloqueada.getCorrelativo() == null || siguienteNumero > serieBloqueada.getCorrelativo()) {
                serieBloqueada.setCorrelativo(siguienteNumero);
                serieRepository.save(serieBloqueada);
            }
        } else {
            siguienteNumero = correlativoServicePort.obtenerSiguienteCorrelativo(
                    sucursal.getId(), command.getTipoComprobante(), command.getSerie());
        }

        // 10. Resolver y rectificar la identidad del cliente (con fallback tolerante a fallos)
        ClienteIdentidad clienteFinal = clienteIdentidadResolver.resolverYValidar(
                command.getTipoComprobante(),
                command.getClienteTipoDoc(),
                command.getClienteNumeroDoc(),
                command.getClienteNombre(),
                command.getClienteDireccion(),
                calculo.importeTotal()
        );

        // 11. Construir Aggregate Root de Dominio Puro
        ComprobanteFiscal comprobante = ComprobanteFiscal.builder()
                .claveIdempotencia(command.getClaveIdempotencia())
                .empresaId(empresa.getId())
                .sucursalId(sucursal.getId())
                .codigoSucursal(sucursal.getCodigo())
                .serieId(serieEntity.getId())
                .tipoComprobante(TipoComprobante.fromCodigo(command.getTipoComprobante()))
                .serie(serieEntity.getSerie())
                .numero(siguienteNumero)
                .fechaEmision(LocalDateTime.now())
                .moneda(command.getMoneda() != null ? command.getMoneda() : "PEN")
                .tipoOperacion(command.getTipoOperacion() != null ? command.getTipoOperacion() : "0101")
                .clienteTipoDoc(clienteFinal.tipoDocumento())
                .clienteNumeroDoc(clienteFinal.numeroDocumento())
                .clienteNombre(clienteFinal.denominacion())
                .clienteDireccion(clienteFinal.direccion())
                .formaPago(command.getFormaPago() != null ? command.getFormaPago().toUpperCase() : "CONTADO")
                .totalOtrosCargos(BigDecimal.ZERO)
                .totalTributos(calculo.totalTributos())
                .subtotal(calculo.totalValorVenta())
                .total(calculo.importeTotal())
                .totalDescuentos(calculo.totalDescuentos())
                .estadoInterno("REGISTRADO")
                .payloadEntrada(command.getPayloadEntrada())
                .detalles(calculo.lineas())
                .totalesAfectacion(calculo.totalesAfectacion())
                .tributosGlobales(calculo.tributosGlobales())
                .cuotas(cuotasPago)
                .detraccion(detraccionFiscal)
                .referencias(referencias)
                .build();

        // 11. Validador de Reglas Fiscales SUNAT
        validadorReglasFiscales.validar(comprobante);

        // 12. Guardar en Base de Datos
        ComprobanteFiscal guardado = documentoRepositoryPort.guardar(comprobante);

        // 13. Fase electrónica: Generación UBL 2.1, Firma XMLDSig, Almacenamiento y Envío SUNAT
        if (tipoEnum.isEsElectronico()) {
            ComprobanteFiscal finalizado = procesadorComprobanteElectronicoService.procesarFirmaYEnvio(guardado, empresa, sucursal);
            log.info("Comprobante fiscal procesado: {} - {} (ID: {})",
                    finalizado.getSerie(), finalizado.getNumero(), finalizado.getId());
            return finalizado;
        } else {
            log.info("Ticket interno guardado (sin firma ni envío SUNAT): {} - {} (ID: {})",
                    guardado.getSerie(), guardado.getNumero(), guardado.getId());
            return guardado;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComprobanteFiscal> consultarPorId(UUID id) {
        return documentoRepositoryPort.buscarPorId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComprobanteFiscal> consultarPorClaveIdempotencia(String claveIdempotencia) {
        return documentoRepositoryPort.buscarPorClaveIdempotencia(claveIdempotencia);
    }

    @Override
    @Transactional
    public ComprobanteFiscal emitir(CrearComprobanteRequest request) {
        ComprobanteDataRequest compReq = request.getComprobante();

        // 1. Idempotencia
        if (compReq.getClaveIdempotencia() != null && !compReq.getClaveIdempotencia().trim().isEmpty()) {
            Optional<ComprobanteFiscal> previo = documentoRepositoryPort.buscarPorClaveIdempotencia(compReq.getClaveIdempotencia().trim());
            if (previo.isPresent()) {
                log.info("Idempotencia detectada en comprobante para clave '{}'", compReq.getClaveIdempotencia());
                return previo.get();
            }
        }

        // 2. Obtener Empresa del Tenant actual
        Empresa empresa = documentValidator.validateAndGetEmpresaTenant();
        documentValidator.validateEmisorRucCoincide(
                request.getEmisor() != null ? request.getEmisor().getRuc() : null, empresa);

        // 3. Validar Serie y Requisitos Fiscales (Fail-Fast)
        String tipoComprobante = compReq.getTipo();
        TipoComprobante tipoEnum = TipoComprobante.fromCodigo(tipoComprobante);
        if (compReq.getSerie() != null && !compReq.getSerie().isBlank()) {
            documentValidator.validateSerieFormato(tipoEnum, compReq.getSerie());
        }
        documentValidator.validateRequisitosFiscales(empresa, tipoEnum);

        // 4. Resolver Serie y Sucursal
        String codSucursalEmisor = request.getEmisor() != null ? request.getEmisor().getCodigoSucursal() : null;
        Serie serieEntity = documentValidator.validateAndResolveSerieRest(tipoComprobante, compReq.getSerie(), codSucursalEmisor);
        Sucursal sucursal = serieEntity.getSucursal();

        // 5. Validar Cliente e Ítems
        ClienteDto clienteDto = request.getCliente();
        documentValidator.validateClientePresent(clienteDto, tipoComprobante);
        List<ItemComprobanteRequest> itemsList = request.getItems();
        documentValidator.validateItemsNotEmpty(itemsList);

        // 6. Mapear líneas de entrada usando regla empresa.incluido_tributo
        BigDecimal tasaIgv = catalogoFiscalPort.determinarTasaIgvPorSucursal(sucursal);
        BigDecimal tasaIcbper = catalogoFiscalPort.obtenerTasaIcbperVigente();
        boolean empresaIncluidoTributo = Boolean.TRUE.equals(empresa.getIncluidoTributo());

        List<CalculoLineaInput> inputs = new ArrayList<>();
        int itemIndex = 1;
        for (ItemComprobanteRequest itemReq : itemsList) {
            TipoAfectacionIgv afectacion = TipoAfectacionIgv.fromCodigo(itemReq.getCodigoAfectacionSunat());

            BigDecimal valorUnitario = null;
            BigDecimal precioUnitario = null;
            BigDecimal valor = itemReq.getValor();
            documentValidator.validateItemValor(valor, itemReq.getDescripcion());

            int bolsasIcbper = 0;
            if (itemReq.getCantidadBolsasIcbper() != null && itemReq.getCantidadBolsasIcbper() > 0) {
                bolsasIcbper = itemReq.getCantidadBolsasIcbper();
            } else if (Boolean.TRUE.equals(itemReq.getIcbper()) && itemReq.getCantidad() != null) {
                bolsasIcbper = itemReq.getCantidad().intValue();
            }

            if (empresaIncluidoTributo) {
                if (bolsasIcbper > 0) {
                    documentValidator.validatePrecioBolsaIcbper(afectacion, valor, tasaIcbper);
                    if (afectacion.isGravaIgv()) {
                        precioUnitario = valor.subtract(tasaIcbper);
                        valorUnitario = null;
                    } else if (afectacion.isGratuito()) {
                        precioUnitario = BigDecimal.ZERO;
                        valorUnitario = BigDecimal.ZERO;
                    } else {
                        precioUnitario = valor;
                    }
                } else {
                    precioUnitario = valor;
                }
            } else {
                valorUnitario = valor;
            }

            String codProducto = (itemReq.getCodigoProducto() != null && !itemReq.getCodigoProducto().isBlank())
                    ? itemReq.getCodigoProducto().trim() : "-";

            inputs.add(new CalculoLineaInput(
                    itemIndex++,
                    codProducto,
                    itemReq.getDescripcion(),
                    itemReq.getCantidad(),
                    itemReq.getUnidadMedida(),
                    valorUnitario,
                    precioUnitario,
                    afectacion,
                    itemReq.getDescuento(),
                    bolsasIcbper
            ));
        }

        CalculadoraFiscalSunat.ConfiguracionTasas configTasas = new CalculadoraFiscalSunat.ConfiguracionTasas(tasaIgv, tasaIcbper);
        ResultadoCalculo calculo = calculadoraFiscalSunat.calcular(inputs, BigDecimal.ZERO, configTasas);

        // Detracción
        DetraccionFiscal detraccionFiscal = null;
        if (request.getDetraccion() != null) {
            DetraccionDto detReq = request.getDetraccion();
            BigDecimal porcentaje = detReq.getPorcentaje() != null ? detReq.getPorcentaje() : BigDecimal.ZERO;
            BigDecimal montoDetraccion = detReq.getMontoDetraccion() != null
                    ? detReq.getMontoDetraccion()
                    : calculo.importeTotal().multiply(porcentaje).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            detraccionFiscal = DetraccionFiscal.builder()
                    .codigoBienServicio(detReq.getCodigoBienServicio())
                    .porcentaje(porcentaje)
                    .montoDetraccion(montoDetraccion)
                    .numeroCuentaDetraccion(detReq.getCuentaBancoNacion())
                    .build();
        }

        // Cuotas si Crédito
        List<CuotaPago> cuotasPago = new ArrayList<>();
        if (request.getCuotas() != null) {
            for (int i = 0; i < request.getCuotas().size(); i++) {
                CuotaDto cr = request.getCuotas().get(i);
                cuotasPago.add(CuotaPago.builder()
                        .numeroCuota(cr.getNumero() != null ? cr.getNumero() : i + 1)
                        .fechaVencimiento(cr.getFechaVencimiento())
                        .monto(cr.getMonto())
                        .moneda(compReq.getMoneda() != null ? compReq.getMoneda() : "PEN")
                        .build());
            }
        }

        // Asignar Correlativo
        Integer correlativo;
        if (compReq.getNumero() != null && compReq.getNumero() > 0) {
            correlativo = compReq.getNumero();
            Serie serieBloqueada = serieRepository.findForUpdate(sucursal.getId(), tipoComprobante, serieEntity.getSerie())
                    .orElse(serieEntity);
            if (serieBloqueada.getCorrelativo() == null || correlativo > serieBloqueada.getCorrelativo()) {
                serieBloqueada.setCorrelativo(correlativo);
                serieRepository.save(serieBloqueada);
            }
        } else {
            correlativo = correlativoServicePort.obtenerSiguienteCorrelativo(
                    sucursal.getId(), tipoComprobante, serieEntity.getSerie());
        }

        LocalDateTime fechaEmision = compReq.getFechaEmision() != null
                ? compReq.getFechaEmision() : LocalDateTime.now();

        // 8. Resolver y rectificar la identidad del cliente (con fallback tolerante a fallos)
        ClienteIdentidad clienteFinal = clienteIdentidadResolver.resolverYValidar(
                tipoComprobante,
                clienteDto != null ? clienteDto.getTipoDocumento() : null,
                clienteDto != null ? clienteDto.getNumeroDocumento() : null,
                clienteDto != null ? clienteDto.getNombreRazonSocial() : null,
                clienteDto != null ? clienteDto.getDireccion() : null,
                calculo.importeTotal()
        );

        ComprobanteFiscal comprobante = ComprobanteFiscal.builder()
                .claveIdempotencia(compReq.getClaveIdempotencia())
                .empresaId(empresa.getId())
                .sucursalId(sucursal.getId())
                .codigoSucursal(sucursal.getCodigo())
                .serieId(serieEntity.getId())
                .tipoComprobante(TipoComprobante.fromCodigo(tipoComprobante))
                .serie(serieEntity.getSerie())
                .numero(correlativo)
                .fechaEmision(fechaEmision)
                .moneda(compReq.getMoneda() != null ? compReq.getMoneda() : "PEN")
                .tipoOperacion(compReq.getTipoOperacion() != null ? compReq.getTipoOperacion() : "0101")
                .clienteTipoDoc(clienteFinal.tipoDocumento())
                .clienteNumeroDoc(clienteFinal.numeroDocumento())
                .clienteNombre(clienteFinal.denominacion())
                .clienteDireccion(clienteFinal.direccion())
                .formaPago(compReq.getFormaPago() != null ? compReq.getFormaPago().toUpperCase() : "CONTADO")
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
                .cuotas(cuotasPago)
                .detraccion(detraccionFiscal)
                .referencias(List.of())
                .build();

        validadorReglasFiscales.validar(comprobante);

        ComprobanteFiscal guardado = documentoRepositoryPort.guardar(comprobante);

        // Fase electrónica: Generación UBL 2.1, Firma XMLDSig, Almacenamiento y Envío SUNAT (Solo CPE)
        if (tipoEnum.isEsElectronico()) {
            ComprobanteFiscal finalizado = procesadorComprobanteElectronicoService.procesarFirmaYEnvio(guardado, empresa, sucursal);
            log.info("Comprobante emitido con formato OpenAPI: {} - {} (ID: {})",
                    finalizado.getSerie(), finalizado.getNumero(), finalizado.getId());
            return finalizado;
        } else {
            log.info("Ticket interno emitido con formato OpenAPI (sin firma ni envío SUNAT): {} - {} (ID: {})",
                    guardado.getSerie(), guardado.getNumero(), guardado.getId());
            return guardado;
        }
    }
}
