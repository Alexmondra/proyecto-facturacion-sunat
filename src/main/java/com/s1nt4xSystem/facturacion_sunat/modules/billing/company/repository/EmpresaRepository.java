package com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {
    Optional<Empresa> findByRuc(String ruc);
    boolean existsByRuc(String ruc);
}
