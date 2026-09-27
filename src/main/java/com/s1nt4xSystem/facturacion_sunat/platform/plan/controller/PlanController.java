package com.s1nt4xSystem.facturacion_sunat.platform.plan.controller;

import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.service.PlanService;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(@Valid @RequestBody PlanRequest request) {
        PlanResponse created = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Plan creado exitosamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.ok(planService.getAllPlans()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlanById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(planService.getPlanById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlan(
            @PathVariable Integer id,
            @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(planService.updatePlan(id, request), "Plan actualizado exitosamente"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlanStatus(
            @PathVariable Integer id,
            @RequestParam Boolean estado) {
        return ResponseEntity.ok(ApiResponse.ok(planService.updateStatus(id, estado), "Estado del plan actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Integer id) {
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Plan desactivado exitosamente"));
    }
}
