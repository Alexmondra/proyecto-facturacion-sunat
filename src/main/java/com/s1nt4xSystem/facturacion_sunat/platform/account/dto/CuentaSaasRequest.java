package com.s1nt4xSystem.facturacion_sunat.platform.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaSaasRequest {

    @NotBlank(message = "El nombre de la cuenta SaaS es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @NotNull(message = "El ID del plan es obligatorio")
    private Integer planId;

    private String accessKey;

    private Integer fechaCorte;

    @Builder.Default
    private String tipo = "CLIENTE";

    @Builder.Default
    private Boolean estado = true;
}
