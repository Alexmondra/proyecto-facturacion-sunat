package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.service;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.liquibase.TenantLiquibaseMigrator;
import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import com.s1nt4xSystem.facturacion_sunat.platform.account.service.CuentaSaasService;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaOnboardingRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaRouterResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.validation.EmpresaRouterValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaOnboardingServiceImplTest {

    @Mock
    private EmpresaRouterRepository empresaRouterRepository;

    @Mock
    private CuentaSaasService cuentaSaasService;

    @Mock
    private TenantLiquibaseMigrator tenantLiquibaseMigrator;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private EmpresaRouterValidator empresaRouterValidator;

    @TempDir
    Path tempDir;

    private EmpresaOnboardingServiceImpl onboardingService;

    @BeforeEach
    void setUp() {
        onboardingService = new EmpresaOnboardingServiceImpl(
                empresaRouterRepository,
                cuentaSaasService,
                tenantLiquibaseMigrator,
                jdbcTemplate,
                tempDir.toString(),
                empresaRouterValidator
        );
    }

    @Test
    @DisplayName("Debe aprovisionar esquema tenant y sembrar empresa, config, sucursales, series y plantillas_impresion (A4 y TICKET_80)")
    void onboardEmpresa_exitoso_siembraPlantillas() {
        EmpresaOnboardingRequest request = EmpresaOnboardingRequest.builder()
                .ruc("20601234567")
                .razonSocial("MI TIENDA TECH S.A.C.")
                .direccionFiscal("Av. Los Pinos 456, Lima")
                .saasId(1L)
                .build();

        CuentaSaas cuentaSaas = CuentaSaas.builder().id(1L).nombre("Cuenta Demo").build();
        when(cuentaSaasService.getAccountEntity(1L)).thenReturn(cuentaSaas);

        when(empresaRouterRepository.save(any(EmpresaRouter.class))).thenAnswer(inv -> {
            EmpresaRouter r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        EmpresaRouterResponse response = onboardingService.onboardEmpresa(request);

        assertThat(response).isNotNull();
        assertThat(response.getRuc()).isEqualTo("20601234567");
        assertThat(response.getDbSchema()).isEqualTo("tenant_20601234567");

        // Verifica que se ejecutó la migración del esquema tenant
        verify(tenantLiquibaseMigrator).migrateTenant("tenant_20601234567");

        // Verifica que se sembraron las plantillas de impresión A4 y TICKET_80
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeast(4)).update(sqlCaptor.capture());

        boolean plantillasSeeded = sqlCaptor.getAllValues().stream()
                .anyMatch(sql -> sql.contains("plantillas_impresion") 
                        && sql.contains("TICKET_80") 
                        && sql.contains("A4") 
                        && sql.contains("plantillas_base"));

        assertThat(plantillasSeeded).isTrue();
    }
}
