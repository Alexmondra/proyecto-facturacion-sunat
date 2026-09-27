package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "documento_totales_afectacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoTotalesAfectacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoEntity documento;

    @Column(name = "tipo_afectacion_codigo", nullable = false, length = 10)
    private String tipoAfectacionCodigo;

    @Column(name = "tipo_total", length = 50)
    private String tipoTotal;

    @Column(name = "base_imponible", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseImponible;

    @Column(name = "monto_tributo", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoTributo;

    @Column(name = "monto_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoTotal;
}
