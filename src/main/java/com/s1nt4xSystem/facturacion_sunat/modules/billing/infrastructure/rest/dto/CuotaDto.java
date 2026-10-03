package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CuotaDto {
    @JsonProperty("numero")
    @JsonAlias({"numero_cuota", "numero", "cuota"})
    private Integer numero;

    @JsonProperty("fecha_vencimiento")
    @NotNull(message = "La fecha de vencimiento de la cuota es obligatoria")
    private LocalDate fechaVencimiento;

    @NotNull(message = "El monto de la cuota es obligatorio")
    private BigDecimal monto;
}
