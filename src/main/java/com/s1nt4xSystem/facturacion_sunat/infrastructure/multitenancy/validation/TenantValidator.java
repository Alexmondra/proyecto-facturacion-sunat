package com.s1nt4xSystem.facturacion_sunat.infrastructure.multitenancy.validation;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validador centralizado de acceso y enrutamiento a tenants.
 * Encapsula la autenticación de llamadas a /api/v1/tenant/**, cabecera X-Tenant-ID,
 * correspondencia con API Keys de Empresa o SaaS, y estado del tenant.
 */
@Component
public class TenantValidator {

    private static final Pattern SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final EmpresaRouterRepository empresaRouterRepository;

    public TenantValidator(EmpresaRouterRepository empresaRouterRepository) {
        this.empresaRouterRepository = empresaRouterRepository;
    }

    /**
     * Valida que exista un principal autenticado en el contexto de seguridad.
     */
    public void validateAuthenticated(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.TENANT_AUTH_REQUIRED);
        }
    }

    /**
     * Valida el acceso cuando la petición usa una API Key exclusiva de Empresa.
     * Si se envía X-Tenant-ID opcionalmente, este debe coincidir con su RUC o esquema asignado.
     */
    public String resolveSchemaForEmpresa(EmpresaRouter router, String tenantHeader) {
        if (router == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND, "");
        }
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            String cleanHeader = tenantHeader.trim();
            if (!cleanHeader.equalsIgnoreCase(router.getRuc()) && !cleanHeader.equalsIgnoreCase(router.getDbSchema())) {
                throw new BusinessException(ErrorCode.TENANT_ACCESS_DENIED, router.getRuc());
            }
        }
        return router.getDbSchema();
    }

    /**
     * Valida y resuelve el tenant cuando la petición proviene de un Cliente SaaS o Administrador.
     * La cabecera X-Tenant-ID es obligatoria y debe corresponder a un RUC registrado, activo
     * y perteneciente a su cuenta SaaS (para rol CLIENTE).
     */
    public EmpresaRouter resolveAndValidateForSaasOrAdmin(String tenantHeader, AuthenticatedPrincipal principal) {
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_HEADER_REQUIRED);
        }

        String identifier = tenantHeader.trim();
        String ruc = identifier.startsWith("tenant_") ? identifier.substring(7) : identifier;

        EmpresaRouter router = empresaRouterRepository.findByRuc(ruc)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, ruc));

        if (principal != null && principal.isCliente()
                && (router.getCuentaSaas() == null || !router.getCuentaSaas().getId().equals(principal.getSaasId()))) {
            throw new BusinessException(ErrorCode.TENANT_SAAS_ACCESS_DENIED);
        }

        if (!"ACTIVO".equalsIgnoreCase(router.getEstado())) {
            throw new BusinessException(ErrorCode.TENANT_INACTIVE);
        }

        return router;
    }

    /**
     * Valida el nombre del esquema PostgreSQL para prevenir inyección SQL al ejecutar SET search_path.
     */
    public void validateSchemaName(String schemaName) {
        if (schemaName == null || !SCHEMA_PATTERN.matcher(schemaName.trim()).matches()) {
            throw new BusinessException(ErrorCode.TENANT_INVALID_SCHEMA, schemaName);
        }
    }
}
