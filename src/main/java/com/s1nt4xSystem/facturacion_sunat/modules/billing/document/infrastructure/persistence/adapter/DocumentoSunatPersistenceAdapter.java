package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.DocumentoArchivoInfo;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.DocumentoSunatResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.DocumentoSunatPersistencePort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.entity.DocumentoArchivoEntity;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.entity.DocumentoSunatEntity;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.repository.DocumentoArchivoJpaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.repository.DocumentoSunatJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentoSunatPersistenceAdapter implements DocumentoSunatPersistencePort {

    private final DocumentoSunatJpaRepository sunatJpaRepository;
    private final DocumentoArchivoJpaRepository archivoJpaRepository;

    @Override
    @Transactional
    public void guardarRespuestaSunat(UUID documentoId, DocumentoSunatResponse resp) {
        if (documentoId == null || resp == null) return;

        DocumentoSunatEntity entity = sunatJpaRepository.findByDocumentoId(documentoId)
                .orElse(DocumentoSunatEntity.builder().documentoId(documentoId).build());

        entity.setTicket(resp.getTicket());
        entity.setEstadoSunat(resp.getEstadoSunat());
        entity.setCodigoRespuestaSunat(resp.getCodigoRespuestaSunat());
        entity.setMensajeSunat(resp.getMensajeSunat());
        entity.setFechaEnvio(resp.getFechaEnvio());
        entity.setFechaRespuesta(resp.getFechaRespuesta());

        sunatJpaRepository.saveAndFlush(entity);
        log.info("Respuesta SUNAT guardada para documentoId {}: Estado={}, Código={}",
                documentoId, resp.getEstadoSunat(), resp.getCodigoRespuestaSunat());
    }

    @Override
    @Transactional
    public DocumentoArchivoInfo registrarArchivo(UUID documentoId, String tipoArchivo, String rutaArchivo, String nombreArchivo) {
        DocumentoArchivoEntity entity = DocumentoArchivoEntity.builder()
                .documentoId(documentoId)
                .tipoArchivo(tipoArchivo)
                .proveedorAlmacenamiento("LOCAL")
                .rutaArchivo(rutaArchivo)
                .nombreArchivo(nombreArchivo)
                .build();

        DocumentoArchivoEntity saved = archivoJpaRepository.saveAndFlush(entity);
        log.info("Archivo registrado en documento_archivos: documentoId={}, tipo={}, ruta={}",
                documentoId, tipoArchivo, rutaArchivo);

        return DocumentoArchivoInfo.builder()
                .id(saved.getId())
                .documentoId(saved.getDocumentoId())
                .tipoArchivo(saved.getTipoArchivo())
                .proveedorAlmacenamiento(saved.getProveedorAlmacenamiento())
                .bucket(saved.getBucket())
                .rutaArchivo(saved.getRutaArchivo())
                .nombreArchivo(saved.getNombreArchivo())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentoSunatResponse> obtenerRespuestaSunat(UUID documentoId) {
        return sunatJpaRepository.findByDocumentoId(documentoId)
                .map(e -> DocumentoSunatResponse.builder()
                        .ticket(e.getTicket())
                        .estadoSunat(e.getEstadoSunat())
                        .codigoRespuestaSunat(e.getCodigoRespuestaSunat())
                        .mensajeSunat(e.getMensajeSunat())
                        .fechaEnvio(e.getFechaEnvio())
                        .fechaRespuesta(e.getFechaRespuesta())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoArchivoInfo> listarArchivosPorDocumento(UUID documentoId) {
        return archivoJpaRepository.findByDocumentoId(documentoId).stream()
                .map(e -> DocumentoArchivoInfo.builder()
                        .id(e.getId())
                        .documentoId(e.getDocumentoId())
                        .tipoArchivo(e.getTipoArchivo())
                        .proveedorAlmacenamiento(e.getProveedorAlmacenamiento())
                        .bucket(e.getBucket())
                        .rutaArchivo(e.getRutaArchivo())
                        .nombreArchivo(e.getNombreArchivo())
                        .build())
                .toList();
    }
}
