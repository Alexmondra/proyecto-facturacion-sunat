package com.s1nt4xSystem.facturacion_sunat.modules.core.company.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "ruc", nullable = false, length = 20, unique = true)
    private String ruc;

    @Column(name = "logo", columnDefinition = "text")
    private String logo;

    @Builder.Default
    @Column(name = "incluido_tributo", nullable = false)
    private Boolean incluidoTributo = true;

    @Column(name = "razon_social", nullable = false, length = 255)
    private String razonSocial;

    @Column(name = "direccion_fiscal", length = 255)
    private String direccionFiscal;

    @Builder.Default
    @Column(name = "entorno", nullable = false, length = 30)
    private String entorno = "dev";

    @Builder.Default
    @Column(name = "limite_asignado", nullable = false)
    private Integer limiteAsignado = 0;

    @Builder.Default
    @Column(name = "consumido_mes", nullable = false)
    private Integer consumidoMes = 0;
}
