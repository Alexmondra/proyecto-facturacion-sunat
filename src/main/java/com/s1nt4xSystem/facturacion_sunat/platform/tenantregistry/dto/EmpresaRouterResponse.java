package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto;

import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaRouterResponse {
    private Long id;
    private Long saasId;
    private String saasNombre;
    private String ruc;
    private String nombre;
    private String dbSchema;
    private String accessKey;
    private String estado;

    public static EmpresaRouterResponse fromEntity(EmpresaRouter router) {
        return EmpresaRouterResponse.builder()
                .id(router.getId())
                .saasId(router.getCuentaSaas() != null ? router.getCuentaSaas().getId() : null)
                .saasNombre(router.getCuentaSaas() != null ? router.getCuentaSaas().getNombre() : null)
                .ruc(router.getRuc())
                .nombre(router.getNombre())
                .dbSchema(router.getDbSchema())
                .accessKey(router.getAccessKey())
                .estado(router.getEstado())
                .build();
    }
}
