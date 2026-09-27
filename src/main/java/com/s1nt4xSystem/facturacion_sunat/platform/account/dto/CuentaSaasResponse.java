package com.s1nt4xSystem.facturacion_sunat.platform.account.dto;

import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaSaasResponse {
    private Long id;
    private String accessKey;
    private Integer planId;
    private String planNombre;
    private String nombre;
    private Integer consumidoMes;
    private Integer fechaCorte;
    private String tipo;
    private Boolean estado;

    public static CuentaSaasResponse fromEntity(CuentaSaas cuenta) {
        return CuentaSaasResponse.builder()
                .id(cuenta.getId())
                .accessKey(cuenta.getAccessKey())
                .planId(cuenta.getPlan() != null ? cuenta.getPlan().getId() : null)
                .planNombre(cuenta.getPlan() != null ? cuenta.getPlan().getNombrePlan() : null)
                .nombre(cuenta.getNombre())
                .consumidoMes(cuenta.getConsumidoMes())
                .fechaCorte(cuenta.getFechaCorte())
                .tipo(cuenta.getTipo())
                .estado(cuenta.getEstado())
                .build();
    }
}
