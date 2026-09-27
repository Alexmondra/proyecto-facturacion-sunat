package com.s1nt4xSystem.facturacion_sunat.infrastructure.security;

public final class SecurityContext {

    private static final ThreadLocal<AuthenticatedPrincipal> CURRENT_PRINCIPAL = new ThreadLocal<>();

    private SecurityContext() {
    }

    public static void setPrincipal(AuthenticatedPrincipal principal) {
        CURRENT_PRINCIPAL.set(principal);
    }

    public static AuthenticatedPrincipal getPrincipal() {
        return CURRENT_PRINCIPAL.get();
    }

    public static void clear() {
        CURRENT_PRINCIPAL.remove();
    }
}
