package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetraccionCommand {
    @NotBlank(message = "El código de bien/servicio sujeto a detracción es obligatorio (Catálogo 54)")
    private String codigoBienServicio;

    @NotNull(message = "El porcentaje de detracción es obligatorio")
    @DecimalMin(value = "0.01", message = "El porcentaje de detracción debe ser mayor a cero")
    private BigDecimal porcentaje;

    private BigDecimal montoDetraccion; // Si no viene, se calcula automáticamente (total * porcentaje / 100)

    @Builder.Default
    private String moneda = "PEN";

    private String medioPago;
    private String numeroCuentaDetraccion;
}
