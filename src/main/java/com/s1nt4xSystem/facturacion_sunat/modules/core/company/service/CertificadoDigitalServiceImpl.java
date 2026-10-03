package com.s1nt4xSystem.facturacion_sunat.modules.core.company.service;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.DomainException;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.validation.CompanyValidator;

@Service
public class CertificadoDigitalServiceImpl implements CertificadoDigitalService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoDigitalServiceImpl.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final String storageBasePath;
    private final CompanyValidator companyValidator;

    @org.springframework.beans.factory.annotation.Autowired
    public CertificadoDigitalServiceImpl(
            @Value("${app.storage.base-path:storage}") String storageBasePath,
            CompanyValidator companyValidator) {
        this.storageBasePath = storageBasePath;
        this.companyValidator = companyValidator;
    }

    public CertificadoDigitalServiceImpl(String storageBasePath) {
        this.storageBasePath = storageBasePath;
        this.companyValidator = new CompanyValidator(null, this);
    }

    @Override
    public CertificadoDigitalResponse procesarYGuardar(MultipartFile file, String password, String ruc) {
        companyValidator.validateCertificadoFile(file);
        try {
            return procesarYGuardarBytes(file.getBytes(), file.getOriginalFilename(), password, ruc);
        } catch (DomainException de) {
            throw de;
        } catch (Exception e) {
            log.error("Error al leer el archivo de certificado digital: {}", e.getMessage(), e);
            throw new DomainException("Error al leer el archivo de certificado digital: " + e.getMessage());
        }
    }

    @Override
    public CertificadoDigitalResponse procesarYGuardarBytes(byte[] bytes, String originalFilename, String password, String ruc) {
        companyValidator.validateCertificadoBytes(bytes, ruc);

        try {
            String filename = originalFilename != null ? originalFilename.toLowerCase() : "";
            boolean isPem = detectIsPem(filename, bytes);
            String formatoOriginal = isPem ? "PEM" : (filename.endsWith(".p12") ? "P12" : "PFX");

            companyValidator.validatePasswordParaPfx(isPem, password);

            String effectivePassword;
            boolean claveAutoGenerada = false;

            if (isPem) {
                String pemContent = new String(bytes, StandardCharsets.UTF_8);
                boolean containsEncryptedKey = pemContent.contains("BEGIN ENCRYPTED PRIVATE KEY")
                        || pemContent.contains("Proc-Type: 4,ENCRYPTED");

                companyValidator.validatePasswordPemCifrado(containsEncryptedKey, password);

                if (password == null || password.isBlank()) {
                    effectivePassword = "Pfx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
                    claveAutoGenerada = true;
                } else {
                    effectivePassword = password.trim();
                }
            } else {
                effectivePassword = password.trim();
            }

            ParsedCertificateResult result = isPem 
                    ? parsePem(bytes, effectivePassword)
                    : parsePkcs12(bytes, effectivePassword);

            companyValidator.validatePrivateKeyPresent(result.privateKey());

            // Normalizar y almacenar en formato PKCS#12 estándar (.pfx)
            Path certDir = Paths.get(storageBasePath, "tenants", ruc.trim(), "certificates");
            Files.createDirectories(certDir);
            Path targetFile = certDir.resolve("certificate.pfx");

            KeyStore outputKs = KeyStore.getInstance("PKCS12");
            outputKs.load(null, null);
            outputKs.setKeyEntry("certificate", result.privateKey(), effectivePassword.toCharArray(), result.chain());

            try (OutputStream os = Files.newOutputStream(targetFile)) {
                outputKs.store(os, effectivePassword.toCharArray());
            }

            String rutaRelativa = "tenants/" + ruc.trim() + "/certificates/certificate.pfx";
            log.info("Certificado digital para tenant RUC {} guardado exitosamente en {}", ruc, targetFile.toAbsolutePath());

            return buildResponse(ruc, result.primaryCert(), formatoOriginal, rutaRelativa, effectivePassword, claveAutoGenerada);

        } catch (DomainException de) {
            throw de;
        } catch (Exception e) {
            log.error("Error al procesar el certificado digital para RUC {}: {}", ruc, e.getMessage(), e);
            throw new DomainException("Error al procesar el certificado digital: " + e.getMessage());
        }
    }

    @Override
    public CertificadoDigitalResponse obtenerMetadatos(String rutaRelativa, String password, String ruc) {
        Path certPath = (rutaRelativa != null && !rutaRelativa.isBlank())
                ? Paths.get(storageBasePath, rutaRelativa)
                : null;
        companyValidator.validateRutaCertificadoFisico(rutaRelativa, certPath);

        try {
            byte[] bytes = Files.readAllBytes(certPath);
            ParsedCertificateResult result = parsePkcs12(bytes, password != null ? password.trim() : "");
            return buildResponse(ruc, result.primaryCert(), "PFX", rutaRelativa, password, false);
        } catch (DomainException de) {
            throw de;
        } catch (Exception e) {
            log.error("Error al leer metadatos del certificado para RUC {}: {}", ruc, e.getMessage(), e);
            throw new DomainException("No se pudo leer el certificado digital configurado: " + e.getMessage());
        }
    }

    @Override
    public KeyStore cargarKeyStore(String rutaRelativa, String password) {
        if (rutaRelativa != null && !rutaRelativa.isBlank()) {
            Path certPath = Paths.get(storageBasePath, rutaRelativa);
            if (Files.exists(certPath)) {
                try (InputStream is = Files.newInputStream(certPath)) {
                    KeyStore ks = KeyStore.getInstance("PKCS12");
                    ks.load(is, password != null ? password.toCharArray() : new char[0]);
                    return ks;
                } catch (Exception e) {
                    try (InputStream is = Files.newInputStream(certPath)) {
                        KeyStore ks = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
                        ks.load(is, password != null ? password.toCharArray() : new char[0]);
                        return ks;
                    } catch (Exception e2) {
                        log.warn("Fallo al cargar KeyStore desde {}: {}. Intentando fallback demo.", rutaRelativa, e2.getMessage());
                    }
                }
            } else {
                log.info("Archivo de certificado {} no encontrado físicamente. Usando certificado demo de respaldo.", rutaRelativa);
            }
        }

        // Fallback: cargar certificado demo para pruebas / SUNAT Beta
        log.info("Cargando certificado digital demo desde classpath (/certificates/certificado_demo.pem)...");
        try (InputStream is = getClass().getResourceAsStream("/certificates/certificado_demo.pem")) {
            if (is != null) {
                byte[] pemBytes = is.readAllBytes();
                ParsedCertificateResult demoParsed = parsePem(pemBytes, "");
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(null, null);
                char[] entryPass = (password != null && !password.isBlank()) ? password.toCharArray() : "".toCharArray();
                ks.setKeyEntry("certificate", demoParsed.privateKey(), entryPass, demoParsed.chain());
                return ks;
            }
        } catch (Exception e) {
            log.error("No se pudo inicializar el certificado demo: {}", e.getMessage(), e);
        }

        throw new com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException(
                com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode.COMPANY_CERTIFICATE_NOT_FOUND);
    }

    @Override
    public boolean existeCertificado(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return false;
        }
        if ("DEMO".equalsIgnoreCase(rutaRelativa.trim()) || rutaRelativa.startsWith("classpath:")) {
            return getClass().getResourceAsStream("/certificates/certificado_demo.pem") != null;
        }
        try {
            Path certPath = Paths.get(storageBasePath, rutaRelativa);
            return Files.exists(certPath) && Files.isRegularFile(certPath) && Files.size(certPath) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean detectIsPem(String filename, byte[] bytes) {
        if (filename.endsWith(".pem") || filename.endsWith(".crt") || filename.endsWith(".cer")) {
            return true;
        }
        String preview = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.UTF_8);
        return preview.contains("-----BEGIN") || preview.contains("Bag Attributes");
    }

    private ParsedCertificateResult parsePem(byte[] bytes, String password) throws Exception {
        List<X509Certificate> certs = new ArrayList<>();
        PrivateKey privateKey = null;

        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);

        try (PEMParser pemParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            Object obj;
            while ((obj = pemParser.readObject()) != null) {
                if (obj instanceof X509CertificateHolder holder) {
                    certs.add(certConverter.getCertificate(holder));
                } else if (obj instanceof PEMKeyPair keyPair) {
                    privateKey = keyConverter.getPrivateKey(keyPair.getPrivateKeyInfo());
                } else if (obj instanceof PrivateKeyInfo pki) {
                    privateKey = keyConverter.getPrivateKey(pki);
                } else if (obj instanceof PKCS8EncryptedPrivateKeyInfo encryptedPki) {
                    try {
                        InputDecryptorProvider decryptorProvider = new JcePKCSPBEInputDecryptorProviderBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                .build(password != null ? password.toCharArray() : new char[0]);
                        PrivateKeyInfo pki = encryptedPki.decryptPrivateKeyInfo(decryptorProvider);
                        privateKey = keyConverter.getPrivateKey(pki);
                    } catch (Exception e) {
                        throw new DomainException("El archivo PEM está protegido con contraseña y la clave ingresada no es válida para descifrarlo.");
                    }
                } else if (obj instanceof org.bouncycastle.openssl.PEMEncryptedKeyPair encryptedKeyPair) {
                    try {
                        org.bouncycastle.openssl.PEMDecryptorProvider decProv = new org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                .build(password != null ? password.toCharArray() : new char[0]);
                        org.bouncycastle.openssl.PEMKeyPair decryptedKeyPair = encryptedKeyPair.decryptKeyPair(decProv);
                        privateKey = keyConverter.getPrivateKey(decryptedKeyPair.getPrivateKeyInfo());
                    } catch (Exception e) {
                        throw new DomainException("El archivo PEM está protegido con contraseña y la clave ingresada no es válida para descifrarlo.");
                    }
                }
            }
        }

        if (certs.isEmpty()) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> parsedCerts = cf.generateCertificates(new ByteArrayInputStream(bytes));
            for (Certificate c : parsedCerts) {
                if (c instanceof X509Certificate x509) {
                    certs.add(x509);
                }
            }
        }

        if (certs.isEmpty()) {
            throw new DomainException("El archivo PEM no contiene ningún certificado X.509 válido");
        }
        if (privateKey == null) {
            throw new DomainException("El archivo PEM no contiene una clave privada válida (PKCS#1, PKCS#8 o cifrada)");
        }

        return new ParsedCertificateResult(certs.get(0), certs.toArray(new X509Certificate[0]), privateKey);
    }

    private ParsedCertificateResult parsePkcs12(byte[] bytes, String password) throws Exception {
        KeyStore ks;
        char[] passChars = password != null ? password.toCharArray() : new char[0];
        try {
            ks = KeyStore.getInstance("PKCS12");
            ks.load(new ByteArrayInputStream(bytes), passChars);
        } catch (Exception e) {
            try {
                ks = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
                ks.load(new ByteArrayInputStream(bytes), passChars);
            } catch (Exception e2) {
                throw new DomainException("Contraseña incorrecta o archivo de certificado PKCS12/PFX inválido");
            }
        }

        Enumeration<String> aliases = ks.aliases();
        List<X509Certificate> chainList = new ArrayList<>();
        PrivateKey privateKey = null;

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isKeyEntry(alias)) {
                Key key = ks.getKey(alias, passChars);
                if (key instanceof PrivateKey pk) {
                    privateKey = pk;
                }
                Certificate[] chain = ks.getCertificateChain(alias);
                if (chain != null) {
                    for (Certificate c : chain) {
                        if (c instanceof X509Certificate x509) {
                            chainList.add(x509);
                        }
                    }
                }
                break;
            }
        }

        if (chainList.isEmpty()) {
            Enumeration<String> allAliases = ks.aliases();
            while (allAliases.hasMoreElements()) {
                Certificate c = ks.getCertificate(allAliases.nextElement());
                if (c instanceof X509Certificate x509) {
                    chainList.add(x509);
                }
            }
        }

        if (chainList.isEmpty()) {
            throw new DomainException("El archivo PKCS12 no contiene ningún certificado X.509");
        }

        return new ParsedCertificateResult(chainList.get(0), chainList.toArray(new X509Certificate[0]), privateKey);
    }

    private CertificadoDigitalResponse buildResponse(String ruc, X509Certificate cert, String formatoOriginal, String rutaRelativa, String claveAsignada, boolean autoGenerada) {
        LocalDateTime notBefore = cert.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime notAfter = cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        long diasRestantes = ChronoUnit.DAYS.between(LocalDateTime.now(), notAfter);
        boolean vencido = LocalDateTime.now().isAfter(notAfter);

        String mensaje;
        if (vencido) {
            mensaje = "El certificado digital venció el " + notAfter.toLocalDate() + ". Se requiere renovación para emitir ante SUNAT.";
        } else if (diasRestantes <= 30) {
            mensaje = "El certificado digital vencerá pronto (" + diasRestantes + " días restantes, vence el " + notAfter.toLocalDate() + ").";
        } else {
            mensaje = "Certificado digital activo y válido por " + diasRestantes + " días (vence el " + notAfter.toLocalDate() + ").";
        }

        if (autoGenerada) {
            mensaje += " Se configuró exitosamente en el almacén digital seguro para la firma electrónica.";
        }

        return CertificadoDigitalResponse.builder()
                .ruc(ruc)
                .sujeto(cert.getSubjectX500Principal().getName())
                .emisor(cert.getIssuerX500Principal().getName())
                .numeroSerie(cert.getSerialNumber().toString(16).toUpperCase())
                .validoDesde(notBefore)
                .validoHasta(notAfter)
                .diasRestantes(diasRestantes)
                .vencido(vencido)
                .formatoOriginal(formatoOriginal)
                .rutaAlmacenamiento(rutaRelativa)
                .claveAsignada(claveAsignada)
                .mensaje(mensaje)
                .build();
    }

    private record ParsedCertificateResult(
            X509Certificate primaryCert,
            X509Certificate[] chain,
            PrivateKey privateKey
    ) {}
}
