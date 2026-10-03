package com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaConfigRequest {

    @NotBlank(message = "El modo de emisión es obligatorio (PROPIO, PSE o DIRECTO_SUNAT)")
    @Pattern(regexp = "(?i)^(PROPIO|PSE|DIRECTO_SUNAT|SUNAT)$", message = "El modo de emisión debe ser PROPIO, PSE o DIRECTO_SUNAT")
    private String modoEmision;

    private Boolean envioAsincrono;

    private Long idPse;

    private String webhookUrl;

    private String tipoCertificado;

    private String certificado;

    private String certificadoPass;

    private String userSol;

    private String passSol;

    private String sunatClientId;

    private String sunatClientSecret;

    private String numeroCuentaDetraccion;
}
