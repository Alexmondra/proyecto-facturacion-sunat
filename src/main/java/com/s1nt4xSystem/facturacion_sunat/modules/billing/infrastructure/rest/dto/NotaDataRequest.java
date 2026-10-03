package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotaDataRequest {

    @JsonProperty("clave_idempotencia")
    private String claveIdempotencia;

    @NotBlank(message = "El tipo es obligatorio (07=Nota de Crédito, 08=Nota de Débito)")
    @JsonProperty("tipo")
    @JsonAlias({"tipo_documento", "tipo"})
    private String tipo;

    private String serie;
    private Integer numero;

    @JsonProperty("fecha_emision")
    private String fechaEmision;

    @JsonProperty("hora_emision")
    private String horaEmision;

    private String moneda;

    @JsonProperty("motivo_codigo")
    @NotBlank(message = "El código de motivo de nota SUNAT es obligatorio (ej. 01, 07)")
    private String motivoCodigo;

    @JsonProperty("motivo_descripcion")
    @NotBlank(message = "La descripción del motivo es obligatoria")
    private String motivoDescripcion;

    @JsonProperty("documento_referenciado_id")
    private UUID documentoReferenciadoId;

    @JsonProperty("documento_referencia")
    @Valid
    private DocumentoReferenciaRequest documentoReferencia;

    @Valid
    private ClienteDto cliente;

    @Valid
    @Builder.Default
    private List<ItemComprobanteRequest> items = new ArrayList<>();

    public LocalDateTime getFechaEmision() {
        if (fechaEmision == null || fechaEmision.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            String f = fechaEmision.trim();
            if (f.length() == 10) {
                LocalDate date = LocalDate.parse(f);
                if (horaEmision != null && !horaEmision.isBlank()) {
                    try {
                        LocalTime time = LocalTime.parse(horaEmision.trim());
                        return LocalDateTime.of(date, time);
                    } catch (Exception ignored) {}
                }
                return date.atTime(LocalTime.now());
            }
            if (f.contains("T")) {
                return LocalDateTime.parse(f);
            }
            if (f.contains(" ")) {
                return LocalDateTime.parse(f, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            return LocalDateTime.parse(f);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
