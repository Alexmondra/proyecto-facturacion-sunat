package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuotaCommand {
    private Integer numeroCuota;

    @NotNull(message = "La fecha de vencimiento de la cuota es obligatoria")
    private LocalDate fechaVencimiento;

    @NotNull(message = "El monto de la cuota es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto de la cuota debe ser mayor a cero")
    private BigDecimal monto;

    @Builder.Default
    private String moneda = "PEN";
}
