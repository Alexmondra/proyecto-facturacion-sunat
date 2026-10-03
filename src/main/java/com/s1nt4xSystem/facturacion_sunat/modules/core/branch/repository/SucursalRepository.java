package com.s1nt4xSystem.facturacion_sunat.modules.core.branch.repository;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, UUID> {
    Optional<Sucursal> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Sucursal> findByActivoTrue();
    List<Sucursal> findByEmpresaId(UUID empresaId);
    Optional<Sucursal> findByEmpresaIdAndCodigo(UUID empresaId, String codigo);
    boolean existsByEmpresaIdAndCodigo(UUID empresaId, String codigo);
}
