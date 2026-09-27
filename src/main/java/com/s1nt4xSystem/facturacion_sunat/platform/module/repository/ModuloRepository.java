package com.s1nt4xSystem.facturacion_sunat.platform.module.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.module.model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, Long> {
    Optional<Modulo> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
