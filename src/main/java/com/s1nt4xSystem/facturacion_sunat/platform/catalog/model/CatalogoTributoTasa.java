package com.s1nt4xSystem.facturacion_sunat.platform.catalog.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "catalogo_tributo_tasas", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoTributoTasa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tributo_id", nullable = false)
    private CatalogoTributo tributo;

    @Column(name = "valor", nullable = false, precision = 12, scale = 6)
    private BigDecimal valor;

    @Column(name = "vigencia_desde")
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Builder.Default
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;
}
