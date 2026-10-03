package com.s1nt4xSystem.facturacion_sunat.platform.account.validation;

import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.account.repository.CuentaSaasRepository;
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
class CuentaSaasValidatorTest {

    @Mock
    private CuentaSaasRepository cuentaSaasRepository;

    @InjectMocks
    private CuentaSaasValidator cuentaSaasValidator;

    @Test
    @DisplayName("Debe lanzar excepción si el nombre de la cuenta está vacío")
    void validateNewAccount_nombreVacio() {
        CuentaSaasRequest req = CuentaSaasRequest.builder().nombre("").planId(1).build();

        assertThatThrownBy(() -> cuentaSaasValidator.validateNewAccount(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NAME_REQUIRED));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el plan no se especifica")
    void validateNewAccount_planNulo() {
        CuentaSaasRequest req = CuentaSaasRequest.builder().nombre("Empresa Corp").planId(null).build();

        assertThatThrownBy(() -> cuentaSaasValidator.validateNewAccount(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_PLAN_REQUIRED));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la accessKey ya existe")
    void validateNewAccount_accessKeyDuplicada() {
        when(cuentaSaasRepository.existsByAccessKey("ak_existente")).thenReturn(true);
        CuentaSaasRequest req = CuentaSaasRequest.builder().nombre("Empresa Corp").planId(1).accessKey("ak_existente").build();

        assertThatThrownBy(() -> cuentaSaasValidator.validateNewAccount(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_ACCESS_KEY_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("Debe validar exitosamente cuando los datos son correctos")
    void validateNewAccount_valido() {
        when(cuentaSaasRepository.existsByAccessKey("ak_nueva")).thenReturn(false);
        CuentaSaasRequest req = CuentaSaasRequest.builder().nombre("Empresa Corp").planId(1).accessKey("ak_nueva").build();

        assertThatCode(() -> cuentaSaasValidator.validateNewAccount(req))
                .doesNotThrowAnyException();
    }
}
