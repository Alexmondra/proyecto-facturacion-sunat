package com.s1nt4xSystem.facturacion_sunat.modules.core.company.service;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse;
import org.springframework.web.multipart.MultipartFile;

import java.security.KeyStore;

public interface CertificadoDigitalService {

    /**
     * Procesa, valida y guarda el certificado digital en el almacenamiento del tenant.
     * Convierte archivos PEM automáticamente a PKCS12 (.pfx).
     *
     * @param file Archivo subido (.pfx, .p12 o .pem)
     * @param password Contraseña para desencriptar o proteger el certificado
     * @param ruc RUC del tenant actual
     * @return Metadatos e información de validez del certificado
     */
    CertificadoDigitalResponse procesarYGuardar(MultipartFile file, String password, String ruc);

    /**
     * Procesa, valida y guarda el certificado digital a partir de un arreglo de bytes.
     * Convierte archivos PEM automáticamente a PKCS12 (.pfx).
     */
    CertificadoDigitalResponse procesarYGuardarBytes(byte[] bytes, String originalFilename, String password, String ruc);

    /**
     * Obtiene los metadatos del certificado actualmente instalado en el tenant.
     *
     * @param rutaRelativa Ruta relativa guardada en empresa_config
     * @param password Contraseña del certificado
     * @param ruc RUC del tenant
     * @return Metadatos del certificado
     */
    CertificadoDigitalResponse obtenerMetadatos(String rutaRelativa, String password, String ruc);

    /**
     * Carga el KeyStore PKCS12 listo para la firma digital XML (XMLDSig).
     *
     * @param rutaRelativa Ruta relativa guardada en empresa_config
     * @param password Contraseña del certificado
     * @return KeyStore inicializado
     */
    KeyStore cargarKeyStore(String rutaRelativa, String password);

    /**
     * Comprueba si el archivo físico del certificado existe en el storage.
     *
     * @param rutaRelativa Ruta relativa guardada en empresa_config
     * @return true si el archivo existe físicamente y no está vacío
     */
    boolean existeCertificado(String rutaRelativa);
}
