package com.s1nt4xSystem.facturacion_sunat.infrastructure.multitenancy;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.multitenancy.validation.TenantValidator;
import com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal;
import com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantValidator tenantValidator;

    public TenantFilter(TenantValidator tenantValidator) {
        this.tenantValidator = tenantValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Si es una ruta de tenant (/api/v1/tenant/**), aplicamos validación estricta de pertenencia
        if (path.startsWith("/api/v1/tenant")) {
            try {
                AuthenticatedPrincipal principal = SecurityContext.getPrincipal();
                tenantValidator.validateAuthenticated(principal);

                String targetSchema;
                String tenantHeader = request.getHeader(TENANT_HEADER);

                if (principal.isEmpresa()) {
                    targetSchema = tenantValidator.resolveSchemaForEmpresa(principal.getEmpresaRouter(), tenantHeader);
                } else {
                    EmpresaRouter router = tenantValidator.resolveAndValidateForSaasOrAdmin(tenantHeader, principal);
                    targetSchema = router.getDbSchema();
                }

                tenantValidator.validateSchemaName(targetSchema);
                TenantContext.setCurrentTenant(targetSchema);

            } catch (BusinessException be) {
                writeErrorResponse(response, be);
                return;
            } catch (Exception e) {
                writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                return;
            }
        } else {
            // Rutas de plataforma u otros recursos: esquema 'public'
            TenantContext.setCurrentTenant("public");
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void writeErrorResponse(HttpServletResponse response, BusinessException be) throws IOException {
        response.setStatus(be.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = String.format("{\"success\":false,\"codigo_error\":%d,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                be.getCode(), be.getMessage().replace("\"", "\\\""), Instant.now());
        response.getWriter().write(json);
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = String.format("{\"success\":false,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                message != null ? message.replace("\"", "\\\"") : "", Instant.now());
        response.getWriter().write(json);
    }
}
