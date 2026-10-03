package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "documento_tributos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoTributoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoEntity documento;

    @Column(name = "tributo_id", nullable = false)
    private Long tributoId;

    @Column(name = "base_imponible", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseImponible;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;
}
