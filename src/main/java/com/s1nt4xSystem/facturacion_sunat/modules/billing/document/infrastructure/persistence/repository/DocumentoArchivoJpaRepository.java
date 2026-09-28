package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.repository;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.entity.DocumentoArchivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoArchivoJpaRepository extends JpaRepository<DocumentoArchivoEntity, UUID> {
    List<DocumentoArchivoEntity> findByDocumentoId(UUID documentoId);
    List<DocumentoArchivoEntity> findByDocumentoIdAndTipoArchivo(UUID documentoId, String tipoArchivo);
}
