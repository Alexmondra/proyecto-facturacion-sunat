package com.s1nt4xSystem.facturacion_sunat.modules.billing.series.repository;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SerieRepository extends JpaRepository<Serie, UUID> {

    List<Serie> findBySucursalId(UUID sucursalId);

    Optional<Serie> findBySucursalIdAndTipoComprobanteAndSerie(
            UUID sucursalId, String tipoComprobante, String serie);

    Optional<Serie> findByTipoComprobanteAndSerie(String tipoComprobante, String serie);

    Optional<Serie> findFirstBySucursalIdAndTipoComprobante(
            UUID sucursalId, String tipoComprobante);

    boolean existsBySucursalIdAndTipoComprobanteAndSerie(
            UUID sucursalId, String tipoComprobante, String serie);

    boolean existsByTipoComprobanteAndSerie(String tipoComprobante, String serie);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Serie s WHERE s.sucursal.id = :sucursalId AND s.tipoComprobante = :tipoComprobante AND s.serie = :serie")
    Optional<Serie> findForUpdate(
            @Param("sucursalId") UUID sucursalId,
            @Param("tipoComprobante") String tipoComprobante,
            @Param("serie") String serie);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Serie s WHERE s.tipoComprobante = :tipoComprobante AND s.serie = :serie")
    Optional<Serie> findForUpdateByTipoComprobanteAndSerie(
            @Param("tipoComprobante") String tipoComprobante,
            @Param("serie") String serie);
}
