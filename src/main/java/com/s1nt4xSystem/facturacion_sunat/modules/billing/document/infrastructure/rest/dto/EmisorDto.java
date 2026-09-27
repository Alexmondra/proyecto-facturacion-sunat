package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmisorDto {
    private String ruc;

    @JsonProperty("codigo_sucursal")
    @Builder.Default
    private String codigoSucursal = "0000";
}
