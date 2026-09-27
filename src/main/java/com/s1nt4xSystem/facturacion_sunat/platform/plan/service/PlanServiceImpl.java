package com.s1nt4xSystem.facturacion_sunat.platform.plan.service;

import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.model.Plan;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.repository.PlanRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    public PlanServiceImpl(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public PlanResponse createPlan(PlanRequest request) {
        if (request.getCodigo() != null && planRepository.existsByCodigo(request.getCodigo())) {
            throw new DomainException("Ya existe un plan con el código: " + request.getCodigo());
        }
        Plan plan = Plan.builder()
                .nombrePlan(request.getNombrePlan())
                .codigo(request.getCodigo())
                .limiteMensualBolsa(request.getLimiteMensualBolsa())
                .precioMensual(request.getPrecioMensual())
                .estado(request.getEstado() != null ? request.getEstado() : true)
                .build();
        Plan saved = planRepository.save(plan);
        return PlanResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getPlanById(Integer id) {
        return PlanResponse.fromEntity(getPlanEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Plan getPlanEntity(Integer id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        return planRepository.findAll().stream()
                .map(PlanResponse::fromEntity)
                .toList();
    }

    @Override
    public PlanResponse updatePlan(Integer id, PlanRequest request) {
        Plan plan = getPlanEntity(id);
        if (request.getCodigo() != null && !request.getCodigo().equalsIgnoreCase(plan.getCodigo())
                && planRepository.existsByCodigo(request.getCodigo())) {
            throw new DomainException("El código " + request.getCodigo() + " ya está en uso por otro plan");
        }
        plan.setNombrePlan(request.getNombrePlan());
        plan.setCodigo(request.getCodigo());
        plan.setLimiteMensualBolsa(request.getLimiteMensualBolsa());
        plan.setPrecioMensual(request.getPrecioMensual());
        if (request.getEstado() != null) {
            plan.setEstado(request.getEstado());
        }
        return PlanResponse.fromEntity(planRepository.save(plan));
    }

    @Override
    public PlanResponse updateStatus(Integer id, Boolean estado) {
        Plan plan = getPlanEntity(id);
        plan.setEstado(estado);
        return PlanResponse.fromEntity(planRepository.save(plan));
    }

    @Override
    public void deletePlan(Integer id) {
        Plan plan = getPlanEntity(id);
        plan.setEstado(false); // Soft delete
        planRepository.save(plan);
    }
}
