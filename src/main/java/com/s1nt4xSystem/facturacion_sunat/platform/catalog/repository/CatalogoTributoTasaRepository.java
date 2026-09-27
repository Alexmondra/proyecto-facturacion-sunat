package com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoTributoTasa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CatalogoTributoTasaRepository extends JpaRepository<CatalogoTributoTasa, Long> {

    @Query("""
        SELECT t FROM CatalogoTributoTasa t
        WHERE t.tributo.codigo = :codigoTributo
          AND t.estado = true
          AND (t.vigenciaDesde IS NULL OR t.vigenciaDesde <= :fecha)
          AND (t.vigenciaHasta IS NULL OR t.vigenciaHasta >= :fecha)
        ORDER BY t.vigenciaDesde DESC NULLS LAST
    """)
    Optional<CatalogoTributoTasa> findTasaVigente(@Param("codigoTributo") String codigoTributo, @Param("fecha") LocalDate fecha);
}
