package com.s1nt4xSystem.facturacion_sunat.infrastructure.security;

import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import com.s1nt4xSystem.facturacion_sunat.platform.account.repository.CuentaSaasRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final CuentaSaasRepository cuentaSaasRepository;
    private final EmpresaRouterRepository empresaRouterRepository;

    public ApiKeyAuthenticationFilter(
            CuentaSaasRepository cuentaSaasRepository,
            EmpresaRouterRepository empresaRouterRepository) {
        this.cuentaSaasRepository = cuentaSaasRepository;
        this.empresaRouterRepository = empresaRouterRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Solo protegemos las rutas bajo /api/v1/**
        if (!path.startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Únicamente el método GET de la API de planes es público (para landing/precios)
        if (path.startsWith("/api/v1/platform/plans") && HttpMethod.GET.matches(request.getMethod())) {
            String token = extractToken(request);
            if (token != null && !token.isBlank()) {
                AuthenticatedPrincipal principal = resolvePrincipal(token);
                if (principal != null) {
                    SecurityContext.setPrincipal(principal);
                }
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContext.clear();
            }
            return;
        }

        // 1. Extraer token (soporta tanto 'Authorization: Bearer <key>' como 'X-API-Key: <key>')
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED,
                    "Se requiere autenticación. Envíe su Access Key mediante cabecera 'Authorization: Bearer <key>' o 'X-API-Key: <key>'");
            return;
        }

        // 2. Validar Token en Cuentas SaaS o en Empresas Router
        AuthenticatedPrincipal principal = resolvePrincipal(token);
        if (principal == null) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Access Key inválida o inexistente");
            return;
        }

        // 3. Validar permisos de nivel plataforma
        if (path.startsWith("/api/v1/platform")) {
            if (principal.isEmpresa()) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN,
                        "Las llaves de empresa no tienen permisos para acceder a la administración de plataforma");
                return;
            }

            // Solo el ADMIN puede crear, modificar o eliminar planes
            if (path.startsWith("/api/v1/platform/plans") && !principal.isAdmin()) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN,
                        "Solo el Administrador del sistema puede crear, modificar o eliminar planes comerciales");
                return;
            }

            // Restricciones de cuentas para rol CLIENTE
            if (path.startsWith("/api/v1/platform/accounts")) {
                if (principal.isCliente()) {
                    if (HttpMethod.POST.matches(request.getMethod())) {
                        writeErrorResponse(response, HttpStatus.FORBIDDEN,
                                "Solo el Administrador del sistema puede dar de alta nuevas cuentas SaaS");
                        return;
                    }
                    if (path.equals("/api/v1/platform/accounts") || path.equals("/api/v1/platform/accounts/")) {
                        writeErrorResponse(response, HttpStatus.FORBIDDEN,
                                "Solo el Administrador del sistema puede listar todas las cuentas SaaS");
                        return;
                    }
                }
            }
        }

        try {
            SecurityContext.setPrincipal(principal);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }

        String apiKeyHeader = request.getHeader("X-API-Key");
        if (apiKeyHeader != null && !apiKeyHeader.trim().isEmpty()) {
            return apiKeyHeader.trim();
        }

        return null;
    }

    private AuthenticatedPrincipal resolvePrincipal(String token) {
        // A. Buscar en cuentas_saas
        Optional<CuentaSaas> cuentaOpt = cuentaSaasRepository.findByAccessKey(token);
        if (cuentaOpt.isPresent()) {
            CuentaSaas cuenta = cuentaOpt.get();
            if (Boolean.FALSE.equals(cuenta.getEstado())) {
                return null; // Cuenta inactiva
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
                return null; // Empresa inactiva
            }
            if (Boolean.FALSE.equals(router.getCuentaSaas().getEstado())) {
                return null; // Cuenta matriz inactiva
            }
            return AuthenticatedPrincipal.builder()
                    .saasId(router.getCuentaSaas().getId())
                    .tipo("EMPRESA")
                    .empresaRouter(router)
                    .accessKey(token)
                    .build();
        }

        return null;
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
