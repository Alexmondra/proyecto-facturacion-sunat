package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "documento_referencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoReferenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoEntity documento;

    @Column(name = "tipo_relacion", length = 10)
    private String tipoRelacion;

    @Column(name = "documento_referenciado_id")
    private UUID documentoReferenciadoId;

    @Column(name = "tipo_documento_ref", length = 5)
    private String tipoDocumentoRef;

    @Column(name = "serie_ref", length = 10)
    private String serieRef;

    @Column(name = "numero_ref")
    private Integer numeroRef;

    @Column(name = "motivo_codigo", length = 10)
    private String motivoCodigo;

    @Column(name = "motivo_descripcion", columnDefinition = "TEXT")
    private String motivoDescripcion;

    @Column(name = "fecha_emision_ref")
    private LocalDate fechaEmisionRef;

    @Column(name = "moneda_ref", length = 5)
    private String monedaRef;
}
