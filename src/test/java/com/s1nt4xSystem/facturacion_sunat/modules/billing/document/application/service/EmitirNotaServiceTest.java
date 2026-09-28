package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.CatalogoFiscalPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.CorrelativoServicePort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.DocumentoRepositoryPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.repository.SerieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmitirNotaServiceTest {

    @Mock
    private DocumentoRepositoryPort documentoRepositoryPort;

    @Mock
    private CorrelativoServicePort correlativoServicePort;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private SucursalRepository sucursalRepository;

    @Mock
    private SerieRepository serieRepository;

    @Mock
    private CatalogoFiscalPort catalogoFiscalPort;

    @Mock
    private ProcesadorComprobanteElectronicoService procesadorComprobanteElectronicoService;

    @Mock
    private com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaConfigRepository empresaConfigRepository;

    @Mock
    private com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.CertificadoDigitalService certificadoDigitalService;

    @InjectMocks
    private EmitirNotaService emitirNotaService;

    private Empresa empresa;
    private Sucursal sucursal;
    private Serie serieNC;
    private ComprobanteFiscal facturaOriginal;

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

        empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20123456789")
                .razonSocial("Empresa Demo SAC")
                .incluidoTributo(true)
                .build();

        sucursal = Sucursal.builder()
                .id(UUID.randomUUID())
                .codigo("0000")
                .nombreSucursal("Casa Matriz")
                .impuestoPorcentaje(new BigDecimal("18.00"))
                .activo(true)
                .build();

        serieNC = Serie.builder()
                .id(UUID.randomUUID())
                .sucursal(sucursal)
                .tipoComprobante("07")
                .serie("FC01")
                .correlativo(0)
                .build();

        facturaOriginal = ComprobanteFiscal.builder()
                .id(UUID.randomUUID())
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .numero(123)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20601234567")
                .clienteNombre("Cliente ABC S.A.C.")
                .clienteDireccion("Av. Las Flores 123")
                .subtotal(new BigDecimal("100.00"))
                .totalTributos(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .detalles(List.of(
                        LineaComprobante.builder()
                                .item(1)
                                .codigoProducto("PROD-01")
                                .descripcion("Monitor Gamer")
                                .cantidad(new BigDecimal("1.0000"))
                                .unidadMedida("NIU")
                                .valorUnitario(new BigDecimal("100.00"))
                                .precioUnitario(new BigDecimal("118.00"))
                                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                                .subtotal(new BigDecimal("100.00"))
                                .totalTributos(new BigDecimal("18.00"))
                                .total(new BigDecimal("118.00"))
                                .build()
                ))
                .build();

        when(documentoRepositoryPort.buscarPorClaveIdempotencia(any())).thenReturn(Optional.empty());
        when(empresaRepository.findAll()).thenReturn(List.of(empresa));
        when(sucursalRepository.findByCodigo(any())).thenReturn(Optional.of(sucursal));
        when(serieRepository.findFirstBySucursalIdAndTipoComprobante(any(), eq("07"))).thenReturn(Optional.of(serieNC));
        when(catalogoFiscalPort.determinarTasaIgvPorSucursal(any())).thenReturn(new BigDecimal("18.00"));
        when(catalogoFiscalPort.obtenerTasaIcbperVigente()).thenReturn(new BigDecimal("0.50"));
        when(correlativoServicePort.obtenerSiguienteCorrelativo(any(), eq("07"), any())).thenReturn(1);
        when(documentoRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Nota de Crédito Total clona automáticamente cliente e items de factura interna original")
    void emitirNota_anulacionTotalInterna_clonaDetallesCorrectamente() {
        when(documentoRepositoryPort.buscarPorId(facturaOriginal.getId())).thenReturn(Optional.of(facturaOriginal));

        CrearNotaRequest request = CrearNotaRequest.builder()
                .emisor(EmisorDto.builder().ruc("20123456789").codigoSucursal("0000").build())
                .nota(NotaDataRequest.builder()
                        .claveIdempotencia("nc-test-total-1")
                        .tipo("07")
                        .motivoCodigo("01")
                        .motivoDescripcion("Anulación total de la operación")
                        .build())
                .documentoReferencia(DocumentoReferenciaRequest.builder()
                        .tipoRelacion("afecta")
                        .documentoReferenciadoId(facturaOriginal.getId())
                        .build())
                .build();

        ComprobanteFiscal resultado = emitirNotaService.emitirNota(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTipoComprobante()).isEqualTo(TipoComprobante.NOTA_CREDITO);
        assertThat(resultado.getSerie()).isEqualTo("FC01");
        assertThat(resultado.getNumero()).isEqualTo(1);
        assertThat(resultado.getClienteNombre()).isEqualTo("Cliente ABC S.A.C.");
        assertThat(resultado.getDetalles()).hasSize(1);
        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resultado.getTotalTributos()).isEqualByComparingTo(new BigDecimal("18.00"));
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("118.00"));
        assertThat(resultado.getReferencias()).hasSize(1);
        assertThat(resultado.getReferencias().get(0).getSerieRef()).isEqualTo("F001");
        assertThat(resultado.getReferencias().get(0).getNumeroRef()).isEqualTo(123);
    }

    @Test
    @DisplayName("Nota de Crédito Parcial sobre documento externo con items manuales")
    void emitirNota_documentoExterno_calculaTributosCorrectamente() {
        CrearNotaRequest request = CrearNotaRequest.builder()
                .emisor(EmisorDto.builder().ruc("20123456789").codigoSucursal("0000").build())
                .nota(NotaDataRequest.builder()
                        .claveIdempotencia("nc-test-ext-1")
                        .tipo("07")
                        .motivoCodigo("07")
                        .motivoDescripcion("Devolución parcial")
                        .moneda("PEN")
                        .build())
                .documentoReferencia(DocumentoReferenciaRequest.builder()
                        .tipoRelacion("afecta")
                        .tipoDocumentoRef("01")
                        .serieRef("F001")
                        .numeroRef(999)
                        .monedaRef("PEN")
                        .build())
                .cliente(ClienteDto.builder()
                        .tipoDocumento("6")
                        .numeroDocumento("20601234567")
                        .nombreRazonSocial("Cliente Externo SAC")
                        .build())
                .items(List.of(
                        ItemComprobanteRequest.builder()
                                .descripcion("Teclado Mecanico")
                                .unidadMedida("NIU")
                                .codigoAfectacionSunat("10")
                                .cantidad(new BigDecimal("1.0000"))
                                .valor(new BigDecimal("118.00")) // con IGV porque incluidoTributo = true
                                .build()
                ))
                .build();

        ComprobanteFiscal resultado = emitirNotaService.emitirNota(request);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resultado.getTotalTributos()).isEqualByComparingTo(new BigDecimal("18.00"));
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("118.00"));
        assertThat(resultado.getReferencias().get(0).getSerieRef()).isEqualTo("F001");
        assertThat(resultado.getReferencias().get(0).getNumeroRef()).isEqualTo(999);
    }
}
