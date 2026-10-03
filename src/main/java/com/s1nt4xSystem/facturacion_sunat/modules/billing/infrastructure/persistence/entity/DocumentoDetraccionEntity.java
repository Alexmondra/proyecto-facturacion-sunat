package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "documento_detracciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoDetraccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false, unique = true)
    private DocumentoEntity documento;

    @Column(name = "codigo_bien_servicio", nullable = false, length = 10)
    private String codigoBienServicio;

    @Column(name = "porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(name = "monto_detraccion", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoDetraccion;

    @Column(name = "moneda", nullable = false, length = 5)
    private String moneda;

    @Column(name = "medio_pago", length = 10)
    private String medioPago;

    @Column(name = "numero_cuenta_detraccion", length = 50)
    private String numeroCuentaDetraccion;
}
