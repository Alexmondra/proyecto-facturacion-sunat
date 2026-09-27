package com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto;

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
public class SucursalUpdateRequest {

    @Size(max = 10, message = "El código no puede exceder 10 caracteres")
    private String codigo;

    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombreSucursal;

    private String ubigeo;
    private String direccion;
    private String telefono;
    private String email;
    private String imagenSucursal;
    private BigDecimal impuestoPorcentaje;
    private String configuracionExtra;
    private Boolean activo;
}
