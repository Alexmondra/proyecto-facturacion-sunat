package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documentos", uniqueConstraints = {
        @UniqueConstraint(name = "uq_documentos_empresa_emision", columnNames = {"empresa_id", "tipo_comprobante", "serie", "numero"}),
        @UniqueConstraint(name = "uq_documentos_clave_idempotencia", columnNames = {"clave_idempotencia"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clave_idempotencia", length = 100, unique = true)
    private String claveIdempotencia;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(name = "serie_id")
    private UUID serieId;

    @Column(name = "tipo_comprobante", nullable = false, length = 5)
    private String tipoComprobante;

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "moneda", nullable = false, length = 5)
    private String moneda;

    @Column(name = "tipo_operacion", length = 10)
    private String tipoOperacion;

    @Column(name = "cliente_tipo_doc", nullable = false, length = 5)
    private String clienteTipoDoc;

    @Column(name = "cliente_numero_doc", nullable = false, length = 20)
    private String clienteNumeroDoc;

    @Column(name = "cliente_nombre", nullable = false, length = 255)
    private String clienteNombre;

    @Column(name = "forma_pago", length = 30)
    private String formaPago;

    @Column(name = "total_otros_cargos", precision = 14, scale = 2)
    private BigDecimal totalOtrosCargos;

    @Column(name = "total_tributos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTributos;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "estado_interno", nullable = false, length = 30)
    private String estadoInterno;

    @Column(name = "hash_cpe", length = 150)
    private String hashCpe;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payload_entrada", columnDefinition = "jsonb")
    private String payloadEntrada;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentoDetalleEntity> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentoTributoEntity> tributos = new ArrayList<>();

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentoTotalesAfectacionEntity> totalesAfectacion = new ArrayList<>();

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentoCuotaEntity> cuotas = new ArrayList<>();

    @OneToOne(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    private DocumentoDetraccionEntity detraccion;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentoReferenciaEntity> referencias = new ArrayList<>();
}
