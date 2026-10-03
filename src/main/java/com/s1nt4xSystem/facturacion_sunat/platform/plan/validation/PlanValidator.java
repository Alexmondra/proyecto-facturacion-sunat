package com.s1nt4xSystem.facturacion_sunat.platform.plan.validation;

import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.model.Plan;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.repository.PlanRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Validador centralizado para la administración de Planes Comerciales (Platform Plan).
 */
@Component
public class PlanValidator {

    private final PlanRepository planRepository;

    public PlanValidator(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    /**
     * Valida la creación de un nuevo plan comercial.
     */
    public void validateNewPlan(PlanRequest request) {
        if (request.getCodigo() == null || request.getCodigo().isBlank()) {
            throw new BusinessException(ErrorCode.PLAN_CODE_REQUIRED);
        }
        if (request.getNombrePlan() == null || request.getNombrePlan().isBlank()) {
            throw new BusinessException(ErrorCode.PLAN_NAME_REQUIRED);
        }
        if (planRepository.existsByCodigo(request.getCodigo().trim())) {
            throw new BusinessException(ErrorCode.PLAN_CODE_ALREADY_EXISTS, request.getCodigo().trim());
        }
        if (request.getPrecioMensual() != null && request.getPrecioMensual().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PLAN_PRICE_INVALID);
        }
        if (request.getLimiteMensualBolsa() != null && request.getLimiteMensualBolsa() < 0) {
            throw new BusinessException(ErrorCode.PLAN_LIMIT_INVALID);
        }
    }

    /**
     * Valida la actualización de un plan comercial existente.
     */
    public void validateUpdatePlan(Plan plan, PlanRequest request) {
        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            String cleanCode = request.getCodigo().trim();
            if (!cleanCode.equalsIgnoreCase(plan.getCodigo())
                    && planRepository.existsByCodigo(cleanCode)) {
                throw new BusinessException(ErrorCode.PLAN_CODE_IN_USE, cleanCode);
            }
        }
        if (request.getPrecioMensual() != null && request.getPrecioMensual().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PLAN_PRICE_INVALID);
        }
        if (request.getLimiteMensualBolsa() != null && request.getLimiteMensualBolsa() < 0) {
            throw new BusinessException(ErrorCode.PLAN_LIMIT_INVALID);
        }
    }
}
