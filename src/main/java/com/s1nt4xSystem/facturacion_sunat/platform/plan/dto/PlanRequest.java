package com.s1nt4xSystem.facturacion_sunat.platform.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {

    @NotBlank(message = "El nombre del plan es obligatorio")
    @Size(max = 50, message = "El nombre del plan no puede exceder 50 caracteres")
    private String nombrePlan;

    @NotBlank(message = "El código del plan es obligatorio")
    @Size(max = 10, message = "El código no puede exceder 10 caracteres")
    private String codigo;

    private Integer limiteMensualBolsa;

    private BigDecimal precioMensual;

    @Builder.Default
    private Boolean estado = true;
}
