package com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sucursales", uniqueConstraints = {
        @UniqueConstraint(name = "uq_sucursal_empresa_codigo", columnNames = {"empresa_id", "codigo"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sucursal {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "codigo", nullable = false, length = 10)
    private String codigo;

    @Column(name = "ubigeo", length = 10)
    private String ubigeo;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "nombre_sucursal", nullable = false, length = 150)
    private String nombreSucursal;

    @Column(name = "imagen_sucursal", columnDefinition = "text")
    private String imagenSucursal;

    @Builder.Default
    @Column(name = "impuesto_porcentaje", precision = 5, scale = 2)
    private BigDecimal impuestoPorcentaje = new BigDecimal("18.00");

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "configuracion_extra", columnDefinition = "jsonb")
    private String configuracionExtra;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
