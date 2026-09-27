package com.s1nt4xSystem.facturacion_sunat.infrastructure.multitenancy;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal;
import com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
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
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final Pattern SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final EmpresaRouterRepository empresaRouterRepository;

    public TenantFilter(EmpresaRouterRepository empresaRouterRepository) {
        this.empresaRouterRepository = empresaRouterRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Si es una ruta de tenant (/api/v1/tenant/**), aplicamos validación estricta de pertenencia
        if (path.startsWith("/api/v1/tenant")) {
            AuthenticatedPrincipal principal = SecurityContext.getPrincipal();
            if (principal == null) {
                writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Se requiere autenticación para acceder a este tenant");
                return;
            }

            String targetSchema;

            if (principal.isEmpresa()) {
                // Caso A: Llave exclusiva de una Empresa
                EmpresaRouter router = principal.getEmpresaRouter();
                String tenantHeader = request.getHeader(TENANT_HEADER);

                if (tenantHeader != null && !tenantHeader.isBlank()) {
                    String cleanHeader = tenantHeader.trim();
                    if (!cleanHeader.equalsIgnoreCase(router.getRuc()) && !cleanHeader.equalsIgnoreCase(router.getDbSchema())) {
                        writeErrorResponse(response, HttpStatus.FORBIDDEN,
                                String.format("Acceso denegado: Su Access Key solo autoriza operar sobre el RUC %s", router.getRuc()));
                        return;
                    }
                }
                targetSchema = router.getDbSchema();

            } else {
                // Caso B: Llave Maestra de Cuenta SaaS (CLIENTE) o SuperAdmin (ADMIN)
                String tenantHeader = request.getHeader(TENANT_HEADER);
                if (tenantHeader == null || tenantHeader.isBlank()) {
                    writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                            "La cabecera 'X-Tenant-ID' (con el RUC de la empresa) es obligatoria para operar sobre este recurso");
                    return;
                }

                String identifier = tenantHeader.trim();
                String ruc = identifier.startsWith("tenant_") ? identifier.substring(7) : identifier;

                Optional<EmpresaRouter> routerOpt = empresaRouterRepository.findByRuc(ruc);
                if (routerOpt.isEmpty()) {
                    writeErrorResponse(response, HttpStatus.NOT_FOUND, "Empresa con RUC " + ruc + " no encontrada en el sistema");
                    return;
                }

                EmpresaRouter router = routerOpt.get();

                // Si es un CLIENTE, validar que la empresa pertenezca a su propia cuenta SaaS
                if (principal.isCliente() && !router.getCuentaSaas().getId().equals(principal.getSaasId())) {
                    writeErrorResponse(response, HttpStatus.FORBIDDEN,
                            "Acceso denegado: Esta empresa no pertenece a su cuenta SaaS");
                    return;
                }

                if (!"ACTIVO".equalsIgnoreCase(router.getEstado())) {
                    writeErrorResponse(response, HttpStatus.FORBIDDEN, "La empresa se encuentra inactiva o suspendida");
                    return;
                }

                targetSchema = router.getDbSchema();
            }

            TenantContext.setCurrentTenant(targetSchema);

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

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = String.format("{\"success\":false,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                message.replace("\"", "\\\""), Instant.now());
        response.getWriter().write(json);
    }
}
