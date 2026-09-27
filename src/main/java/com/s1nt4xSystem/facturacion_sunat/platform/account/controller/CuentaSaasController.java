package com.s1nt4xSystem.facturacion_sunat.platform.account.controller;

import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.account.service.CuentaSaasService;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/accounts")
public class CuentaSaasController {

    private final CuentaSaasService cuentaSaasService;

    public CuentaSaasController(CuentaSaasService cuentaSaasService) {
        this.cuentaSaasService = cuentaSaasService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CuentaSaasResponse>> createAccount(@Valid @RequestBody CuentaSaasRequest request) {
        CuentaSaasResponse response = cuentaSaasService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Cuenta SaaS creada exitosamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CuentaSaasResponse>>> getAllAccounts() {
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && !principal.isAdmin()) {
            throw new com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException("Acceso denegado: Solo el Administrador puede listar todas las cuentas");
        }
        return ResponseEntity.ok(ApiResponse.ok(cuentaSaasService.getAllAccounts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CuentaSaasResponse>> getAccountById(@PathVariable Long id) {
        validateAccountOwnership(id);
        return ResponseEntity.ok(ApiResponse.ok(cuentaSaasService.getAccountById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CuentaSaasResponse>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody CuentaSaasRequest request) {
        validateAccountOwnership(id);
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && !principal.isAdmin()) {
            // Un cliente no puede auto-cambiarse el plan ni el tipo de cuenta
            request.setPlanId(null);
            request.setTipo(null);
            request.setEstado(null);
        }
        return ResponseEntity.ok(ApiResponse.ok(cuentaSaasService.updateAccount(id, request), "Cuenta SaaS actualizada exitosamente"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CuentaSaasResponse>> updateAccountStatus(
            @PathVariable Long id,
            @RequestParam Boolean estado) {
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && !principal.isAdmin()) {
            throw new com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException("Solo el Administrador del sistema puede suspender o activar cuentas");
        }
        return ResponseEntity.ok(ApiResponse.ok(cuentaSaasService.updateStatus(id, estado), "Estado de la cuenta SaaS actualizado"));
    }

    @PatchMapping("/{id}/regenerate-key")
    public ResponseEntity<ApiResponse<CuentaSaasResponse>> regenerateAccessKey(@PathVariable Long id) {
        validateAccountOwnership(id);
        return ResponseEntity.ok(ApiResponse.ok(cuentaSaasService.regenerateAccessKey(id), "Access Key regenerada exitosamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && !principal.isAdmin()) {
            throw new com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException("Solo el Administrador del sistema puede dar de baja cuentas");
        }
        cuentaSaasService.deleteAccount(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Cuenta SaaS desactivada exitosamente"));
    }

    private void validateAccountOwnership(Long accountId) {
        com.s1nt4xSystem.facturacion_sunat.infrastructure.security.AuthenticatedPrincipal principal =
                com.s1nt4xSystem.facturacion_sunat.infrastructure.security.SecurityContext.getPrincipal();
        if (principal != null && principal.isCliente() && !accountId.equals(principal.getSaasId())) {
            throw new com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException("Acceso denegado: No puede consultar o modificar otra cuenta SaaS");
        }
    }
}
