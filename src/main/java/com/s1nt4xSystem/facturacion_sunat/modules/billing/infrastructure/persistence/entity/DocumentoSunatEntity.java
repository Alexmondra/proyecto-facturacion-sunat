package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documento_sunat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoSunatEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "documento_id", nullable = false, unique = true)
    private UUID documentoId;

    @Column(name = "ticket", length = 100)
    private String ticket;

    @Column(name = "estado_sunat", length = 50)
    private String estadoSunat;

    @Column(name = "codigo_respuesta_sunat", length = 20)
    private String codigoRespuestaSunat;

    @Column(name = "mensaje_sunat", columnDefinition = "text")
    private String mensajeSunat;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}
