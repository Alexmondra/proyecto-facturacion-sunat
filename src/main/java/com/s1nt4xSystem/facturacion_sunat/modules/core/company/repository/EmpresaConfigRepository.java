package com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaConfigRepository extends JpaRepository<EmpresaConfig, UUID> {
    Optional<EmpresaConfig> findByEmpresaId(UUID empresaId);
}
