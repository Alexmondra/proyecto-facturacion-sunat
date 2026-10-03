package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "documento_archivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoArchivoEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(name = "tipo_archivo", nullable = false, length = 30)
    private String tipoArchivo; // XML, CDR, PDF

    @Builder.Default
    @Column(name = "proveedor_almacenamiento", length = 50)
    private String proveedorAlmacenamiento = "LOCAL";

    @Column(name = "bucket", length = 100)
    private String bucket;

    @Column(name = "ruta_archivo", nullable = false, columnDefinition = "text")
    private String rutaArchivo;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;
}
