package com.s1nt4xSystem.facturacion_sunat.platform.plan.repository;

import com.s1nt4xSystem.facturacion_sunat.platform.plan.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {
    Optional<Plan> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
