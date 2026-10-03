package com.s1nt4xSystem.facturacion_sunat.modules.core.branch.service;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.dto.SucursalRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.dto.SucursalResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.dto.SucursalUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;

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
