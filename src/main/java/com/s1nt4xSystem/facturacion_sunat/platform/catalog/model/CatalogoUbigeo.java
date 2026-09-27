package com.s1nt4xSystem.facturacion_sunat.platform.catalog.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalogo_ubigeos", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoUbigeo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(name = "departamento", nullable = false, length = 120)
    private String departamento;

    @Column(name = "provincia", nullable = false, length = 120)
    private String provincia;

    @Column(name = "distrito", nullable = false, length = 120)
    private String distrito;

    @Builder.Default
    @Column(name = "es_amazonia", nullable = false)
    private Boolean esAmazonia = false;

    @Builder.Default
    @Column(name = "estado", nullable = false)
    private Boolean estado = true;
}
