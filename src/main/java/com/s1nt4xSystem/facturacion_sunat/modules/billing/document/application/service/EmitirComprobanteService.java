package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in.ConsultarComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in.EmitirComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.CorrelativoServicePort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.DocumentoRepositoryPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service.CalculadoraFiscalSunat;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service.CalculadoraFiscalSunat.CalculoLineaInput;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service.CalculadoraFiscalSunat.ResultadoCalculo;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service.ValidadorReglasFiscales;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.repository.SerieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
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
@RequiredArgsConstructor
@Slf4j
public class EmitirComprobanteService implements EmitirComprobanteUseCase, ConsultarComprobanteUseCase {

    private final DocumentoRepositoryPort documentoRepositoryPort;
    private final CorrelativoServicePort correlativoServicePort;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final SerieRepository serieRepository;
    private final com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.CatalogoFiscalPort catalogoFiscalPort;

    private final CalculadoraFiscalSunat calculadoraFiscalSunat = new CalculadoraFiscalSunat();
    private final ValidadorReglasFiscales validadorReglasFiscales = new ValidadorReglasFiscales();

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
        Empresa empresa = empresaRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe ninguna empresa configurada en el esquema actual"));

        // 3. Resolver Serie y Sucursal asociada (la serie determina unívocamente la sucursal)
        Serie serieEntity;
        if (command.getSucursalId() != null) {
            sucursalRepository.findById(command.getSucursalId())
                    .orElseThrow(() -> new IllegalArgumentException("La sucursal indicada no existe: " + command.getSucursalId()));
            serieEntity = serieRepository.findBySucursalIdAndTipoComprobanteAndSerie(
                    command.getSucursalId(), command.getTipoComprobante(), command.getSerie()
            ).orElseThrow(() -> new IllegalArgumentException(
                    String.format("La serie '%s' para tipo de comprobante '%s' no está configurada en la sucursal indicada",
                            command.getSerie(), command.getTipoComprobante())));
        } else {
            serieEntity = serieRepository.findByTipoComprobanteAndSerie(
                    command.getTipoComprobante(), command.getSerie()
            ).orElseThrow(() -> new IllegalArgumentException(
                    String.format("La serie '%s' para tipo de comprobante '%s' no está configurada en la empresa",
                            command.getSerie(), command.getTipoComprobante())));
        }
        Sucursal sucursal = serieEntity.getSucursal();

        // 5. Mapear líneas de entrada y ejecutar motor de cálculo tributario SUNAT con tasas dinámicas
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

            // Si solo envió uno de los dos (o si ambos son nulos tras enviar solo precio):
            // Si la empresa tiene incluido_tributo = false y solo enviaron precioUnitario (o viceversa),
            // la empresa manda la pauta predeterminada:
            if (valorUnitario == null && precioUnitario == null) {
                throw new IllegalArgumentException("Debe proporcionar el precio para el ítem: " + itemCmd.getDescripcion());
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
                    itemCmd.getCantidadBolsasIcbper()
            ));
        }

        BigDecimal tasaIgv = catalogoFiscalPort.determinarTasaIgvPorSucursal(sucursal);
        BigDecimal tasaIcbper = catalogoFiscalPort.obtenerTasaIcbperVigente();
        CalculadoraFiscalSunat.ConfiguracionTasas configTasas = new CalculadoraFiscalSunat.ConfiguracionTasas(tasaIgv, tasaIcbper);

        ResultadoCalculo calculo = calculadoraFiscalSunat.calcular(inputs, command.getDescuentoGlobal(), configTasas);

        // 6. Procesar Detracción si aplica
        DetraccionFiscal detraccionFiscal = null;
        if (command.getDetraccion() != null) {
            DetraccionCommand detCmd = command.getDetraccion();
            BigDecimal porcentajeDetraccion = detCmd.getPorcentaje();

            // Si no se proporcionó porcentaje o es cero, autocompletar desde el catálogo oficial
            if (porcentajeDetraccion == null || porcentajeDetraccion.compareTo(BigDecimal.ZERO) == 0) {
                var detOpt = catalogoFiscalPort.buscarDetraccion(detCmd.getCodigoBienServicio());
                if (detOpt.isPresent()) {
                    porcentajeDetraccion = detOpt.get().porcentaje();
                } else {
                    throw new IllegalArgumentException(
                            String.format("El código de detracción '%s' no está registrado en el catálogo de SUNAT",
                                    detCmd.getCodigoBienServicio()));
                }
            }

            BigDecimal montoDetraccion = detCmd.getMontoDetraccion();
            if (montoDetraccion == null) {
                montoDetraccion = calculo.importeTotal()
                        .multiply(porcentajeDetraccion)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }

            detraccionFiscal = DetraccionFiscal.builder()
                    .codigoBienServicio(detCmd.getCodigoBienServicio())
                    .porcentaje(porcentajeDetraccion)
                    .montoDetraccion(montoDetraccion)
                    .moneda(detCmd.getMoneda() != null ? detCmd.getMoneda() : "PEN")
                    .medioPago(detCmd.getMedioPago())
                    .numeroCuentaDetraccion(detCmd.getNumeroCuentaDetraccion())
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

        // 9. Asignar correlativo: si viene en el comando (ej. sincronización offline POS) se respeta y actualiza la serie; si no viene, se asigna atómicamente con bloqueo pesimista
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

        // 10. Construir Aggregate Root de Dominio Puro
        ComprobanteFiscal comprobante = ComprobanteFiscal.builder()
                .claveIdempotencia(command.getClaveIdempotencia())
                .empresaId(empresa.getId())
                .sucursalId(sucursal.getId())
                .codigoSucursal(sucursal.getCodigo())
                .serieId(serieEntity.getId())
                .tipoComprobante(TipoComprobante.fromCodigo(command.getTipoComprobante()))
                .serie(command.getSerie())
                .numero(siguienteNumero)
                .fechaEmision(LocalDateTime.now())
                .moneda(command.getMoneda() != null ? command.getMoneda() : "PEN")
                .tipoOperacion(command.getTipoOperacion() != null ? command.getTipoOperacion() : "0101")
                .clienteTipoDoc(command.getClienteTipoDoc())
                .clienteNumeroDoc(command.getClienteNumeroDoc())
                .clienteNombre(command.getClienteNombre())
                .clienteDireccion(command.getClienteDireccion())
                .formaPago(command.getFormaPago() != null ? command.getFormaPago().toUpperCase() : "CONTADO")
                .totalOtrosCargos(BigDecimal.ZERO)
                .totalTributos(calculo.totalTributos())
                .subtotal(calculo.totalValorVenta())
                .total(calculo.importeTotal())
                .totalDescuentos(calculo.totalDescuentos())
                .estadoInterno("REGISTRADO")
                .detalles(calculo.lineas())
                .totalesAfectacion(calculo.totalesAfectacion())
                .tributosGlobales(calculo.tributosGlobales())
                .cuotas(cuotasPago)
                .detraccion(detraccionFiscal)
                .referencias(referencias)
                .build();

        // 11. Validar reglas fiscales oficiales de SUNAT
        validadorReglasFiscales.validar(comprobante);

        // 12. Persistir Comprobante Fiscal en esquema del Tenant
        ComprobanteFiscal guardado = documentoRepositoryPort.guardar(comprobante);

        log.info("Comprobante emitido con éxito: {} - {} (ID: {})",
                guardado.getSerie(), guardado.getNumero(), guardado.getId());

        return guardado;
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

        // 1. Control de Idempotencia
        if (compReq.getClaveIdempotencia() != null && !compReq.getClaveIdempotencia().trim().isEmpty()) {
            Optional<ComprobanteFiscal> previo = documentoRepositoryPort.buscarPorClaveIdempotencia(compReq.getClaveIdempotencia().trim());
            if (previo.isPresent()) {
                log.info("Idempotencia detectada en comprobante para clave '{}'", compReq.getClaveIdempotencia());
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

        // 3. Resolver Serie y Sucursal (la serie identifica unívocamente la sucursal)
        String tipoComprobante = compReq.getTipo();
        Serie serieEntity;
        if (compReq.getSerie() != null && !compReq.getSerie().isBlank()) {
            serieEntity = serieRepository.findByTipoComprobanteAndSerie(tipoComprobante, compReq.getSerie().trim())
                    .orElseThrow(() -> new DomainException(
                            String.format("La serie '%s' para tipo '%s' no está configurada en la empresa",
                                    compReq.getSerie(), tipoComprobante)));
        } else {
            String codSucursal = (request.getEmisor() != null && request.getEmisor().getCodigoSucursal() != null)
                    ? request.getEmisor().getCodigoSucursal().trim() : "0000";
            Sucursal sucursalTemp = sucursalRepository.findByCodigo(codSucursal)
                    .orElseGet(() -> sucursalRepository.findByActivoTrue().stream().findFirst()
                            .orElseThrow(() -> new DomainException("No se encontró sucursal activa con código: " + codSucursal)));
            serieEntity = serieRepository.findFirstBySucursalIdAndTipoComprobante(sucursalTemp.getId(), tipoComprobante)
                    .orElseThrow(() -> new DomainException(
                            String.format("No existe ninguna serie activa configurada para tipo '%s'", tipoComprobante)));
        }
        Sucursal sucursal = serieEntity.getSucursal();

        // 4. Validar Cliente e Ítems
        ClienteDto clienteDto = request.getCliente();
        if (clienteDto == null) {
            throw new DomainException("El bloque cliente es obligatorio");
        }
        List<ItemComprobanteRequest> itemsList = request.getItems();
        if (itemsList == null || itemsList.isEmpty()) {
            throw new DomainException("El comprobante debe contener al menos un ítem");
        }

        // 5. Mapear líneas de entrada usando regla empresa.incluido_tributo
        boolean empresaIncluidoTributo = Boolean.TRUE.equals(empresa.getIncluidoTributo());
        List<CalculoLineaInput> inputs = new ArrayList<>();
        int itemIndex = 1;
        for (ItemComprobanteRequest itemReq : itemsList) {
            TipoAfectacionIgv afectacion = TipoAfectacionIgv.fromCodigo(itemReq.getCodigoAfectacionSunat());

            BigDecimal valorUnitario = null;
            BigDecimal precioUnitario = null;
            BigDecimal valor = itemReq.getValor();

            if (empresaIncluidoTributo) {
                precioUnitario = valor;
            } else {
                valorUnitario = valor;
            }

            String codProducto = (itemReq.getCodigoProducto() != null && !itemReq.getCodigoProducto().isBlank())
                    ? itemReq.getCodigoProducto().trim() : "-";

            int bolsasIcbper = 0;
            if (itemReq.getCantidadBolsasIcbper() != null && itemReq.getCantidadBolsasIcbper() > 0) {
                bolsasIcbper = itemReq.getCantidadBolsasIcbper();
            } else if (Boolean.TRUE.equals(itemReq.getIcbper()) && itemReq.getCantidad() != null) {
                bolsasIcbper = itemReq.getCantidad().intValue();
            }

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

        BigDecimal tasaIgv = catalogoFiscalPort.determinarTasaIgvPorSucursal(sucursal);
        BigDecimal tasaIcbper = catalogoFiscalPort.obtenerTasaIcbperVigente();
        CalculadoraFiscalSunat.ConfiguracionTasas configTasas = new CalculadoraFiscalSunat.ConfiguracionTasas(tasaIgv, tasaIcbper);

        ResultadoCalculo calculo = calculadoraFiscalSunat.calcular(inputs, request.getDescuentoGlobal(), configTasas);

        // 6. Detracción
        DetraccionFiscal detraccionFiscal = null;
        if (request.getDetraccion() != null) {
            DetraccionDto detDto = request.getDetraccion();
            BigDecimal porcentaje = detDto.getPorcentaje();
            if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) == 0) {
                var detOpt = catalogoFiscalPort.buscarDetraccion(detDto.getCodigoBienServicio());
                if (detOpt.isPresent()) {
                    porcentaje = detOpt.get().porcentaje();
                } else {
                    throw new DomainException("El código de detracción '" + detDto.getCodigoBienServicio() + "' no está en el catálogo oficial");
                }
            }
            BigDecimal montoDetraccion = detDto.getMontoDetraccion();
            if (montoDetraccion == null) {
                montoDetraccion = calculo.importeTotal().multiply(porcentaje)
                        .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            }
            detraccionFiscal = DetraccionFiscal.builder()
                    .codigoBienServicio(detDto.getCodigoBienServicio())
                    .porcentaje(porcentaje)
                    .montoDetraccion(montoDetraccion)
                    .moneda(compReq.getMoneda() != null ? compReq.getMoneda() : "PEN")
                    .medioPago(detDto.getMedioPago() != null ? detDto.getMedioPago() : "001")
                    .numeroCuentaDetraccion(detDto.getCuentaBancoNacion())
                    .build();
        }

        // 7. Cuotas
        List<CuotaPago> cuotasPago = new ArrayList<>();
        if ("credito".equalsIgnoreCase(compReq.getFormaPago())) {
            if (request.getCuotas() == null || request.getCuotas().isEmpty()) {
                throw new DomainException("Las ventas al crédito requieren al menos una cuota de pago");
            }
            for (CuotaDto c : request.getCuotas()) {
                cuotasPago.add(CuotaPago.builder()
                        .numeroCuota(c.getNumero())
                        .fechaVencimiento(c.getFechaVencimiento())
                        .monto(c.getMonto())
                        .moneda(compReq.getMoneda() != null ? compReq.getMoneda() : "PEN")
                        .build());
            }
        }

        // 8. Correlativo
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
                .clienteTipoDoc(clienteDto.getTipoDocumento())
                .clienteNumeroDoc(clienteDto.getNumeroDocumento())
                .clienteNombre(clienteDto.getNombreRazonSocial())
                .clienteDireccion(clienteDto.getDireccion())
                .formaPago(compReq.getFormaPago() != null ? compReq.getFormaPago().toUpperCase() : "CONTADO")
                .totalOtrosCargos(BigDecimal.ZERO)
                .totalTributos(calculo.totalTributos())
                .subtotal(calculo.totalValorVenta())
                .total(calculo.importeTotal())
                .totalDescuentos(calculo.totalDescuentos())
                .estadoInterno("GENERADO")
                .detalles(calculo.lineas())
                .totalesAfectacion(calculo.totalesAfectacion())
                .tributosGlobales(calculo.tributosGlobales())
                .cuotas(cuotasPago)
                .detraccion(detraccionFiscal)
                .referencias(List.of())
                .build();

        validadorReglasFiscales.validar(comprobante);

        ComprobanteFiscal guardado = documentoRepositoryPort.guardar(comprobante);
        log.info("Comprobante emitido con formato OpenAPI: {} - {} (ID: {})",
                guardado.getSerie(), guardado.getNumero(), guardado.getId());

        return guardado;
    }
}
