package com.s1nt4xSystem.facturacion_sunat.platform.plan.validation;

import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.model.Plan;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.repository.PlanRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanValidatorTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanValidator planValidator;

    @Test
    @DisplayName("Debe lanzar excepción si el código del plan es nulo o vacío")
    void validateNewPlan_codigoVacio() {
        PlanRequest req = PlanRequest.builder().codigo("").nombrePlan("Plan Pro").build();

        assertThatThrownBy(() -> planValidator.validateNewPlan(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PLAN_CODE_REQUIRED));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el nombre del plan es nulo o vacío")
    void validateNewPlan_nombreVacio() {
        PlanRequest req = PlanRequest.builder().codigo("PRO").nombrePlan(" ").build();

        assertThatThrownBy(() -> planValidator.validateNewPlan(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PLAN_NAME_REQUIRED));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el código del plan ya existe")
    void validateNewPlan_codigoDuplicado() {
        when(planRepository.existsByCodigo("PRO")).thenReturn(true);
        PlanRequest req = PlanRequest.builder().codigo("PRO").nombrePlan("Plan Pro").build();

        assertThatThrownBy(() -> planValidator.validateNewPlan(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PLAN_CODE_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el precio mensual es negativo")
    void validateNewPlan_precioNegativo() {
        when(planRepository.existsByCodigo("PRO")).thenReturn(false);
        PlanRequest req = PlanRequest.builder().codigo("PRO").nombrePlan("Plan Pro").precioMensual(new BigDecimal("-10.00")).build();

        assertThatThrownBy(() -> planValidator.validateNewPlan(req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PLAN_PRICE_INVALID));
    }

    @Test
    @DisplayName("Debe validar exitosamente plan con datos correctos")
    void validateNewPlan_valido() {
        when(planRepository.existsByCodigo("PRO")).thenReturn(false);
        PlanRequest req = PlanRequest.builder().codigo("PRO").nombrePlan("Plan Pro")
                .precioMensual(new BigDecimal("99.00")).limiteMensualBolsa(5000).build();

        assertThatCode(() -> planValidator.validateNewPlan(req))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe rechazar actualización si el nuevo código ya está en uso por otro plan")
    void validateUpdatePlan_codigoEnUso() {
        Plan plan = Plan.builder().id(1).codigo("BASIC").nombrePlan("Plan Basico").build();
        when(planRepository.existsByCodigo("PRO")).thenReturn(true);

        PlanRequest req = PlanRequest.builder().codigo("PRO").nombrePlan("Plan Pro").build();

        assertThatThrownBy(() -> planValidator.validateUpdatePlan(plan, req))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PLAN_CODE_IN_USE));
    }
}
