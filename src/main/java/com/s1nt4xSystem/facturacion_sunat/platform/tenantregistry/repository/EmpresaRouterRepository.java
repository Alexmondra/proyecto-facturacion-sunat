package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRouterRepository extends JpaRepository<EmpresaRouter, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"cuentaSaas"})
    Optional<EmpresaRouter> findByRuc(String ruc);

    boolean existsByRuc(String ruc);
    boolean existsByDbSchema(String dbSchema);
    List<EmpresaRouter> findByCuentaSaasId(Long saasId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"cuentaSaas"})
    Optional<EmpresaRouter> findByAccessKey(String accessKey);

    boolean existsByAccessKey(String accessKey);
}
