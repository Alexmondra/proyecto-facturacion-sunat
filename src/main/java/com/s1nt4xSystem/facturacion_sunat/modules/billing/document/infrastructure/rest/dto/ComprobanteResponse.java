package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComprobanteResponse {
    private UUID id;
    private EmisorDto emisor;
    private ComprobanteDataResponse comprobante;
    private ClienteDto cliente;
    private List<ItemComprobanteResponse> items;
    private List<CuotaDto> cuotas;
    private DetraccionDto detraccion;
    private TotalesResponse totales;
    private String estado;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
