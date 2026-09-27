package com.s1nt4xSystem.facturacion_sunat.modules.billing.company.controller;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.EmpresaResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.EmpresaTenantUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.CertificadoDigitalResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.EmpresaTenantService;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenant/company")
public class EmpresaTenantController {

    private final EmpresaTenantService empresaTenantService;

    public EmpresaTenantController(EmpresaTenantService empresaTenantService) {
        this.empresaTenantService = empresaTenantService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<EmpresaResponse>> getCompany() {
        return ResponseEntity.ok(ApiResponse.ok(empresaTenantService.getEmpresa()));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<EmpresaResponse>> patchCompany(
            @Valid @RequestBody EmpresaTenantUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(empresaTenantService.updateEmpresa(request), "Empresa y configuración actualizadas exitosamente"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<EmpresaResponse>> updateCompany(
            @Valid @RequestBody EmpresaTenantUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(empresaTenantService.updateEmpresa(request), "Empresa y configuración actualizadas exitosamente"));
    }

    @PostMapping(value = "/certificate", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CertificadoDigitalResponse>> uploadCertificate(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "password", required = false) String password) {
        CertificadoDigitalResponse response = empresaTenantService.uploadCertificate(file, password);
        return ResponseEntity.ok(ApiResponse.ok(response, "Certificado digital procesado y configurado exitosamente"));
    }
}
