package com.s1nt4xSystem.facturacion_sunat.platform.account.validation;

import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.account.repository.CuentaSaasRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Validador centralizado para la administración de Cuentas SaaS (Platform Account).
 */
@Component
public class CuentaSaasValidator {

    private final CuentaSaasRepository cuentaSaasRepository;

    public CuentaSaasValidator(CuentaSaasRepository cuentaSaasRepository) {
        this.cuentaSaasRepository = cuentaSaasRepository;
    }

    /**
     * Valida los datos para la creación de una nueva Cuenta SaaS.
     */
    public void validateNewAccount(CuentaSaasRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NAME_REQUIRED);
        }
        if (request.getPlanId() == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_PLAN_REQUIRED);
        }
        if (request.getAccessKey() != null && !request.getAccessKey().trim().isEmpty()) {
            validateAccessKeyUnique(request.getAccessKey().trim());
        }
    }

    /**
     * Valida que una accessKey no esté previamente registrada.
     */
    public void validateAccessKeyUnique(String accessKey) {
        if (accessKey != null && cuentaSaasRepository.existsByAccessKey(accessKey.trim())) {
            throw new BusinessException(ErrorCode.ACCOUNT_ACCESS_KEY_ALREADY_EXISTS);
        }
    }
}
