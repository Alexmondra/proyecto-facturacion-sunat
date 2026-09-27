package com.s1nt4xSystem.facturacion_sunat.platform.plan.service;

import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.model.Plan;

import java.util.List;

public interface PlanService {
    PlanResponse createPlan(PlanRequest request);
    PlanResponse getPlanById(Integer id);
    Plan getPlanEntity(Integer id);
    List<PlanResponse> getAllPlans();
    PlanResponse updatePlan(Integer id, PlanRequest request);
    PlanResponse updateStatus(Integer id, Boolean estado);
    void deletePlan(Integer id);
}
