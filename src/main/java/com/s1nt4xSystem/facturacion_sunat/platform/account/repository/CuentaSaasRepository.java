package com.s1nt4xSystem.facturacion_sunat.platform.account.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaSaasRepository extends JpaRepository<CuentaSaas, Long> {
    Optional<CuentaSaas> findByAccessKey(String accessKey);
    boolean existsByAccessKey(String accessKey);
}
