package com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoDetraccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatalogoDetraccionRepository extends JpaRepository<CatalogoDetraccion, Long> {
    Optional<CatalogoDetraccion> findByCodigoBienServicioAndEstadoTrue(String codigoBienServicio);
}
