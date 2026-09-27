package com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto;

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
public class EmpresaTenantUpdateRequest {

    @Size(max = 255, message = "La razón social no puede exceder 255 caracteres")
    private String razonSocial;

    private String direccionFiscal;

    private String logo;

    private Boolean envioAsincrono;

    private Boolean incluidoTributo;

    private String entorno;

    private String modoEmision;

    private Long idPse;

    private String webhookUrl;

    private String userSol;

    private String passSol;

    private String sunatClientId;

    private String sunatClientSecret;

    private String numeroCuentaDetraccion;
}
