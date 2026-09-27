package com.s1nt4xSystem.facturacion_sunat.platform.module.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.module.model.EmpresaModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaModuloRepository extends JpaRepository<EmpresaModulo, Long> {
    List<EmpresaModulo> findByEmpresaRouterId(Long empresaRouterId);
    Optional<EmpresaModulo> findByEmpresaRouterIdAndModuloId(Long empresaRouterId, Long moduloId);
}
