package com.s1nt4xSystem.facturacion_sunat.infrastructure.security;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.security.validation.SecurityValidator;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityValidator securityValidator;

    public ApiKeyAuthenticationFilter(SecurityValidator securityValidator) {
        this.securityValidator = securityValidator;
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

        // Rutas públicas de consulta / descarga sin autenticación (planes y descargas públicas)
        if ((path.startsWith("/api/v1/platform/plans") || path.startsWith("/api/v1/public/")) && HttpMethod.GET.matches(request.getMethod())) {
            String token = extractToken(request);
            AuthenticatedPrincipal principal = securityValidator.resolvePrincipalQuietly(token);
            if (principal != null) {
                SecurityContext.setPrincipal(principal);
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContext.clear();
            }
            return;
        }

        try {
            // 1. Extraer y validar token presente
            String token = extractToken(request);

            // 2. Validar token y resolver AuthenticatedPrincipal
            AuthenticatedPrincipal principal = securityValidator.validateAndResolvePrincipal(token);

            // 3. Validar permisos de nivel plataforma
            if (path.startsWith("/api/v1/platform")) {
                securityValidator.validatePlatformAccess(principal, path, request.getMethod());
            }

            SecurityContext.setPrincipal(principal);
            filterChain.doFilter(request, response);

        } catch (BusinessException be) {
            writeErrorResponse(response, be);
        } catch (Exception e) {
            writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
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
