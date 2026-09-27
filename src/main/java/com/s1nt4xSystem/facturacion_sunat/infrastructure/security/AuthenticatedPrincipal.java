package com.s1nt4xSystem.facturacion_sunat.infrastructure.security;

import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthenticatedPrincipal {

    private final Long saasId;
    private final String tipo; // "ADMIN", "CLIENTE", "EMPRESA"
    private final EmpresaRouter empresaRouter;
    private final String accessKey;

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(tipo);
    }

    public boolean isCliente() {
        return "CLIENTE".equalsIgnoreCase(tipo);
    }

    public boolean isEmpresa() {
        return "EMPRESA".equalsIgnoreCase(tipo);
    }
}
