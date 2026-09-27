package com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.controller;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto.SucursalRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto.SucursalResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto.SucursalUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.service.SucursalService;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant/branches")
public class SucursalController {

    private final SucursalService sucursalService;

    public SucursalController(SucursalService sucursalService) {
        this.sucursalService = sucursalService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SucursalResponse>> createSucursal(
            @Valid @RequestBody SucursalRequest request) {
        SucursalResponse response = sucursalService.createSucursal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Sucursal creada exitosamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SucursalResponse>>> getAllSucursales() {
        return ResponseEntity.ok(ApiResponse.ok(sucursalService.getAllSucursales()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SucursalResponse>> getSucursalById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(sucursalService.getSucursalById(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SucursalResponse>> patchSucursal(
            @PathVariable UUID id,
            @Valid @RequestBody SucursalUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sucursalService.updateSucursal(id, request), "Sucursal actualizada exitosamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SucursalResponse>> updateSucursal(
            @PathVariable UUID id,
            @Valid @RequestBody SucursalUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sucursalService.updateSucursal(id, request), "Sucursal actualizada exitosamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSucursal(@PathVariable UUID id) {
        sucursalService.deleteSucursal(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Sucursal desactivada exitosamente"));
    }
}
