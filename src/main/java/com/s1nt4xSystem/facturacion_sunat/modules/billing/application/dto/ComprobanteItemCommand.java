package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.dto;

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
public class ComprobanteItemCommand {
    private String codigoProducto;

    @NotBlank(message = "La descripción del ítem es obligatoria")
    private String descripcion;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
    private BigDecimal cantidad;

    @Builder.Default
    private String unidadMedida = "NIU";

    private BigDecimal valorUnitario; // Precio sin IGV
    private BigDecimal precioUnitario; // Precio con IGV incluido
    private BigDecimal precio; // Campo genérico opcional (se interpretará según empresa.incluido_tributo)

    @NotBlank(message = "El código de afectación al IGV es obligatorio (ej. 10, 20, 30)")
    private String tipoAfectacion;

    private BigDecimal descuento;
    private Integer cantidadBolsasIcbper;
}
