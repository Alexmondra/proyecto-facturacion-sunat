package com.s1nt4xSystem.facturacion_sunat.modules.billing.series.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.service.SucursalService;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.EmpresaTenantService;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SerieServiceTest {

    @Mock
    private SerieRepository serieRepository;

    @Mock
    private SucursalService sucursalService;

    @Mock
    private EmpresaTenantService empresaTenantService;

    @InjectMocks
    private SerieServiceImpl serieService;

    private Empresa empresa;
    private Sucursal sucursal;
    private UUID empresaId;
    private UUID sucursalId;

    @BeforeEach
    void setUp() {
        empresaId = UUID.randomUUID();
        empresa = Empresa.builder()
                .id(empresaId)
                .ruc("20000000001")
                .razonSocial("EMPRESA TEST")
                .build();

        sucursalId = UUID.randomUUID();
        sucursal = Sucursal.builder()
                .id(sucursalId)
                .empresa(empresa)
                .codigo("0000")
                .nombreSucursal("Matriz Principal")
                .build();
    }

    @Test
    @DisplayName("Debe crear serie exitosamente si la sucursal pertenece a la empresa")
    void createSerie_exitoso() {
        SerieRequest request = SerieRequest.builder()
                .sucursalId(sucursalId)
                .tipoComprobante("01")
                .serie("F001")
                .correlativo(0)
                .build();

        when(empresaTenantService.getEmpresaEntity()).thenReturn(empresa);
        when(sucursalService.getSucursalEntity(sucursalId)).thenReturn(sucursal);
        when(serieRepository.existsByTipoComprobanteAndSerie("01", "F001")).thenReturn(false);
        when(serieRepository.save(any(Serie.class))).thenAnswer(inv -> {
            Serie s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SerieResponse response = serieService.createSerie(request);

        assertThat(response).isNotNull();
        assertThat(response.getSerie()).isEqualTo("F001");
        assertThat(response.getTipoComprobante()).isEqualTo("01");
        assertThat(response.getSucursalId()).isEqualTo(sucursalId);
    }

    @Test
    @DisplayName("Debe fallar al crear serie si ya existe en cualquier sucursal de la empresa")
    void createSerie_fallaSiSerieYaExisteEnEmpresa() {
        SerieRequest request = SerieRequest.builder()
                .sucursalId(sucursalId)
                .tipoComprobante("01")
                .serie("F001")
                .build();

        when(empresaTenantService.getEmpresaEntity()).thenReturn(empresa);
        when(sucursalService.getSucursalEntity(sucursalId)).thenReturn(sucursal);
        when(serieRepository.existsByTipoComprobanteAndSerie("01", "F001")).thenReturn(true);

        assertThatThrownBy(() -> serieService.createSerie(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ya está registrada en la empresa");

        verify(serieRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar al crear serie si la sucursal pertenece a otra empresa")
    void createSerie_fallaSiSucursalDeOtraEmpresa() {
        Empresa otraEmpresa = Empresa.builder().id(UUID.randomUUID()).ruc("20999999999").build();
        Sucursal sucursalAjena = Sucursal.builder().id(sucursalId).empresa(otraEmpresa).build();

        SerieRequest request = SerieRequest.builder()
                .sucursalId(sucursalId)
                .tipoComprobante("01")
                .serie("F001")
                .build();

        when(empresaTenantService.getEmpresaEntity()).thenReturn(empresa);
        when(sucursalService.getSucursalEntity(sucursalId)).thenReturn(sucursalAjena);

        assertThatThrownBy(() -> serieService.createSerie(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no pertenece a la empresa actual");

        verify(serieRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe actualizar serie exitosamente con PATCH unificado")
    void updateSerie_exitoso() {
        UUID serieId = UUID.randomUUID();
        Serie serie = Serie.builder()
                .id(serieId)
                .sucursal(sucursal)
                .tipoComprobante("01")
                .serie("F001")
                .correlativo(0)
                .build();

        SerieUpdateRequest updateRequest = SerieUpdateRequest.builder()
                .serie("F002")
                .correlativo(5)
                .build();

        when(serieRepository.findById(serieId)).thenReturn(Optional.of(serie));
        when(empresaTenantService.getEmpresaEntity()).thenReturn(empresa);
        when(serieRepository.findByTipoComprobanteAndSerie("01", "F002")).thenReturn(Optional.empty());
        when(serieRepository.save(any(Serie.class))).thenAnswer(inv -> inv.getArgument(0));

        SerieResponse response = serieService.updateSerie(serieId, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getSerie()).isEqualTo("F002");
        assertThat(response.getCorrelativo()).isEqualTo(5);
    }

    @Test
    @DisplayName("Debe fallar cambiar código de serie si ya tiene comprobantes emitidos")
    void updateSerie_fallaSiYaTieneComprobantes() {
        UUID serieId = UUID.randomUUID();
        Serie serie = Serie.builder()
                .id(serieId)
                .sucursal(sucursal)
                .tipoComprobante("01")
                .serie("F001")
                .correlativo(10)
                .build();

        SerieUpdateRequest updateRequest = SerieUpdateRequest.builder()
                .serie("F002")
                .build();

        when(serieRepository.findById(serieId)).thenReturn(Optional.of(serie));
        when(empresaTenantService.getEmpresaEntity()).thenReturn(empresa);

        assertThatThrownBy(() -> serieService.updateSerie(serieId, updateRequest))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ya tiene comprobantes emitidos");
    }
}
