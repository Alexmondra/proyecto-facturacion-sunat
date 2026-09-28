package com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto.SucursalRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto.SucursalResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.EmpresaTenantService;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoUbigeo;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository.CatalogoUbigeoRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SucursalServiceTest {

    @Mock
    private SucursalRepository sucursalRepository;

    @Mock
    private EmpresaTenantService empresaTenantService;

    @Mock
    private CatalogoUbigeoRepository catalogoUbigeoRepository;

    @Mock
    private com.s1nt4xSystem.facturacion_sunat.modules.billing.series.repository.SerieRepository serieRepository;

    @InjectMocks
    private SucursalServiceImpl sucursalService;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20100070970")
                .razonSocial("Supermercado Central S.A.C.")
                .build();
        lenient().when(serieRepository.save(any(com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(serieRepository.findAll()).thenReturn(new java.util.ArrayList<>());
    }

    @Test
    @DisplayName("Debe lanzar DomainException si el ubigeo no existe en el catálogo oficial")
    void createSucursal_debeFallarSiUbigeoInvalido() {
        SucursalRequest request = SucursalRequest.builder()
                .codigo("0001")
                .nombreSucursal("Sucursal Invalida")
                .ubigeo("999999")
                .build();

        when(empresaTenantService.getEmpresaEntity()).thenReturn(empresa);
        when(sucursalRepository.existsByEmpresaIdAndCodigo(empresa.getId(), "0001")).thenReturn(false);
        when(sucursalRepository.existsByCodigo("0001")).thenReturn(false);
        when(catalogoUbigeoRepository.findByCodigoAndEstadoTrue("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sucursalService.createSucursal(request))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no existe en el catálogo oficial");

        verify(sucursalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe asignar 0.00% de impuesto automáticamente si el ubigeo es de Amazonía (ej. Loreto/Iquitos)")
    void createSucursal_debeAsignarCeroImpuestoEnAmazonia() {
        SucursalRequest request = SucursalRequest.builder()
                .codigo("0002")
                .nombreSucursal("Sucursal Iquitos")
                .ubigeo("160101")
                .impuestoPorcentaje(null)
                .build();

        CatalogoUbigeo ubigeoIquitos = CatalogoUbigeo.builder()
                .codigo("160101")
                .departamento("Loreto")
                .provincia("Maynas")
                .distrito("Iquitos")
                .esAmazonia(true)
                .estado(true)
                .build();

        when(empresaTenantService.getEmpresaEntity()).thenReturn(empresa);
        when(sucursalRepository.existsByEmpresaIdAndCodigo(empresa.getId(), "0002")).thenReturn(false);
        when(sucursalRepository.existsByCodigo("0002")).thenReturn(false);
        when(catalogoUbigeoRepository.findByCodigoAndEstadoTrue("160101")).thenReturn(Optional.of(ubigeoIquitos));
        when(sucursalRepository.save(any(Sucursal.class))).thenAnswer(invocation -> {
            Sucursal s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SucursalResponse response = sucursalService.createSucursal(request);

        assertThat(response).isNotNull();
        assertThat(response.getUbigeo()).isEqualTo("160101");
        assertThat(response.getDepartamento()).isEqualTo("Loreto");
        assertThat(response.getEsAmazonia()).isTrue();
        assertThat(response.getImpuestoPorcentaje()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Debe asignar 18.00% si el ubigeo es de Lima y no se especifica impuesto")
    void createSucursal_debeAsignar18PorcientoFueraDeAmazonia() {
        SucursalRequest request = SucursalRequest.builder()
                .codigo("0003")
                .nombreSucursal("Sucursal Miraflores")
                .ubigeo("150122")
                .impuestoPorcentaje(null)
                .build();

        CatalogoUbigeo ubigeoMiraflores = CatalogoUbigeo.builder()
                .codigo("150122")
                .departamento("Lima")
                .provincia("Lima")
                .distrito("Miraflores")
                .esAmazonia(false)
                .estado(true)
                .build();

        when(empresaTenantService.getEmpresaEntity()).thenReturn(empresa);
        when(sucursalRepository.existsByEmpresaIdAndCodigo(empresa.getId(), "0003")).thenReturn(false);
        when(sucursalRepository.existsByCodigo("0003")).thenReturn(false);
        when(catalogoUbigeoRepository.findByCodigoAndEstadoTrue("150122")).thenReturn(Optional.of(ubigeoMiraflores));
        when(sucursalRepository.save(any(Sucursal.class))).thenAnswer(invocation -> {
            Sucursal s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SucursalResponse response = sucursalService.createSucursal(request);

        assertThat(response).isNotNull();
        assertThat(response.getUbigeo()).isEqualTo("150122");
        assertThat(response.getDepartamento()).isEqualTo("Lima");
        assertThat(response.getEsAmazonia()).isFalse();
        assertThat(response.getImpuestoPorcentaje()).isEqualByComparingTo(new BigDecimal("18.00"));
    }
}
