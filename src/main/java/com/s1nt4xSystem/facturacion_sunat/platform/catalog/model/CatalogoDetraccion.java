package com.s1nt4xSystem.facturacion_sunat.platform.catalog.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "catalogo_detracciones", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoDetraccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_bien_servicio", nullable = false, unique = true, length = 10)
    private String codigoBienServicio;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    @Column(name = "porcentaje", nullable = false, precision = 7, scale = 4)
    private BigDecimal porcentaje;

    @Column(name = "monto_minimo", precision = 14, scale = 2)
    private BigDecimal montoMinimo;

    @Column(name = "vigencia_desde")
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Builder.Default
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;
}
