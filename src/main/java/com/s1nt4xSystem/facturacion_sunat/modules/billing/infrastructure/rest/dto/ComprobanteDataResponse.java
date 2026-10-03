package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComprobanteDataResponse {
    @JsonProperty("clave_idempotencia")
    private String claveIdempotencia;
    private String tipo;
    private String serie;
    private Integer numero;
    @JsonProperty("fecha_emision")
    private LocalDateTime fechaEmision;
    private String moneda;
    @JsonProperty("forma_pago")
    private String formaPago;
}
