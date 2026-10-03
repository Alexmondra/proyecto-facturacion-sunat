package com.s1nt4xSystem.facturacion_sunat.modules.core.series.model;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "series", uniqueConstraints = {
        @UniqueConstraint(name = "uq_serie_tipo_comprobante", columnNames = {"tipo_comprobante", "serie"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Serie {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "tipo_comprobante", nullable = false, length = 5)
    private String tipoComprobante;

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;

    @Builder.Default
    @Column(name = "correlativo", nullable = false)
    private Integer correlativo = 0;
}
