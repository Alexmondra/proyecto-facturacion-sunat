package com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto.SucursalRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto.SucursalResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.dto.SucursalUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;

import java.util.List;
import java.util.UUID;

public interface SucursalService {
    SucursalResponse createSucursal(SucursalRequest request);
    SucursalResponse getSucursalById(UUID id);
    Sucursal getSucursalEntity(UUID id);
    List<SucursalResponse> getAllSucursales();
    SucursalResponse updateSucursal(UUID id, SucursalUpdateRequest request);
    SucursalResponse updateSucursal(UUID id, SucursalRequest request);
    SucursalResponse updateStatus(UUID id, Boolean activo);
    void deleteSucursal(UUID id);
}
