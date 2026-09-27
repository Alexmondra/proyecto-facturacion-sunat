package com.s1nt4xSystem.facturacion_sunat.infrastructure.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantContext {
    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final String DEFAULT_TENANT = "public";

    public static void setCurrentTenant(String tenant) {
        log.debug("Setting tenant context to {}", tenant);
        currentTenant.set(tenant);
    }

    public static String getCurrentTenant() {
        String tenant = currentTenant.get();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }

    public static void clear() {
        log.debug("Clearing tenant context");
        currentTenant.remove();
    }
}
