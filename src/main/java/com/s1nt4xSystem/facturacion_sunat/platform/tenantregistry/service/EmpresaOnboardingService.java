package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.service;

import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaOnboardingRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaRouterResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaUpdateRequest;

import java.util.List;

public interface EmpresaOnboardingService {
    EmpresaRouterResponse onboardEmpresa(EmpresaOnboardingRequest request);
    EmpresaRouterResponse getEmpresaRouterById(Long id);
    List<EmpresaRouterResponse> getAllEmpresas();
    List<EmpresaRouterResponse> getEmpresasBySaasId(Long saasId);
    EmpresaRouterResponse updateEmpresa(Long id, EmpresaUpdateRequest request);
    EmpresaRouterResponse updateEstado(Long id, String estado);
    EmpresaRouterResponse regenerateAccessKey(Long id);
    void deleteEmpresa(Long id);
}
