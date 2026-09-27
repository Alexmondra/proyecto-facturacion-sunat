package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model;

import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "empresas_router", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaRouter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saas_id", nullable = false)
    private CuentaSaas cuentaSaas;

    @Column(name = "ruc", nullable = false, length = 20)
    private String ruc;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "db_schema", nullable = false, length = 100)
    private String dbSchema;

    @Column(name = "access_key", length = 100, unique = true)
    private String accessKey;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVO";
}
