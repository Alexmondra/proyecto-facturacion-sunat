package com.s1nt4xSystem.facturacion_sunat.platform.pse.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedores_pse", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorPse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "url_base", length = 255)
    private String urlBase;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(name = "usuario", length = 120)
    private String usuario;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "token", columnDefinition = "text")
    private String token;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
