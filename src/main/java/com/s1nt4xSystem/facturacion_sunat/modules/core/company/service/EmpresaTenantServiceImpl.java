package com.s1nt4xSystem.facturacion_sunat.modules.core.company.service;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.EmpresaConfigRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.EmpresaConfigResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.EmpresaResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaConfigRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmpresaTenantServiceImpl implements EmpresaTenantService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaConfigRepository empresaConfigRepository;
    private final CertificadoDigitalService certificadoDigitalService;
    private final com.s1nt4xSystem.facturacion_sunat.modules.core.company.validation.CompanyValidator companyValidator;

    public EmpresaTenantServiceImpl(
            EmpresaRepository empresaRepository,
            EmpresaConfigRepository empresaConfigRepository,
            CertificadoDigitalService certificadoDigitalService,
            com.s1nt4xSystem.facturacion_sunat.modules.core.company.validation.CompanyValidator companyValidator) {
        this.empresaRepository = empresaRepository;
        this.empresaConfigRepository = empresaConfigRepository;
        this.certificadoDigitalService = certificadoDigitalService;
        this.companyValidator = companyValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaResponse getEmpresa() {
        Empresa empresa = getEmpresaEntity();
        EmpresaConfig config = empresaConfigRepository.findByEmpresaId(empresa.getId()).orElse(null);
        com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse certInfo = null;
        if (config != null && config.getCertificado() != null && !config.getCertificado().isBlank()) {
            try {
                certInfo = certificadoDigitalService.obtenerMetadatos(
                        config.getCertificado(), config.getCertificadoPass(), empresa.getRuc());
            } catch (Exception e) {
                // Si el archivo físico aún no existe o hay error de lectura, se continúa sin bloquear
            }
        }
        return EmpresaResponse.fromEntity(empresa, config, certInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Empresa getEmpresaEntity() {
        return empresaRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no inicializada en este tenant"));
    }

    @Override
    @Transactional
    public EmpresaConfigResponse getConfig() {
        Empresa empresa = getEmpresaEntity();
        EmpresaConfig config = empresaConfigRepository.findByEmpresaId(empresa.getId())
                .orElseGet(() -> {
                    EmpresaConfig nueva = EmpresaConfig.builder()
                            .empresa(empresa)
                            .envioAsincrono(true)
                            .modoEmision("PROPIO")
                            .build();
                    return empresaConfigRepository.save(nueva);
                });
        return EmpresaConfigResponse.fromEntity(config);
    }

    @Override
    public EmpresaConfigResponse saveOrUpdateConfig(EmpresaConfigRequest request) {
        Empresa empresa = getEmpresaEntity();
        EmpresaConfig config = empresaConfigRepository.findByEmpresaId(empresa.getId())
                .orElseGet(() -> EmpresaConfig.builder().empresa(empresa).build());

        if (request.getEnvioAsincrono() != null) {
            config.setEnvioAsincrono(request.getEnvioAsincrono());
        }
        if (request.getModoEmision() != null && !request.getModoEmision().isBlank()) {
            String modo = request.getModoEmision().trim().toUpperCase();
            if ("DIRECTO_SUNAT".equals(modo) || "SUNAT".equals(modo)) {
                modo = "PROPIO";
            }
            companyValidator.validateModoEmision(modo, request.getIdPse());
            config.setModoEmision(modo);
        }
        if (request.getIdPse() != null) {
            config.setIdPse(request.getIdPse());
        }
        if (request.getWebhookUrl() != null) {
            config.setWebhookUrl(request.getWebhookUrl().trim());
        }
        if (request.getTipoCertificado() != null && !request.getTipoCertificado().isBlank()) {
            config.setTipoCertificado(request.getTipoCertificado().trim().toUpperCase());
        }
        if (request.getCertificado() != null && !request.getCertificado().isBlank()) {
            config.setCertificado(request.getCertificado().trim());
        }
        if (request.getCertificadoPass() != null && !request.getCertificadoPass().isBlank()) {
            config.setCertificadoPass(request.getCertificadoPass());
        }
        if (request.getUserSol() != null && !request.getUserSol().isBlank()) {
            config.setUserSol(request.getUserSol().trim());
        }
        if (request.getPassSol() != null && !request.getPassSol().isBlank()) {
            config.setPassSol(request.getPassSol());
        }
        if (request.getSunatClientId() != null && !request.getSunatClientId().isBlank()) {
            config.setSunatClientId(request.getSunatClientId().trim());
        }
        if (request.getSunatClientSecret() != null && !request.getSunatClientSecret().isBlank()) {
            config.setSunatClientSecret(request.getSunatClientSecret());
        }
        if (request.getNumeroCuentaDetraccion() != null && !request.getNumeroCuentaDetraccion().isBlank()) {
            config.setNumeroCuentaDetraccion(request.getNumeroCuentaDetraccion().trim());
        }

        EmpresaConfig saved = empresaConfigRepository.save(config);
        return EmpresaConfigResponse.fromEntity(saved);
    }

    @Override
    public EmpresaResponse updateEmpresa(com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.EmpresaTenantUpdateRequest request) {
        Empresa empresa = getEmpresaEntity();
        if (request.getRazonSocial() != null) {
            companyValidator.validateRazonSocial(request.getRazonSocial());
            empresa.setRazonSocial(request.getRazonSocial().trim());
        }
        if (request.getDireccionFiscal() != null) {
            empresa.setDireccionFiscal(request.getDireccionFiscal().trim());
        }
        if (request.getLogo() != null) {
            empresa.setLogo(request.getLogo());
        }
        if (request.getIncluidoTributo() != null) {
            empresa.setIncluidoTributo(request.getIncluidoTributo());
        }
        if (request.getEntorno() != null && !request.getEntorno().isBlank()) {
            empresa.setEntorno(request.getEntorno().trim().toUpperCase());
        }
        Empresa finalEmpresa = empresaRepository.save(empresa);

        // Actualizar configuración fiscal si se proporcionan campos de configuración
        EmpresaConfig config = empresaConfigRepository.findByEmpresaId(finalEmpresa.getId())
                .orElseGet(() -> EmpresaConfig.builder().empresa(finalEmpresa).build());

        boolean configUpdated = false;
        if (request.getEnvioAsincrono() != null) {
            config.setEnvioAsincrono(request.getEnvioAsincrono());
            configUpdated = true;
        }
        if (request.getModoEmision() != null && !request.getModoEmision().isBlank()) {
            String modo = request.getModoEmision().trim().toUpperCase();
            if ("DIRECTO_SUNAT".equals(modo) || "SUNAT".equals(modo)) {
                modo = "PROPIO";
            }
            companyValidator.validateModoEmision(modo, request.getIdPse());
            config.setModoEmision(modo);
            configUpdated = true;
        }
        if (request.getIdPse() != null) {
            config.setIdPse(request.getIdPse());
            configUpdated = true;
        }
        if (request.getWebhookUrl() != null) {
            config.setWebhookUrl(request.getWebhookUrl().trim());
            configUpdated = true;
        }
        if (request.getUserSol() != null) {
            config.setUserSol(request.getUserSol().trim());
            configUpdated = true;
        }
        if (request.getPassSol() != null && !request.getPassSol().isBlank()) {
            config.setPassSol(request.getPassSol());
            configUpdated = true;
        }
        if (request.getSunatClientId() != null) {
            config.setSunatClientId(request.getSunatClientId().trim());
            configUpdated = true;
        }
        if (request.getSunatClientSecret() != null && !request.getSunatClientSecret().isBlank()) {
            config.setSunatClientSecret(request.getSunatClientSecret());
            configUpdated = true;
        }
        if (request.getNumeroCuentaDetraccion() != null) {
            config.setNumeroCuentaDetraccion(request.getNumeroCuentaDetraccion().trim());
            configUpdated = true;
        }

        if (configUpdated || (config != null && config.getId() == null)) {
            EmpresaConfig savedConfig = empresaConfigRepository.save(config);
            if (savedConfig != null) {
                config = savedConfig;
            }
        }

        com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse certInfo = null;
        if (config != null && config.getCertificado() != null && !config.getCertificado().isBlank()) {
            try {
                certInfo = certificadoDigitalService.obtenerMetadatos(
                        config.getCertificado(), config.getCertificadoPass(), empresa.getRuc());
            } catch (Exception e) {
                // Si el archivo físico aún no existe o hay error de lectura, se continúa sin bloquear
            }
        }

        return EmpresaResponse.fromEntity(empresa, config, certInfo);
    }

    @Override
    public EmpresaResponse updateEnvironment(String entorno) {
        companyValidator.validateEntorno(entorno);
        Empresa empresa = getEmpresaEntity();
        empresa.setEntorno(entorno.trim().toUpperCase());
        return EmpresaResponse.fromEntity(empresaRepository.save(empresa));
    }

    @Override
    public com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse uploadCertificate(
            org.springframework.web.multipart.MultipartFile file, String password) {
        Empresa empresa = getEmpresaEntity();
        companyValidator.validateCertificateUpload(file != null ? file.getOriginalFilename() : null, password);
        com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse response = 
                certificadoDigitalService.procesarYGuardar(file, password, empresa.getRuc());

        EmpresaConfig config = empresaConfigRepository.findByEmpresaId(empresa.getId())
                .orElseGet(() -> EmpresaConfig.builder().empresa(empresa).build());

        config.setTipoCertificado("PFX");
        config.setCertificado(response.getRutaAlmacenamiento());
        String claveFinal = response.getClaveAsignada() != null ? response.getClaveAsignada() : password;
        config.setCertificadoPass(claveFinal);
        empresaConfigRepository.save(config);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse getCertificateInfo() {
        Empresa empresa = getEmpresaEntity();
        EmpresaConfig config = empresaConfigRepository.findByEmpresaId(empresa.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe configuración fiscal para la empresa"));

        if (config.getCertificado() == null || config.getCertificado().isBlank()) {
            throw new ResourceNotFoundException("La empresa no cuenta con un certificado digital configurado");
        }

        return certificadoDigitalService.obtenerMetadatos(
                config.getCertificado(),
                config.getCertificadoPass(),
                empresa.getRuc()
        );
    }
}
