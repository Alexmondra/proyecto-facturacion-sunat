package com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoTipoAfectacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatalogoTipoAfectacionRepository extends JpaRepository<CatalogoTipoAfectacion, Long> {
    Optional<CatalogoTipoAfectacion> findByCodigoAndEstadoTrue(String codigo);
}
