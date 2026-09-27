package com.s1nt4xSystem.facturacion_sunat.platform.account.model;

import com.s1nt4xSystem.facturacion_sunat.platform.plan.model.Plan;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cuentas_saas", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaSaas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_key", nullable = false, length = 100)
    private String accessKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Builder.Default
    @Column(name = "consumido_mes")
    private Integer consumidoMes = 0;

    @Column(name = "fecha_corte")
    private Integer fechaCorte;

    @Builder.Default
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo = "CLIENTE";

    @Builder.Default
    @Column(name = "estado")
    private Boolean estado = true;
}
