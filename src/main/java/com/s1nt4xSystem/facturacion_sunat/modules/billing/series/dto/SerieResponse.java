package com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerieResponse {
    private UUID id;
    private UUID sucursalId;
    private String sucursalCodigo;
    private String sucursalNombre;
    private String tipoComprobante;
    private String serie;
    private Integer correlativo;

    public static SerieResponse fromEntity(Serie s) {
        return SerieResponse.builder()
                .id(s.getId())
                .sucursalId(s.getSucursal() != null ? s.getSucursal().getId() : null)
                .sucursalCodigo(s.getSucursal() != null ? s.getSucursal().getCodigo() : null)
                .sucursalNombre(s.getSucursal() != null ? s.getSucursal().getNombreSucursal() : null)
                .tipoComprobante(s.getTipoComprobante())
                .serie(s.getSerie())
                .correlativo(s.getCorrelativo())
                .build();
    }
}
