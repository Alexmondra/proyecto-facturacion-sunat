package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.validation;

import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaOnboardingRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaRouterResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validador centralizado para el registro de Tenants y enrutamiento de Empresas (Platform Tenant Registry).
 */
@Component
public class EmpresaRouterValidator {

    private static final Pattern RUC_PATTERN = Pattern.compile("^(10|20)[0-9]{9}$");

    private final EmpresaRouterRepository empresaRouterRepository;

    public EmpresaRouterValidator(EmpresaRouterRepository empresaRouterRepository) {
        this.empresaRouterRepository = empresaRouterRepository;
    }

    /**
     * Valida los datos requeridos y la no duplicidad para el onboarding de una nueva empresa tenant.
     */
    public void validateOnboarding(EmpresaOnboardingRequest request, String schemaName, String accessKey) {
        if (request.getRuc() == null || !RUC_PATTERN.matcher(request.getRuc().trim()).matches()) {
            throw new BusinessException(ErrorCode.ONBOARDING_INVALID_RUC, request.getRuc());
        }
        if (request.getRazonSocial() == null || request.getRazonSocial().isBlank()) {
            throw new BusinessException(ErrorCode.ONBOARDING_RAZON_SOCIAL_REQUIRED);
        }
        if (empresaRouterRepository.existsByRuc(request.getRuc().trim())) {
            throw new BusinessException(ErrorCode.ONBOARDING_RUC_ALREADY_EXISTS, request.getRuc().trim());
        }
        if (empresaRouterRepository.existsByDbSchema(schemaName)) {
            throw new BusinessException(ErrorCode.ONBOARDING_SCHEMA_ALREADY_EXISTS, schemaName);
        }
        if (accessKey != null && !accessKey.isBlank() && empresaRouterRepository.existsByAccessKey(accessKey.trim())) {
            throw new BusinessException(ErrorCode.ONBOARDING_ACCESS_KEY_ALREADY_EXISTS);
        }
    }

    /**
     * Valida los estados permitidos para una empresa tenant en la plataforma.
     */
    public void validateEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return;
        }
        String clean = estado.trim().toUpperCase();
        if (!clean.equals("ACTIVO") && !clean.equals("INACTIVO") && !clean.equals("SUSPENDIDO")) {
            throw new BusinessException(ErrorCode.ONBOARDING_INVALID_STATE);
        }
    }

    /**
     * Valida la pertenencia de una empresa a la cuenta SaaS del cliente autenticado.
     */
    public void validateAccountOwnership(EmpresaRouterResponse response, Long saasId) {
        if (response != null && saasId != null && !response.getSaasId().equals(saasId)) {
            throw new BusinessException(ErrorCode.ONBOARDING_ACCOUNT_ACCESS_DENIED);
        }
    }

    /**
     * Valida que un cliente SaaS no intente consultar datos de otra cuenta SaaS.
     */
    public void validateSameAccountAccess(Long requestedSaasId, Long principalSaasId) {
        if (requestedSaasId != null && principalSaasId != null && !requestedSaasId.equals(principalSaasId)) {
            throw new BusinessException(ErrorCode.ONBOARDING_CROSS_ACCOUNT_DENIED);
        }
    }
}
