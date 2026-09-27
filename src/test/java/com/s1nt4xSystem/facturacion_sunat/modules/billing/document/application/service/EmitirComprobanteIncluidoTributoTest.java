package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto.ComprobanteItemCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto.EmitirComprobanteCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoAfectacionIgv;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private EmitirComprobanteService emitirComprobanteService;

    private Sucursal sucursal;
    private Serie serie;

    @BeforeEach
    void setUp() {
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

        when(documentoRepositoryPort.buscarPorClaveIdempotencia(any())).thenReturn(Optional.empty());
        when(sucursalRepository.findById(any())).thenReturn(Optional.of(sucursal));
        when(serieRepository.findBySucursalIdAndTipoComprobanteAndSerie(any(), any(), any())).thenReturn(Optional.of(serie));
        when(catalogoFiscalPort.determinarTasaIgvPorSucursal(any())).thenReturn(new BigDecimal("18.00"));
        when(catalogoFiscalPort.obtenerTasaIcbperVigente()).thenReturn(new BigDecimal("0.50"));
        when(correlativoServicePort.obtenerSiguienteCorrelativo(any(), any(), any())).thenReturn(1);
        when(documentoRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
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
}
