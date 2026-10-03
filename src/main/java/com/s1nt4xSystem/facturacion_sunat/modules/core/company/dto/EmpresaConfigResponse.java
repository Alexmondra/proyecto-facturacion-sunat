package com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaConfigResponse {
    private UUID id;
    private UUID empresaId;
    private Boolean envioAsincrono;
    private String modoEmision;
    private Long idPse;
    private String webhookUrl;
    private String tipoCertificado;
    private boolean hasCertificado;
    private String userSol;
    private String sunatClientId;
    private String numeroCuentaDetraccion;

    public static EmpresaConfigResponse fromEntity(EmpresaConfig config) {
        return EmpresaConfigResponse.builder()
                .id(config.getId())
                .empresaId(config.getEmpresa() != null ? config.getEmpresa().getId() : null)
                .envioAsincrono(config.getEnvioAsincrono())
                .modoEmision(config.getModoEmision())
                .idPse(config.getIdPse())
                .webhookUrl(config.getWebhookUrl())
                .tipoCertificado(config.getTipoCertificado())
                .hasCertificado(config.getCertificado() != null && !config.getCertificado().isEmpty())
                .userSol(config.getUserSol())
                .sunatClientId(config.getSunatClientId())
                .numeroCuentaDetraccion(config.getNumeroCuentaDetraccion())
                .build();
    }
}
