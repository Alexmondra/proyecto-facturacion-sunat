package com.s1nt4xSystem.facturacion_sunat.platform.catalog.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_tributos", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoTributo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "tipo_calculo", length = 30)
    private String tipoCalculo;

    @Builder.Default
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;
}
