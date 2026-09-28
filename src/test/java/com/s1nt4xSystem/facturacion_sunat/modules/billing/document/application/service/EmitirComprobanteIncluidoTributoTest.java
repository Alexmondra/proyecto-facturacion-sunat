package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto.ComprobanteItemCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto.EmitirComprobanteCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoAfectacionIgv;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.CatalogoFiscalPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.CorrelativoServicePort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.DocumentoRepositoryPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.repository.SerieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.ClienteDto;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.ComprobanteDataRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.CrearComprobanteRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.ItemComprobanteRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmitirComprobanteIncluidoTributoTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private SucursalRepository sucursalRepository;

    @Mock
    private SerieRepository serieRepository;

    @Mock
    private DocumentoRepositoryPort documentoRepositoryPort;

    @Mock
    private CorrelativoServicePort correlativoServicePort;

    @Mock
    private CatalogoFiscalPort catalogoFiscalPort;

    @Mock
    private ProcesadorComprobanteElectronicoService procesadorComprobanteElectronicoService;

    @Mock
    private com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaConfigRepository empresaConfigRepository;

    @Mock
    private com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.CertificadoDigitalService certificadoDigitalService;

    @InjectMocks
    private EmitirComprobanteService emitirComprobanteService;

    private Sucursal sucursal;
    private Serie serie;

    @BeforeEach
    void setUp() {
        lenient().when(procesadorComprobanteElectronicoService.procesarFirmaYEnvio(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.EmpresaConfig config = 
                com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.EmpresaConfig.builder()
                        .userSol("MODDATOS")
                        .passSol("moddatos")
                        .certificado("certificates/test.pfx")
                        .build();
        lenient().when(empresaConfigRepository.findByEmpresaId(any())).thenReturn(Optional.of(config));
        lenient().when(certificadoDigitalService.existeCertificado(any())).thenReturn(true);

        sucursal = Sucursal.builder()
                .id(UUID.randomUUID())
                .nombreSucursal("Principal")
                .impuestoPorcentaje(new BigDecimal("18.00"))
                .activo(true)
                .build();

        serie = Serie.builder()
                .id(UUID.randomUUID())
                .sucursal(sucursal)
                .tipoComprobante("01")
                .serie("F001")
                .correlativo(0)
                .build();

        lenient().when(documentoRepositoryPort.buscarPorClaveIdempotencia(any())).thenReturn(Optional.empty());
        lenient().when(sucursalRepository.findById(any())).thenReturn(Optional.of(sucursal));
        lenient().when(serieRepository.findBySucursalIdAndTipoComprobanteAndSerie(any(), any(), any())).thenReturn(Optional.of(serie));
        lenient().when(serieRepository.findByTipoComprobanteAndSerie(any(), any())).thenReturn(Optional.of(serie));
        lenient().when(catalogoFiscalPort.determinarTasaIgvPorSucursal(any())).thenReturn(new BigDecimal("18.00"));
        lenient().when(catalogoFiscalPort.obtenerTasaIcbperVigente()).thenReturn(new BigDecimal("0.50"));
        lenient().when(correlativoServicePort.obtenerSiguienteCorrelativo(any(), any(), any())).thenReturn(1);
        lenient().when(documentoRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Cuando empresa.incluido_tributo = TRUE y se envia precio 118, subtotal es 100 y total es 118")
    void emitirComprobante_conIncluidoTributoTrue_desglosaIgv() {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20100070970")
                .razonSocial("Supermercado Central S.A.C.")
                .incluidoTributo(true)
                .build();

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));

        EmitirComprobanteCommand command = EmitirComprobanteCommand.builder()
                .claveIdempotencia("tx-inc-true-1")
                .sucursalId(sucursal.getId())
                .tipoComprobante("01")
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20555555551")
                .clienteNombre("Cliente Prueba")
                .items(List.of(
                        ComprobanteItemCommand.builder()
                                .codigoProducto("PROD-01")
                                .descripcion("Arroz Superior 5kg")
                                .cantidad(new BigDecimal("1.0000"))
                                .precio(new BigDecimal("118.00")) // Envía precio genérico con IGV
                                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA.getCodigo())
                                .build()
                ))
                .build();

        ComprobanteFiscal resultado = emitirComprobanteService.emitir(command);

        assertThat(resultado).isNotNull();
        // Subtotal (sin IGV) debe ser 100.00
        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        // IGV debe ser 18.00
        assertThat(resultado.getTotalTributos()).isEqualByComparingTo(new BigDecimal("18.00"));
        // Total a pagar debe ser 118.00
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("118.00"));
    }

    @Test
    @DisplayName("Cuando empresa.incluido_tributo = FALSE y se envia precio 100, subtotal es 100 y total es 118")
    void emitirComprobante_conIncluidoTributoFalse_sumaIgv() {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20200080980")
                .razonSocial("Distribuidora Mayorista S.A.C.")
                .incluidoTributo(false)
                .build();

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));

        EmitirComprobanteCommand command = EmitirComprobanteCommand.builder()
                .claveIdempotencia("tx-inc-false-1")
                .sucursalId(sucursal.getId())
                .tipoComprobante("01")
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20555555551")
                .clienteNombre("Cliente Mayorista")
                .items(List.of(
                        ComprobanteItemCommand.builder()
                                .codigoProducto("PROD-02")
                                .descripcion("Caja de Aceite")
                                .cantidad(new BigDecimal("1.0000"))
                                .precio(new BigDecimal("100.00")) // Envía precio neto (sin IGV)
                                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA.getCodigo())
                                .build()
                ))
                .build();

        ComprobanteFiscal resultado = emitirComprobanteService.emitir(command);

        assertThat(resultado).isNotNull();
        // Subtotal (sin IGV) debe ser 100.00
        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        // IGV debe ser 18.00
        assertThat(resultado.getTotalTributos()).isEqualByComparingTo(new BigDecimal("18.00"));
        // Total a pagar debe ser 118.00 (100 + 18)
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("118.00"));
    }

    @Test
    @DisplayName("Cuando empresa.incluido_tributo = TRUE y se envia bolsa con precio 0.60, desglosa ICBPER 0.50 y el resto para IGV")
    void emitirComprobante_conBolsaPlasticaEIncluidoTributoTrue_desglosaIcbperEIgv() {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20200080980")
                .razonSocial("Supermercado Retail S.A.C.")
                .incluidoTributo(true)
                .build();

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));

        EmitirComprobanteCommand command = EmitirComprobanteCommand.builder()
                .claveIdempotencia("tx-bolsa-inc-true-1")
                .sucursalId(sucursal.getId())
                .tipoComprobante("01")
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20555555551")
                .clienteNombre("Cliente Retail")
                .items(List.of(
                        ComprobanteItemCommand.builder()
                                .codigoProducto("BOLSA-01")
                                .descripcion("Bolsa plástica ecológica")
                                .cantidad(new BigDecimal("1.0000"))
                                .precio(new BigDecimal("0.60")) // PVP final con ICBPER e IGV incluidos
                                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA.getCodigo())
                                .cantidadBolsasIcbper(1)
                                .build()
                ))
                .build();

        ComprobanteFiscal resultado = emitirComprobanteService.emitir(command);

        assertThat(resultado).isNotNull();
        // Total a pagar debe ser exactamente los 0.60 enviados
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("0.60"));
        // El total de tributos debe incluir ICBPER (0.50) + IGV de la bolsa (0.02) = 0.52
        assertThat(resultado.getTotalTributos()).isEqualByComparingTo(new BigDecimal("0.52"));
        // Subtotal (0.10 / 1.18 = 0.08)
        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("0.08"));
    }

    @Test
    @DisplayName("Cuando empresa.incluido_tributo = TRUE y se envia bolsa con precio <= 0.50 en afectación gravada, lanza DomainException")
    void emitirComprobante_conBolsaPlasticaEIncluidoTributoTrue_precioMenorOIgualAIcbper_lanzaExcepcion() {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20200080980")
                .razonSocial("Supermercado Retail S.A.C.")
                .incluidoTributo(true)
                .build();

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));

        EmitirComprobanteCommand command = EmitirComprobanteCommand.builder()
                .claveIdempotencia("tx-bolsa-invalida-1")
                .sucursalId(sucursal.getId())
                .tipoComprobante("01")
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20555555551")
                .clienteNombre("Cliente Retail")
                .items(List.of(
                        ComprobanteItemCommand.builder()
                                .codigoProducto("BOLSA-01")
                                .descripcion("Bolsa plástica ecológica")
                                .cantidad(new BigDecimal("1.0000"))
                                .precio(new BigDecimal("0.50")) // Precio insuficiente (no cubre bolsa + ICBPER)
                                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA.getCodigo())
                                .cantidadBolsasIcbper(1)
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> emitirComprobanteService.emitir(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("debe ser mayor a la tasa ICBPER");
    }

    @Test
    @DisplayName("Cuando empresa.incluido_tributo = FALSE y se envia bolsa con valor neto 0.10, suma ICBPER 0.50 e IGV 0.02 para total 0.62")
    void emitirComprobante_conBolsaPlasticaEIncluidoTributoFalse_sumaIcbperEIgv() {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20200080980")
                .razonSocial("Distribuidora Mayorista S.A.C.")
                .incluidoTributo(false)
                .build();

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));

        EmitirComprobanteCommand command = EmitirComprobanteCommand.builder()
                .claveIdempotencia("tx-bolsa-inc-false-1")
                .sucursalId(sucursal.getId())
                .tipoComprobante("01")
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20555555551")
                .clienteNombre("Cliente Mayorista")
                .items(List.of(
                        ComprobanteItemCommand.builder()
                                .codigoProducto("BOLSA-01")
                                .descripcion("Bolsa plástica ecológica")
                                .cantidad(new BigDecimal("1.0000"))
                                .precio(new BigDecimal("0.10")) // Valor neto sin tributos
                                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA.getCodigo())
                                .cantidadBolsasIcbper(1)
                                .build()
                ))
                .build();

        ComprobanteFiscal resultado = emitirComprobanteService.emitir(command);

        assertThat(resultado).isNotNull();
        // Subtotal = 0.10
        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("0.10"));
        // Total tributos = 0.02 (IGV) + 0.50 (ICBPER) = 0.52
        assertThat(resultado.getTotalTributos()).isEqualByComparingTo(new BigDecimal("0.52"));
        // Total = 0.10 + 0.52 = 0.62
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("0.62"));
    }

    @Test
    @DisplayName("Cuando emitir(CrearComprobanteRequest) con empresa.incluido_tributo = TRUE y bolsa 0.60, desglosa ICBPER 0.50 e IGV")
    void emitirCrearComprobanteRequest_conBolsaPlasticaEIncluidoTributoTrue_desglosaIcbperEIgv() {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20200080980")
                .razonSocial("Supermercado Retail S.A.C.")
                .incluidoTributo(true)
                .build();

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));

        CrearComprobanteRequest request = CrearComprobanteRequest.builder()
                .comprobante(ComprobanteDataRequest.builder()
                        .tipo("01")
                        .serie("F001")
                        .claveIdempotencia("tx-req-bolsa-1")
                        .build())
                .cliente(ClienteDto.builder()
                        .tipoDocumento("6")
                        .numeroDocumento("20555555551")
                        .nombreRazonSocial("Cliente Retail S.A.C.")
                        .build())
                .items(List.of(
                        ItemComprobanteRequest.builder()
                                .codigoProducto("BOLSA-01")
                                .descripcion("Bolsa plástica mediana")
                                .cantidad(new BigDecimal("1.0000"))
                                .valor(new BigDecimal("0.60")) // PVP con IGV e ICBPER
                                .codigoAfectacionSunat("10")
                                .icbper(true)
                                .cantidadBolsasIcbper(1)
                                .build()
                ))
                .build();

        ComprobanteFiscal resultado = emitirComprobanteService.emitir(request);

        assertThat(resultado).isNotNull();
        // Total a pagar debe ser exactamente 0.60
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("0.60"));
        // Total de tributos: ICBPER (0.50) + IGV (0.02) = 0.52
        assertThat(resultado.getTotalTributos()).isEqualByComparingTo(new BigDecimal("0.52"));
        // Subtotal: 0.10 / 1.18 = 0.08
        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("0.08"));
    }

    @Test
    @DisplayName("Ticket interno (00) debe calcular tributos (IGV e ICBPER) idéntico y OMITIR envío a SUNAT")
    void emitirTicketInterno_desglosaTributosYNoEnviaASunat() {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20100070970")
                .razonSocial("Supermercado Central S.A.C.")
                .incluidoTributo(true)
                .build();

        Serie serieTicket = Serie.builder()
                .id(UUID.randomUUID())
                .sucursal(sucursal)
                .tipoComprobante("00")
                .serie("NV01")
                .correlativo(0)
                .build();

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        when(serieRepository.findByTipoComprobanteAndSerie("00", "NV01")).thenReturn(Optional.of(serieTicket));
        when(correlativoServicePort.obtenerSiguienteCorrelativo(any(), eq("00"), eq("NV01"))).thenReturn(1);

        CrearComprobanteRequest request = CrearComprobanteRequest.builder()
                .comprobante(ComprobanteDataRequest.builder()
                        .tipo("00")
                        .serie("NV01")
                        .claveIdempotencia("ticket-nv-1")
                        .build())
                .cliente(ClienteDto.builder()
                        .tipoDocumento("1")
                        .numeroDocumento("76543210")
                        .nombreRazonSocial("Juan Perez")
                        .build())
                .items(List.of(
                        ItemComprobanteRequest.builder()
                                .descripcion("Gaseosa Inka Kola")
                                .cantidad(new BigDecimal("1.0000"))
                                .valor(new BigDecimal("3.50"))
                                .codigoAfectacionSunat("10")
                                .build(),
                        ItemComprobanteRequest.builder()
                                .descripcion("Bolsa Plastica")
                                .cantidad(new BigDecimal("1.0000"))
                                .valor(new BigDecimal("0.70"))
                                .codigoAfectacionSunat("10")
                                .icbper(true)
                                .cantidadBolsasIcbper(1)
                                .build()
                ))
                .build();

        ComprobanteFiscal resultado = emitirComprobanteService.emitir(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTipoComprobante()).isEqualTo(TipoComprobante.TICKET_INTERNO);
        assertThat(resultado.getSerie()).isEqualTo("NV01");
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("4.20"));
        assertThat(resultado.getTotalTributos()).isEqualByComparingTo(new BigDecimal("1.06"));
        assertThat(resultado.getEstadoInterno()).isEqualTo("REGISTRADO");

        // IMPORTANTE: Para tickets internos, el procesador electrónico NO debe ejecutarse
        verify(procesadorComprobanteElectronicoService, never()).procesarFirmaYEnvio(any(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar error 'Falta certificado para firmar el comprobante' si el certificado no existe")
    void emitirComprobante_fallaSiFaltaCertificado() {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20100070970")
                .razonSocial("Supermercado Central S.A.C.")
                .build();

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        when(certificadoDigitalService.existeCertificado(any())).thenReturn(false);

        CrearComprobanteRequest request = CrearComprobanteRequest.builder()
                .comprobante(ComprobanteDataRequest.builder()
                        .tipo("01")
                        .serie("F001")
                        .build())
                .cliente(ClienteDto.builder().tipoDocumento("6").numeroDocumento("20555555551").nombreRazonSocial("Cliente Test").build())
                .items(List.of(ItemComprobanteRequest.builder().descripcion("Item").cantidad(BigDecimal.ONE).valor(BigDecimal.TEN).codigoAfectacionSunat("10").build()))
                .build();

        assertThatThrownBy(() -> emitirComprobanteService.emitir(request))
                .isInstanceOf(DomainException.class)
                .hasMessage("Falta certificado para firmar el comprobante");
    }

    @Test
    @DisplayName("Debe lanzar error 'Faltan datos de configuración de la empresa' si faltan credenciales SOL")
    void emitirComprobante_fallaSiFaltanCredencialesSOL() {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20100070970")
                .razonSocial("Supermercado Central S.A.C.")
                .build();

        com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.EmpresaConfig configSinCredenciales = 
                com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.EmpresaConfig.builder()
                        .userSol("")
                        .passSol("")
                        .build();

        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        when(empresaConfigRepository.findByEmpresaId(any())).thenReturn(Optional.of(configSinCredenciales));

        CrearComprobanteRequest request = CrearComprobanteRequest.builder()
                .comprobante(ComprobanteDataRequest.builder()
                        .tipo("01")
                        .serie("F001")
                        .build())
                .cliente(ClienteDto.builder().tipoDocumento("6").numeroDocumento("20555555551").nombreRazonSocial("Cliente Test").build())
                .items(List.of(ItemComprobanteRequest.builder().descripcion("Item").cantidad(BigDecimal.ONE).valor(BigDecimal.TEN).codigoAfectacionSunat("10").build()))
                .build();

        assertThatThrownBy(() -> emitirComprobanteService.emitir(request))
                .isInstanceOf(DomainException.class)
                .hasMessage("Faltan datos de configuración de la empresa");
    }
}
