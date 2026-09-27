package com.s1nt4xSystem.facturacion_sunat.platform.plan.dto;

import com.s1nt4xSystem.facturacion_sunat.platform.plan.model.Plan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {
    private Integer id;
    private String nombrePlan;
    private String codigo;
    private Integer limiteMensualBolsa;
    private BigDecimal precioMensual;
    private Boolean estado;

    public static PlanResponse fromEntity(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .nombrePlan(plan.getNombrePlan())
                .codigo(plan.getCodigo())
                .limiteMensualBolsa(plan.getLimiteMensualBolsa())
                .precioMensual(plan.getPrecioMensual())
                .estado(plan.getEstado())
                .build();
    }
}
