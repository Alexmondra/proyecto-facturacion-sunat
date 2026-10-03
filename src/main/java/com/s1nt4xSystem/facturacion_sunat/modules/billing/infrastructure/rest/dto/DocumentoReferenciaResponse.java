package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoReferenciaResponse {
    private UUID id;

    @JsonProperty("tipo_relacion")
    private String tipoRelacion;

    @JsonProperty("documento_referenciado_id")
    private UUID documentoReferenciadoId;

    @JsonProperty("tipo_documento_ref")
    private String tipoDocumentoRef;

    @JsonProperty("serie_ref")
    private String serieRef;

    @JsonProperty("numero_ref")
    private Integer numeroRef;

    @JsonProperty("motivo_codigo")
    private String motivoCodigo;

    @JsonProperty("motivo_descripcion")
    private String motivoDescripcion;

    @JsonProperty("fecha_emision_ref")
    private LocalDate fechaEmisionRef;

    @JsonProperty("moneda_ref")
    private String monedaRef;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
