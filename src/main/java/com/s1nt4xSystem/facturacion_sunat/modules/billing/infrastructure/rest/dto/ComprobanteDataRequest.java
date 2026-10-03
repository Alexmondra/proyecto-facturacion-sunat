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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComprobanteDataRequest {

    @JsonProperty("clave_idempotencia")
    private String claveIdempotencia;

    @NotBlank(message = "El tipo de comprobante es obligatorio (01=Factura, 03=Boleta)")
    @JsonProperty("tipo")
    @JsonAlias({"tipo_documento", "tipo", "tipo_comprobante"})
    private String tipo;

    private String serie;
    private Integer numero;

    @JsonProperty("fecha_emision")
    private String fechaEmision;

    @JsonProperty("hora_emision")
    private String horaEmision;

    @Builder.Default
    private String moneda = "PEN";

    @JsonProperty("forma_pago")
    @Builder.Default
    private String formaPago = "CONTADO";

    @JsonProperty("tipo_operacion")
    @Builder.Default
    private String tipoOperacion = "0101";

    @Valid
    private ClienteDto cliente;

    @Valid
    @Builder.Default
    private List<ItemComprobanteRequest> items = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<CuotaDto> cuotas = new ArrayList<>();

    @Valid
    private DetraccionDto detraccion;

    @JsonProperty("descuento_global")
    private BigDecimal descuentoGlobal;

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
