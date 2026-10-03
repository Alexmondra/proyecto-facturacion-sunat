package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.repository;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity.DocumentoSunatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentoSunatJpaRepository extends JpaRepository<DocumentoSunatEntity, UUID> {
    Optional<DocumentoSunatEntity> findByDocumentoId(UUID documentoId);
}
