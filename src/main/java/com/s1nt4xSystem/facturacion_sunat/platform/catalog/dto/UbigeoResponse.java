package com.s1nt4xSystem.facturacion_sunat.platform.catalog.dto;

import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoUbigeo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UbigeoResponse {
    private String codigo;
    private String departamento;
    private String provincia;
    private String distrito;
    private Boolean esAmazonia;

    public static UbigeoResponse fromEntity(CatalogoUbigeo u) {
        if (u == null) return null;
        return UbigeoResponse.builder()
                .codigo(u.getCodigo())
                .departamento(u.getDepartamento())
                .provincia(u.getProvincia())
                .distrito(u.getDistrito())
                .esAmazonia(u.getEsAmazonia())
                .build();
    }
}
