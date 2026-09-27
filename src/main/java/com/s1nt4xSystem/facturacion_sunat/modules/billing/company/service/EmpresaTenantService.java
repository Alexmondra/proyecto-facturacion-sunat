package com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.EmpresaConfigRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.EmpresaConfigResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.EmpresaResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;

public interface EmpresaTenantService {
    EmpresaResponse getEmpresa();
    Empresa getEmpresaEntity();
    EmpresaConfigResponse getConfig();
    EmpresaConfigResponse saveOrUpdateConfig(EmpresaConfigRequest request);
    EmpresaResponse updateEmpresa(com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.EmpresaTenantUpdateRequest request);
    EmpresaResponse updateEnvironment(String entorno);
    com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.CertificadoDigitalResponse uploadCertificate(org.springframework.web.multipart.MultipartFile file, String password);
    com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.CertificadoDigitalResponse getCertificateInfo();
}
