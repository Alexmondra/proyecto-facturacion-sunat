package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.controller;

import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaOnboardingRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaRouterResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.service.EmpresaOnboardingService;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/companies")
public class EmpresaRouterController {

    private final EmpresaOnboardingService onboardingService;
    private final com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.validation.EmpresaRouterValidator empresaRouterValidator;

    public EmpresaRouterController(
            EmpresaOnboardingService onboardingService,
            com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.validation.EmpresaRouterValidator empresaRouterValidator) {
        this.onboardingService = onboardingService;
        this.empresaRouterValidator = empresaRouterValidator;
    }

    @PostMapping("/onboard")
    public ResponseEntity<ApiResponse<EmpresaRouterResponse>> onboardCompany(
            @Valid @RequestBody EmpresaOnboardingRequest request) {
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && principal.isCliente()) {
            request.setSaasId(principal.getSaasId());
        }
        EmpresaRouterResponse response = onboardingService.onboardEmpresa(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Empresa registrada y esquema PostgreSQL aprovisionado con éxito"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmpresaRouterResponse>>> getAllCompanies() {
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && principal.isCliente()) {
            return ResponseEntity.ok(ApiResponse.ok(onboardingService.getEmpresasBySaasId(principal.getSaasId())));
        }
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.getAllEmpresas()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpresaRouterResponse>> getCompanyById(@PathVariable Long id) {
        EmpresaRouterResponse response = onboardingService.getEmpresaRouterById(id);
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && principal.isCliente()) {
            empresaRouterValidator.validateAccountOwnership(response, principal.getSaasId());
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/by-account/{saasId}")
    public ResponseEntity<ApiResponse<List<EmpresaRouterResponse>>> getCompaniesByAccountId(@PathVariable Long saasId) {
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && principal.isCliente()) {
            empresaRouterValidator.validateSameAccountAccess(saasId, principal.getSaasId());
        }
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.getEmpresasBySaasId(saasId)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpresaRouterResponse>> patchCompany(
            @PathVariable Long id,
            @Valid @RequestBody com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaUpdateRequest request) {
        validateCompanyOwnership(id);
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.updateEmpresa(id, request), "Empresa actualizada exitosamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmpresaRouterResponse>> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaUpdateRequest request) {
        validateCompanyOwnership(id);
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.updateEmpresa(id, request), "Empresa actualizada exitosamente"));
    }

    @PatchMapping("/{id}/regenerate-key")
    public ResponseEntity<ApiResponse<EmpresaRouterResponse>> regenerateAccessKey(@PathVariable Long id) {
        validateCompanyOwnership(id);
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.regenerateAccessKey(id), "Access Key de la empresa regenerada exitosamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Long id) {
        validateCompanyOwnership(id);
        onboardingService.deleteEmpresa(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Empresa dada de baja exitosamente"));
    }

    private void validateCompanyOwnership(Long empresaRouterId) {
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && principal.isCliente()) {
            EmpresaRouterResponse company = onboardingService.getEmpresaRouterById(empresaRouterId);
            empresaRouterValidator.validateAccountOwnership(company, principal.getSaasId());
        }
    }
}
