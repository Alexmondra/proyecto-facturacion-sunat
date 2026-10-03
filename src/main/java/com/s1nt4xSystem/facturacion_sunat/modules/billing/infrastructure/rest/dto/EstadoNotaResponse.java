package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoNotaResponse {
    private UUID id;
    private String estado;

    @JsonProperty("estado_sunat")
    private String estadoSunat;

    private String mensaje;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
