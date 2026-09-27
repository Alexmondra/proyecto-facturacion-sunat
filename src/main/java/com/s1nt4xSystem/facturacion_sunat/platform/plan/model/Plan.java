package com.s1nt4xSystem.facturacion_sunat.platform.plan.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "planes", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_plan", nullable = false, length = 50)
    private String nombrePlan;

    @Column(name = "codigo", length = 10, unique = true)
    private String codigo;

    @Column(name = "limite_mensual_bolsa")
    private Integer limiteMensualBolsa;

    @Column(name = "precio_mensual", precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Builder.Default
    @Column(name = "estado")
    private Boolean estado = true;
}
