package com.s1nt4xSystem.facturacion_sunat.infrastructure.multitenancy.validation;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal;
import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantValidatorTest {

    @Mock
    private EmpresaRouterRepository empresaRouterRepository;

    @InjectMocks
    private TenantValidator tenantValidator;

    private EmpresaRouter router;

    @BeforeEach
    void setUp() {
        CuentaSaas saas = CuentaSaas.builder().id(100L).nombre("Cuenta Demo").build();
        router = EmpresaRouter.builder()
                .id(1L)
                .ruc("20100070970")
                .nombre("Empresa Principal")
                .dbSchema("tenant_20100070970")
                .cuentaSaas(saas)
                .estado("ACTIVO")
                .build();
    }

    @Test
    @DisplayName("Debe lanzar excepción si no hay autenticación")
    void validateAuthenticated_nulo() {
        assertThatThrownBy(() -> tenantValidator.validateAuthenticated(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TENANT_AUTH_REQUIRED));
    }

    @Test
    @DisplayName("Debe validar y retornar esquema para access key de empresa")
    void resolveSchemaForEmpresa_valido() {
        String schema = tenantValidator.resolveSchemaForEmpresa(router, "20100070970");
        assertThat(schema).isEqualTo("tenant_20100070970");
    }

    @Test
    @DisplayName("Debe lanzar excepción si access key de empresa intenta acceder a otro tenant")
    void resolveSchemaForEmpresa_accesoDenegado() {
        assertThatThrownBy(() -> tenantValidator.resolveSchemaForEmpresa(router, "20999999999"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TENANT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("Debe requerir cabecera X-Tenant-ID para rol SaaS o Admin")
    void resolveAndValidateForSaasOrAdmin_faltaCabecera() {
        assertThatThrownBy(() -> tenantValidator.resolveAndValidateForSaasOrAdmin(null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TENANT_HEADER_REQUIRED));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la empresa no existe en empresas_router")
    void resolveAndValidateForSaasOrAdmin_empresaNoEncontrada() {
        when(empresaRouterRepository.findByRuc("20999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantValidator.resolveAndValidateForSaasOrAdmin("20999999999", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TENANT_NOT_FOUND));
    }

    @Test
    @DisplayName("Debe denegar acceso si la empresa pertenece a otra cuenta SaaS")
    void resolveAndValidateForSaasOrAdmin_otraCuentaSaas() {
        when(empresaRouterRepository.findByRuc("20100070970")).thenReturn(Optional.of(router));
        AuthenticatedPrincipal principalCliente = AuthenticatedPrincipal.builder()
                .saasId(999L)
                .tipo("CLIENTE")
                .build();

        assertThatThrownBy(() -> tenantValidator.resolveAndValidateForSaasOrAdmin("20100070970", principalCliente))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TENANT_SAAS_ACCESS_DENIED));
    }

    @Test
    @DisplayName("Debe denegar acceso si la empresa está inactiva o suspendida")
    void resolveAndValidateForSaasOrAdmin_empresaInactiva() {
        router.setEstado("INACTIVO");
        when(empresaRouterRepository.findByRuc("20100070970")).thenReturn(Optional.of(router));

        assertThatThrownBy(() -> tenantValidator.resolveAndValidateForSaasOrAdmin("20100070970", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TENANT_INACTIVE));
    }

    @Test
    @DisplayName("Debe validar esquema PostgreSQL válido")
    void validateSchemaName_valido() {
        assertThatCode(() -> tenantValidator.validateSchemaName("tenant_20100070970"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe rechazar esquema con caracteres inválidos (prevención inyección SQL)")
    void validateSchemaName_invalido() {
        assertThatThrownBy(() -> tenantValidator.validateSchemaName("tenant; DROP TABLE users;"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TENANT_INVALID_SCHEMA));
    }
}
