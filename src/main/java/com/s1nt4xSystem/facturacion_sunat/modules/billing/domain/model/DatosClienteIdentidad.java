package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import lombok.Builder;

@Builder
public record DatosClienteIdentidad(
        String tipoDocumento,
        String numeroDocumento,
        String denominacion,
        String direccion,
        String estado,
        String condicion
) {
    public boolean isActivo() {
        return estado == null || "ACTIVO".equalsIgnoreCase(estado.trim());
    }

    public boolean isHabido() {
        return condicion == null || "HABIDO".equalsIgnoreCase(condicion.trim());
    }
}
