package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import lombok.Builder;

@Builder
public record ClienteIdentidad(
        String tipoDocumento,
        String numeroDocumento,
        String denominacion,
        String direccion,
        boolean verificadoExternamente
) {
    public static ClienteIdentidad clientesVarios() {
        return ClienteIdentidad.builder()
                .tipoDocumento("0")
                .numeroDocumento("00000000")
                .denominacion("CLIENTES VARIOS")
                .direccion("-")
                .verificadoExternamente(false)
                .build();
    }
}
