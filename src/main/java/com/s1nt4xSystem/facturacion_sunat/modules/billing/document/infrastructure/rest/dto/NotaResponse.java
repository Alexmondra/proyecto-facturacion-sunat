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
public class NotaResponse {
    private UUID id;

    @JsonProperty("clave_idempotencia")
    private String claveIdempotencia;

    @JsonProperty("tipo_comprobante")
    private String tipoComprobante;

    private String serie;
    private Integer numero;

    @JsonProperty("fecha_emision")
    private LocalDateTime fechaEmision;

    private String moneda;

    @JsonProperty("estado_interno")
    private String estadoInterno;

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

    @JsonProperty("documento_referencia")
    private DocumentoReferenciaResponse documentoReferencia;

    private ClienteDto cliente;
    private List<ItemComprobanteResponse> items;
    private TotalesNotaResponse totales;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
