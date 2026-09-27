package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documento_detalles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoDetalleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoEntity documento;

    @Column(name = "item", nullable = false)
    private Integer item;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "cantidad", nullable = false, precision = 14, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "unidad_medida", nullable = false, length = 10)
    private String unidadMedida;

    @Column(name = "valor_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal valorUnitario;

    @Column(name = "tipo_afectacion", nullable = false, length = 10)
    private String tipoAfectacion;

    @Column(name = "total_tributos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTributos;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentoDetalleTributoEntity> tributos = new ArrayList<>();
}
