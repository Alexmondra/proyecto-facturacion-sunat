package com.s1nt4xSystem.facturacion_sunat.infrastructure.security.validation;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal;
import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import com.s1nt4xSystem.facturacion_sunat.platform.account.repository.CuentaSaasRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityValidator {

    private final CuentaSaasRepository cuentaSaasRepository;
    private final EmpresaRouterRepository empresaRouterRepository;

    public SecurityValidator(
            CuentaSaasRepository cuentaSaasRepository,
            EmpresaRouterRepository empresaRouterRepository) {
        this.cuentaSaasRepository = cuentaSaasRepository;
        this.empresaRouterRepository = empresaRouterRepository;
    }

    /**
     * Valida que el token de autenticación esté presente y no esté vacío.
     */
    public void validateTokenPresent(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.SECURITY_TOKEN_REQUIRED);
        }
    }

    /**
     * Valida y resuelve el token como un AuthenticatedPrincipal.
     * Busca primero en CuentaSaas y luego en EmpresaRouter.
     */
    public AuthenticatedPrincipal validateAndResolvePrincipal(String token) {
        validateTokenPresent(token);

        // A. Buscar en cuentas_saas
        Optional<CuentaSaas> cuentaOpt = cuentaSaasRepository.findByAccessKey(token);
        if (cuentaOpt.isPresent()) {
            CuentaSaas cuenta = cuentaOpt.get();
            if (Boolean.FALSE.equals(cuenta.getEstado())) {
                throw new BusinessException(ErrorCode.SECURITY_ACCOUNT_INACTIVE);
            }
            return AuthenticatedPrincipal.builder()
                    .saasId(cuenta.getId())
                    .tipo(cuenta.getTipo() != null ? cuenta.getTipo() : "CLIENTE")
                    .empresaRouter(null)
                    .accessKey(token)
                    .build();
        }

        // B. Buscar en empresas_router
        Optional<EmpresaRouter> empresaOpt = empresaRouterRepository.findByAccessKey(token);
        if (empresaOpt.isPresent()) {
            EmpresaRouter router = empresaOpt.get();
            if (!"ACTIVO".equalsIgnoreCase(router.getEstado())) {
                throw new BusinessException(ErrorCode.SECURITY_EMPRESA_INACTIVE);
            }
            if (router.getCuentaSaas() == null || Boolean.FALSE.equals(router.getCuentaSaas().getEstado())) {
                throw new BusinessException(ErrorCode.SECURITY_ACCOUNT_INACTIVE);
            }
            return AuthenticatedPrincipal.builder()
                    .saasId(router.getCuentaSaas().getId())
                    .tipo("EMPRESA")
                    .empresaRouter(router)
                    .accessKey(token)
                    .build();
        }

        throw new BusinessException(ErrorCode.SECURITY_INVALID_TOKEN);
    }

    /**
     * Resuelve opcionalmente el token (por ejemplo para endpoints públicos que pueden autenticar si se envía token).
     */
    public AuthenticatedPrincipal resolvePrincipalQuietly(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return validateAndResolvePrincipal(token);
        } catch (BusinessException ex) {
            return null;
        }
    }

    /**
     * Valida permisos y autorizaciones para rutas bajo /api/v1/platform
     */
    public void validatePlatformAccess(AuthenticatedPrincipal principal, String path, String method) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.SECURITY_TOKEN_REQUIRED);
        }

        if (principal.isEmpresa()) {
            throw new BusinessException(ErrorCode.SECURITY_PLATFORM_ACCESS_DENIED);
        }

        // Solo el ADMIN puede crear, modificar o eliminar planes
        if (path.startsWith("/api/v1/platform/plans") && !principal.isAdmin()) {
            throw new BusinessException(ErrorCode.SECURITY_PLAN_ADMIN_REQUIRED);
        }

        // Restricciones de cuentas para rol CLIENTE
        if (path.startsWith("/api/v1/platform/accounts")) {
            if (principal.isCliente()) {
                if (HttpMethod.POST.matches(method)) {
                    throw new BusinessException(ErrorCode.SECURITY_ACCOUNT_CREATE_ADMIN_REQUIRED);
                }
                if (path.equals("/api/v1/platform/accounts") || path.equals("/api/v1/platform/accounts/")) {
                    throw new BusinessException(ErrorCode.SECURITY_ACCOUNT_LIST_ADMIN_REQUIRED);
                }
            }
        }
    }
}
