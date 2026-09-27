package com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoUbigeo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SucursalResponse {
    private UUID id;
    private String codigo;
    private String nombreSucursal;
    private String ubigeo;
    private String departamento;
    private String provincia;
    private String distrito;
    private Boolean esAmazonia;
    private String direccion;
    private String telefono;
    private String email;
    private String imagenSucursal;
    private BigDecimal impuestoPorcentaje;
    private String configuracionExtra;
    private Boolean activo;

    public static SucursalResponse fromEntity(Sucursal s) {
        return fromEntity(s, null);
    }

    public static SucursalResponse fromEntity(Sucursal s, CatalogoUbigeo u) {
        return SucursalResponse.builder()
                .id(s.getId())
                .codigo(s.getCodigo())
                .nombreSucursal(s.getNombreSucursal())
                .ubigeo(s.getUbigeo())
                .departamento(u != null ? u.getDepartamento() : null)
                .provincia(u != null ? u.getProvincia() : null)
                .distrito(u != null ? u.getDistrito() : null)
                .esAmazonia(u != null ? u.getEsAmazonia() : null)
                .direccion(s.getDireccion())
                .telefono(s.getTelefono())
                .email(s.getEmail())
                .imagenSucursal(s.getImagenSucursal())
                .impuestoPorcentaje(s.getImpuestoPorcentaje())
                .configuracionExtra(s.getConfiguracionExtra())
                .activo(s.getActivo())
                .build();
    }
}
