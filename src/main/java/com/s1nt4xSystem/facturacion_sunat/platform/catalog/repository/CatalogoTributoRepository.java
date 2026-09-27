package com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoTributo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatalogoTributoRepository extends JpaRepository<CatalogoTributo, Long> {
    Optional<CatalogoTributo> findByCodigoAndEstadoTrue(String codigo);
}
