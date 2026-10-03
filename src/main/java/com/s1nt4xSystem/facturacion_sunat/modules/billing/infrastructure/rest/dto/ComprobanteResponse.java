package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

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
    private String tipo;
    private String serie;
    private Integer numero;
    private java.math.BigDecimal total;
    private java.util.Map<String, String> enlaces;

    private EmisorDto emisor;
    private ComprobanteDataResponse comprobante;
    private ClienteDto cliente;
    private List<ItemComprobanteResponse> items;
    private List<CuotaDto> cuotas;
    private DetraccionDto detraccion;
    private TotalesResponse totales;
    private String estado;

    @JsonProperty("estado_sunat")
    private String estadoSunat;

    @JsonProperty("codigo_sunat")
    private String codigoSunat;

    @JsonProperty("mensaje_sunat")
    private String mensajeSunat;

    @JsonProperty("xml_url")
    private String xmlUrl;

    @JsonProperty("cdr_url")
    private String cdrUrl;

    @JsonProperty("hash_cpe")
    private String hashCpe;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
