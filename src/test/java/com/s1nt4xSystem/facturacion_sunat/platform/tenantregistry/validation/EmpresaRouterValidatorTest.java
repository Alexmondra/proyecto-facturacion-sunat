package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.validation;

import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaOnboardingRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaRouterResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpresaRouterValidatorTest {

    @Mock
    private EmpresaRouterRepository empresaRouterRepository;

    @InjectMocks
    private EmpresaRouterValidator empresaRouterValidator;

    @Test
    @DisplayName("Debe lanzar excepción si el RUC es inválido")
    void validateOnboarding_rucInvalido() {
        EmpresaOnboardingRequest req = EmpresaOnboardingRequest.builder()
                .ruc("123456")
                .razonSocial("Empresa SAC")
                .saasId(1L)
                .build();

        assertThatThrownBy(() -> empresaRouterValidator.validateOnboarding(req, "tenant_123456", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ONBOARDING_INVALID_RUC));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la razón social está vacía")
    void validateOnboarding_razonSocialVacia() {
        EmpresaOnboardingRequest req = EmpresaOnboardingRequest.builder()
                .ruc("20100070970")
                .razonSocial("  ")
                .saasId(1L)
                .build();

        assertThatThrownBy(() -> empresaRouterValidator.validateOnboarding(req, "tenant_20100070970", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ONBOARDING_RAZON_SOCIAL_REQUIRED));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el RUC ya existe")
    void validateOnboarding_rucDuplicado() {
        when(empresaRouterRepository.existsByRuc("20100070970")).thenReturn(true);
        EmpresaOnboardingRequest req = EmpresaOnboardingRequest.builder()
                .ruc("20100070970")
                .razonSocial("Empresa SAC")
                .saasId(1L)
                .build();

        assertThatThrownBy(() -> empresaRouterValidator.validateOnboarding(req, "tenant_20100070970", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ONBOARDING_RUC_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el esquema ya existe")
    void validateOnboarding_schemaDuplicado() {
        when(empresaRouterRepository.existsByRuc("20100070970")).thenReturn(false);
        when(empresaRouterRepository.existsByDbSchema("tenant_20100070970")).thenReturn(true);
        EmpresaOnboardingRequest req = EmpresaOnboardingRequest.builder()
                .ruc("20100070970")
                .razonSocial("Empresa SAC")
                .saasId(1L)
                .build();

        assertThatThrownBy(() -> empresaRouterValidator.validateOnboarding(req, "tenant_20100070970", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ONBOARDING_SCHEMA_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("Debe validar exitosamente cuando los datos son correctos")
    void validateOnboarding_valido() {
        when(empresaRouterRepository.existsByRuc("20100070970")).thenReturn(false);
        when(empresaRouterRepository.existsByDbSchema("tenant_20100070970")).thenReturn(false);
        EmpresaOnboardingRequest req = EmpresaOnboardingRequest.builder()
                .ruc("20100070970")
                .razonSocial("Empresa SAC")
                .saasId(1L)
                .build();

        assertThatCode(() -> empresaRouterValidator.validateOnboarding(req, "tenant_20100070970", null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe lanzar excepción si el estado no es ACTIVO, INACTIVO o SUSPENDIDO")
    void validateEstado_invalido() {
        assertThatThrownBy(() -> empresaRouterValidator.validateEstado("BORRADOR"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ONBOARDING_INVALID_STATE));
    }

    @Test
    @DisplayName("Debe denegar acceso si la empresa no pertenece a la cuenta SaaS")
    void validateAccountOwnership_otraCuenta() {
        EmpresaRouterResponse resp = EmpresaRouterResponse.builder().id(1L).saasId(10L).build();

        assertThatThrownBy(() -> empresaRouterValidator.validateAccountOwnership(resp, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ONBOARDING_ACCOUNT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("Debe denegar acceso cruzado a otra cuenta SaaS")
    void validateSameAccountAccess_cuentaDiferente() {
        assertThatThrownBy(() -> empresaRouterValidator.validateSameAccountAccess(50L, 100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ONBOARDING_CROSS_ACCOUNT_DENIED));
    }
}
