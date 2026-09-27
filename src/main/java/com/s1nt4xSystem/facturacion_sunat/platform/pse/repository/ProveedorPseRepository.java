package com.s1nt4xSystem.facturacion_sunat.platform.pse.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.pse.model.ProveedorPse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProveedorPseRepository extends JpaRepository<ProveedorPse, Long> {
    Optional<ProveedorPse> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
