package com.s1nt4xSystem.facturacion_sunat.modules.core.company.validation;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.CertificadoDigitalService;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.regex.Pattern;

/**
 * Validador centralizado de reglas de negocio para el módulo de Empresa (Company).
 * Encapsula la validación de RUC, razón social, credenciales SOL, modo de emisión y certificados.
 */
@Component
public class CompanyValidator {

    private static final Pattern RUC_PATTERN = Pattern.compile("^(10|20)[0-9]{9}$");

    private final EmpresaRepository empresaRepository;
    private final CertificadoDigitalService certificadoDigitalService;

    public CompanyValidator(EmpresaRepository empresaRepository,
                            @Lazy CertificadoDigitalService certificadoDigitalService) {
        this.empresaRepository = empresaRepository;
        this.certificadoDigitalService = certificadoDigitalService;
    }

    /**
     * Valida el formato de un RUC peruano (11 dígitos, comienza con 10 o 20).
     */
    public void validateRucFormat(String ruc) {
        if (ruc == null || !RUC_PATTERN.matcher(ruc.trim()).matches()) {
            throw new BusinessException(ErrorCode.COMPANY_INVALID_RUC, ruc);
        }
    }

    /**
     * Valida que el RUC no se encuentre previamente registrado en el repositorio.
     */
    public void validateRucUnique(String ruc) {
        if (ruc != null && empresaRepository.existsByRuc(ruc.trim())) {
            throw new BusinessException(ErrorCode.COMPANY_RUC_ALREADY_EXISTS, ruc.trim());
        }
    }

    /**
     * Valida que la razón social no esté vacía ni en blanco.
     */
    public void validateRazonSocial(String razonSocial) {
        if (razonSocial == null || razonSocial.trim().isBlank()) {
            throw new BusinessException(ErrorCode.COMPANY_RAZON_SOCIAL_REQUIRED);
        }
    }

    /**
     * Valida el modo de emisión (PROPIO o PSE) y sus requerimientos dependientes.
     */
    public void validateModoEmision(String modoEmision, Long idPse) {
        if (modoEmision == null || modoEmision.trim().isBlank()) {
            return;
        }
        String modo = modoEmision.trim().toUpperCase();
        if (!modo.equals("PROPIO") && !modo.equals("PSE")) {
            throw new BusinessException(ErrorCode.COMPANY_INVALID_MODO_EMISION, modoEmision);
        }
        if (modo.equals("PSE") && idPse == null) {
            throw new BusinessException(ErrorCode.COMPANY_PSE_REQUIRED);
        }
    }

    /**
     * Valida el archivo y contraseña al subir un certificado digital.
     */
    public void validateCertificateUpload(String filename, String password) {
        if (password == null || password.trim().isBlank()) {
            throw new BusinessException(ErrorCode.COMPANY_CERTIFICATE_PASSWORD_MISSING);
        }
        if (filename == null || (!filename.toLowerCase().endsWith(".pfx") 
                && !filename.toLowerCase().endsWith(".p12")
                && !filename.toLowerCase().endsWith(".pem"))) {
            throw new BusinessException(ErrorCode.COMPANY_INVALID_CERTIFICATE_FILE);
        }
    }

    /**
     * Valida que la empresa cumpla todos los requisitos fiscales previos a la emisión.
     * Si no es comprobante electrónico (ej. ticket interno), omite las verificaciones fiscales de SUNAT.
     */
    public void validateForEmission(Empresa empresa, EmpresaConfig config, boolean esElectronico) {
        if (empresa == null) {
            throw new BusinessException(ErrorCode.COMPANY_NOT_FOUND, "");
        }

        if (!esElectronico) {
            return; // Documento interno (Ticket/Nota de venta) no requiere credenciales SUNAT ni certificado
        }

        if (config == null) {
            throw new BusinessException(ErrorCode.COMPANY_CONFIG_NOT_FOUND);
        }

        // 1. Validar credenciales SOL
        if (config.getUserSol() == null || config.getUserSol().trim().isBlank()
                || config.getPassSol() == null || config.getPassSol().trim().isBlank()) {
            throw new BusinessException(ErrorCode.COMPANY_SOL_CREDENTIALS_MISSING);
        }

        // 2. Validar existencia física del certificado digital
        if (config.getCertificado() == null || config.getCertificado().trim().isBlank()
                || !certificadoDigitalService.existeCertificado(config.getCertificado())) {
            throw new BusinessException(ErrorCode.COMPANY_CERTIFICATE_NOT_FOUND);
        }
    }

    /**
     * Valida la presencia de un archivo multipart para certificado digital.
     */
    public void validateCertificadoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMPANY_CERTIFICATE_FILE_REQUIRED);
        }
    }

    /**
     * Valida el payload de bytes y RUC al procesar un certificado digital.
     */
    public void validateCertificadoBytes(byte[] bytes, String ruc) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ErrorCode.COMPANY_CERTIFICATE_CONTENT_EMPTY);
        }
        if (ruc == null || ruc.isBlank()) {
            throw new BusinessException(ErrorCode.COMPANY_INVALID_RUC, "");
        }
    }

    /**
     * Valida la contraseña requerida para certificados en formato binario (PFX / P12).
     */
    public void validatePasswordParaPfx(boolean isPem, String password) {
        if (!isPem && (password == null || password.isBlank())) {
            throw new BusinessException(ErrorCode.COMPANY_CERTIFICATE_PASSWORD_MISSING);
        }
    }

    /**
     * Valida la contraseña si un archivo PEM contiene clave privada cifrada.
     */
    public void validatePasswordPemCifrado(boolean containsEncryptedKey, String password) {
        if (containsEncryptedKey && (password == null || password.isBlank())) {
            throw new BusinessException(ErrorCode.COMPANY_CERTIFICATE_ENCRYPTED_PEM_PASSWORD_REQUIRED);
        }
    }

    /**
     * Valida que el certificado contenga la clave privada necesaria para firmar.
     */
    public void validatePrivateKeyPresent(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new BusinessException(ErrorCode.COMPANY_CERTIFICATE_NO_PRIVATE_KEY);
        }
    }

    /**
     * Valida que la ruta de almacenamiento esté configurada y el archivo exista en disco.
     */
    public void validateRutaCertificadoFisico(String rutaRelativa, Path certPath) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new BusinessException(ErrorCode.COMPANY_CERTIFICATE_STORAGE_PATH_REQUIRED);
        }
        if (!Files.exists(certPath)) {
            throw new BusinessException(ErrorCode.COMPANY_CERTIFICATE_FILE_NOT_FOUND_ON_DISK, rutaRelativa);
        }
    }

    /**
     * Valida que el entorno no esté vacío.
     */
    public void validateEntorno(String entorno) {
        if (entorno == null || entorno.isBlank()) {
            throw new BusinessException(ErrorCode.COMPANY_ENVIRONMENT_REQUIRED);
        }
    }
}
