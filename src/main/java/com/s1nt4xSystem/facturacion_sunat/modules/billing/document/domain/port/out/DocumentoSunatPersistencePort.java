package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.DocumentoArchivoInfo;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.DocumentoSunatResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoSunatPersistencePort {
    /**
     * Guarda o actualiza el registro de auditoría y respuesta de SUNAT para un documento.
     */
    void guardarRespuestaSunat(UUID documentoId, DocumentoSunatResponse sunatResponse);

    /**
     * Registra un archivo generado (XML, CDR, etc.) asociado al documento.
     */
    DocumentoArchivoInfo registrarArchivo(UUID documentoId, String tipoArchivo, String rutaArchivo, String nombreArchivo);

    /**
     * Obtiene la respuesta de SUNAT asociada al documento, si existe.
     */
    Optional<DocumentoSunatResponse> obtenerRespuestaSunat(UUID documentoId);

    /**
     * Lista todos los archivos asociados al documento.
     */
    List<DocumentoArchivoInfo> listarArchivosPorDocumento(UUID documentoId);
}
