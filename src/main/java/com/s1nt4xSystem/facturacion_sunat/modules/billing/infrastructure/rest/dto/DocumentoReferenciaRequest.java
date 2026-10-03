package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoReferenciaRequest {
    @JsonProperty("tipo_relacion")
    @Builder.Default
    private String tipoRelacion = "afecta";

    @JsonProperty("documento_referenciado_id")
    private UUID documentoReferenciadoId;

    @JsonProperty("tipo_documento_ref")
    private String tipoDocumentoRef;

    @JsonProperty("serie_ref")
    private String serieRef;

    @JsonProperty("numero_ref")
    private Integer numeroRef;

    @JsonProperty("fecha_emision_ref")
    private LocalDate fechaEmisionRef;

    @JsonProperty("moneda_ref")
    private String monedaRef;
}
