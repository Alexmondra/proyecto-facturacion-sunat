package com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.EmpresaConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResponse {
    private UUID id;
    private String ruc;
    private Boolean envioAsincrono;
    private String logo;
    private Boolean incluidoTributo;
    private String razonSocial;
    private String direccionFiscal;
    private String entorno;
    private Integer limiteAsignado;
    private Integer consumidoMes;
    private EmpresaConfigResponse configuracion;
    private CertificadoDigitalResponse certificado;

    public static EmpresaResponse fromEntity(Empresa empresa) {
        return fromEntity(empresa, null, null);
    }

    public static EmpresaResponse fromEntity(Empresa empresa, EmpresaConfig config, CertificadoDigitalResponse certificado) {
        EmpresaResponseBuilder builder = EmpresaResponse.builder()
                .id(empresa.getId())
                .ruc(empresa.getRuc())
                .logo(empresa.getLogo())
                .incluidoTributo(empresa.getIncluidoTributo())
                .razonSocial(empresa.getRazonSocial())
                .direccionFiscal(empresa.getDireccionFiscal())
                .entorno(empresa.getEntorno())
                .limiteAsignado(empresa.getLimiteAsignado())
                .consumidoMes(empresa.getConsumidoMes())
                .certificado(certificado);

        if (config != null) {
            builder.configuracion(EmpresaConfigResponse.fromEntity(config))
                   .envioAsincrono(config.getEnvioAsincrono());
        }

        return builder.build();
    }
}
