package com.s1nt4xSystem.facturacion_sunat.modules.core.branch.dto;

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
public class SucursalRequest {

    @NotBlank(message = "El código de la sucursal es obligatorio (ej. 0000 para casa matriz)")
    @Size(max = 10, message = "El código no puede exceder 10 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombreSucursal;

    private String ubigeo;
    private String direccion;
    private String telefono;
    private String email;
    private String imagenSucursal;

    @Builder.Default
    private BigDecimal impuestoPorcentaje = new BigDecimal("18.00");

    private String configuracionExtra;

    @Builder.Default
    private Boolean activo = true;
}
