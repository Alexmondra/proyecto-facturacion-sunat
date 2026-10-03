package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoArchivoInfo {
    private UUID id;
    private UUID documentoId;
    private String tipoArchivo; // XML, CDR, PDF
    private String proveedorAlmacenamiento; // LOCAL, S3, etc.
    private String bucket;
    private String rutaArchivo;
    private String nombreArchivo;
}
