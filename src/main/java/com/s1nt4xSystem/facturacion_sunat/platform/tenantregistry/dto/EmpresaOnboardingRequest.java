package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaOnboardingRequest {

    @NotNull(message = "El ID de la cuenta SaaS es obligatorio")
    private Long saasId;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "^(10|20)[0-9]{9}$", message = "El RUC debe tener 11 dígitos y comenzar con 10 o 20")
    private String ruc;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 150, message = "La razón social no puede exceder 150 caracteres")
    private String razonSocial;

    private String direccionFiscal;

    private String logo;

    private String accessKey;
}
