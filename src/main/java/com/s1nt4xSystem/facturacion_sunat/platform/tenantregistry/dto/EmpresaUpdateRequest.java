package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaUpdateRequest {

    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @Size(max = 255, message = "La razón social no puede exceder 255 caracteres")
    private String razonSocial;

    private String direccionFiscal;

    private String logo;

    private String estado;
}
