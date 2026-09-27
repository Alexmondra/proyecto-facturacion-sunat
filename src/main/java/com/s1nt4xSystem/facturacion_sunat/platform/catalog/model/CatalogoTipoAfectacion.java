package com.s1nt4xSystem.facturacion_sunat.platform.catalog.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_tipo_afectacion", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoTipoAfectacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    @Column(name = "tipo", length = 30)
    private String tipo;

    @Column(name = "tipotributo_codigo", length = 10)
    private String tipotributoCodigo;

    @Column(name = "afecta_igv")
    private Boolean afectaIgv;

    @Builder.Default
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;
}
