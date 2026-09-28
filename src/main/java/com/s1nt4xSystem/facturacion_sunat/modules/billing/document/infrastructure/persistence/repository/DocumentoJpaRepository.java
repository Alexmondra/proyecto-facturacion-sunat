package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.repository;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.entity.DocumentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentoJpaRepository extends JpaRepository<DocumentoEntity, UUID> {

    Optional<DocumentoEntity> findByClaveIdempotencia(String claveIdempotencia);

    @Query("SELECT d FROM DocumentoEntity d WHERE d.empresaId = :empresaId AND d.tipoComprobante = :tipo AND d.serie = :serie AND d.numero = :numero")
    Optional<DocumentoEntity> findByEmision(
            @Param("empresaId") UUID empresaId,
            @Param("tipo") String tipoComprobante,
            @Param("serie") String serie,
            @Param("numero") Integer numero
    );

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE DocumentoEntity d SET d.hashCpe = :hashCpe, d.estadoInterno = :estadoInterno WHERE d.id = :id")
    void actualizarHashYEstado(
            @Param("id") UUID id,
            @Param("hashCpe") String hashCpe,
            @Param("estadoInterno") String estadoInterno
    );
}
