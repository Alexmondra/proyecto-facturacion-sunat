package com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "empresa_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaConfig {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    @Builder.Default
    @Column(name = "envio_asincrono", nullable = false)
    private Boolean envioAsincrono = true;

    @Builder.Default
    @Column(name = "modo_emision", nullable = false, length = 10)
    private String modoEmision = "PROPIO";

    @Column(name = "id_pse")
    private Long idPse;

    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;

    @Column(name = "tipo_certificado", length = 50)
    private String tipoCertificado;

    @Column(name = "certificado", columnDefinition = "text")
    private String certificado;

    @Column(name = "certificado_pass", length = 255)
    private String certificadoPass;

    @Column(name = "user_sol", length = 50)
    private String userSol;

    @Column(name = "pass_sol", length = 255)
    private String passSol;

    @Column(name = "sunat_client_id", length = 150)
    private String sunatClientId;

    @Column(name = "sunat_client_secret", length = 255)
    private String sunatClientSecret;

    @Column(name = "numero_cuenta_detraccion", length = 50)
    private String numeroCuentaDetraccion;
}
