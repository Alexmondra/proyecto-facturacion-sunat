package com.s1nt4xSystem.facturacion_sunat.modules.core.company.service;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.*;

class CertificadoDigitalServiceTest {

    private CertificadoDigitalService certificadoDigitalService;
    private String tempStoragePath;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        tempStoragePath = tempDir.toString();
        certificadoDigitalService = new CertificadoDigitalServiceImpl(tempStoragePath);
    }

    private byte[] getDemoPemBytes() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/certificates/certificado_demo.pem")) {
            if (is != null) {
                return is.readAllBytes();
            }
        }
        Path demoPemPath = Paths.get("storage/tenants/20000000001/certificates/certificado_demo.pem");
        if (Files.exists(demoPemPath)) {
            return Files.readAllBytes(demoPemPath);
        }
        throw new IllegalStateException("No se encontró el certificado demo ni en classpath ni en storage");
    }

    @Test
    @DisplayName("Debe procesar exitosamente un archivo .pem, convertirlo a .pfx y almacenarlo")
    void testProcesarYGuardarPemDemo() throws Exception {
        byte[] pemContent = getDemoPemBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "certificado_demo.pem",
                "application/x-pem-file",
                pemContent
        );

        String ruc = "20000000001";
        String password = "demoPassword123";

        CertificadoDigitalResponse response = certificadoDigitalService.procesarYGuardar(file, password, ruc);

        assertNotNull(response);
        assertEquals(ruc, response.getRuc());
        assertEquals("PEM", response.getFormatoOriginal());
        assertEquals("tenants/20000000001/certificates/certificate.pfx", response.getRutaAlmacenamiento());
        assertTrue(response.getSujeto().contains("CERTIFICADO PARA DEMOSTRACIÓN") || response.getSujeto().contains("LLAMA.PE"));
        assertNotNull(response.getValidoDesde());
        assertNotNull(response.getValidoHasta());
        assertNotNull(response.getNumeroSerie());

        // Verificar que el archivo PFX fue creado físicamente en tempDir
        Path pfxPath = Paths.get(tempStoragePath, response.getRutaAlmacenamiento());
        assertTrue(Files.exists(pfxPath), "El archivo .pfx generado debe existir físicamente");

        // Verificar que el KeyStore puede ser cargado con la clave configurada
        KeyStore ks = certificadoDigitalService.cargarKeyStore(response.getRutaAlmacenamiento(), password);
        assertNotNull(ks);
        assertTrue(ks.containsAlias("certificate"));

        // Verificar que obtenerMetadatos funciona sobre el archivo PFX generado
        CertificadoDigitalResponse metaResponse = certificadoDigitalService.obtenerMetadatos(
                response.getRutaAlmacenamiento(),
                password,
                ruc
        );
        assertNotNull(metaResponse);
        assertEquals(response.getNumeroSerie(), metaResponse.getNumeroSerie());
        assertEquals(response.getValidoHasta(), metaResponse.getValidoHasta());
    }

    @Test
    @DisplayName("Debe procesar exitosamente la subida de un archivo .pfx binario")
    void testProcesarYGuardarPfxDirecto() throws Exception {
        // Primero generamos un PFX a partir del pem demo
        byte[] pemContent = getDemoPemBytes();
        MockMultipartFile pemFile = new MockMultipartFile("file", "demo.pem", "application/x-pem-file", pemContent);
        
        CertificadoDigitalResponse pemResponse = certificadoDigitalService.procesarYGuardar(pemFile, "ClavePfx123", "20000000001");
        Path generatedPfx = Paths.get(tempStoragePath, pemResponse.getRutaAlmacenamiento());
        byte[] pfxBytes = Files.readAllBytes(generatedPfx);

        // Ahora simulamos la subida directa de este archivo .pfx para otro tenant
        MockMultipartFile pfxFile = new MockMultipartFile("file", "mi_certificado.pfx", "application/x-pkcs12", pfxBytes);
        String ruc2 = "20609998877";
        String nuevaClave = "ClaveNueva456";

        CertificadoDigitalResponse pfxResponse = certificadoDigitalService.procesarYGuardar(pfxFile, "ClavePfx123", ruc2);

        assertNotNull(pfxResponse);
        assertEquals(ruc2, pfxResponse.getRuc());
        assertEquals("PFX", pfxResponse.getFormatoOriginal());
        assertEquals("tenants/20609998877/certificates/certificate.pfx", pfxResponse.getRutaAlmacenamiento());
        assertNotNull(pfxResponse.getNumeroSerie());

        Path targetPfx = Paths.get(tempStoragePath, pfxResponse.getRutaAlmacenamiento());
        assertTrue(Files.exists(targetPfx));
    }

    @Test
    @DisplayName("Debe rechazar subida si el archivo está vacío")
    void testValidacionArchivoVacio() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "vacio.pem", "text/plain", new byte[0]);
        assertThrows(DomainException.class, () ->
                certificadoDigitalService.procesarYGuardar(emptyFile, "123456", "20000000001"));
    }

    @Test
    @DisplayName("Debe procesar PEM sin contraseña generando una clave automáticamente")
    void testProcesarPemSinPasswordAutoGeneraClave() throws Exception {
        byte[] pemContent = getDemoPemBytes();
        MockMultipartFile file = new MockMultipartFile("file", "demo.pem", "text/plain", pemContent);

        CertificadoDigitalResponse resp = certificadoDigitalService.procesarYGuardar(file, null, "20000000001");
        assertNotNull(resp);
        assertNotNull(resp.getClaveAsignada());
        assertTrue(resp.getClaveAsignada().startsWith("Pfx_"));

        Path pfxPath = Paths.get(tempStoragePath, resp.getRutaAlmacenamiento());
        assertTrue(Files.exists(pfxPath));
        java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
        try (java.io.InputStream is = Files.newInputStream(pfxPath)) {
            ks.load(is, resp.getClaveAsignada().toCharArray());
        }
        assertTrue(ks.containsAlias("certificate"));
    }

    @Test
    @DisplayName("Debe rechazar PFX si la contraseña está en blanco")
    void testValidacionPasswordBlancoParaPfx() throws Exception {
        byte[] pemContent = getDemoPemBytes();
        MockMultipartFile pemFile = new MockMultipartFile("file", "demo.pem", "application/x-pem-file", pemContent);
        CertificadoDigitalResponse pemResp = certificadoDigitalService.procesarYGuardar(pemFile, "ClavePfx123", "20000000001");
        Path pfxPath = Paths.get(tempStoragePath, pemResp.getRutaAlmacenamiento());
        byte[] pfxBytes = Files.readAllBytes(pfxPath);

        MockMultipartFile pfxFile = new MockMultipartFile("file", "demo.pfx", "application/x-pkcs12", pfxBytes);

        DomainException ex = assertThrows(DomainException.class, () ->
                certificadoDigitalService.procesarYGuardar(pfxFile, "   ", "20000000001"));
        assertTrue(ex.getMessage().contains("PFX/P12"));
    }

    @Test
    @DisplayName("Debe rechazar PFX con contraseña incorrecta")
    void testPfxPasswordIncorrecta() throws Exception {
        byte[] pemContent = getDemoPemBytes();
        MockMultipartFile pemFile = new MockMultipartFile("file", "demo.pem", "application/x-pem-file", pemContent);
        
        CertificadoDigitalResponse pemResp = certificadoDigitalService.procesarYGuardar(pemFile, "ClaveCorrecta", "20000000001");
        Path generatedPfx = Paths.get(tempStoragePath, pemResp.getRutaAlmacenamiento());
        byte[] pfxBytes = Files.readAllBytes(generatedPfx);

        MockMultipartFile pfxFile = new MockMultipartFile("file", "cert.pfx", "application/x-pkcs12", pfxBytes);
        assertThrows(DomainException.class, () ->
                certificadoDigitalService.procesarYGuardar(pfxFile, "PasswordEquivocada", "20000000001"));
    }

    @Test
    @DisplayName("Debe lanzar DomainException si el archivo no contiene una clave privada válida")
    void testPemSinClavePrivada() throws Exception {
        String fullPem = new String(getDemoPemBytes(), java.nio.charset.StandardCharsets.UTF_8);
        // Extraemos solo el certificado sin la clave privada
        int startCert = fullPem.indexOf("-----BEGIN CERTIFICATE-----");
        int endCert = fullPem.indexOf("-----END CERTIFICATE-----") + "-----END CERTIFICATE-----".length();
        String soloCertificado = fullPem.substring(startCert, endCert);

        MockMultipartFile file = new MockMultipartFile("file", "incompleto.pem", "text/plain", soloCertificado.getBytes());

        DomainException ex = assertThrows(DomainException.class, () ->
                certificadoDigitalService.procesarYGuardar(file, "123456", "20000000001"));
        assertTrue(ex.getMessage().contains("clave privada"));
    }

    @Test
    @DisplayName("La serialización JSON no debe exponer rutaAlmacenamiento ni claveAsignada")
    void testJsonNoExponeCamposSensibles() throws Exception {
        byte[] pemContent = getDemoPemBytes();
        MockMultipartFile file = new MockMultipartFile("file", "demo.pem", "text/plain", pemContent);
        CertificadoDigitalResponse resp = certificadoDigitalService.procesarYGuardar(file, null, "20000000001");

        tools.jackson.databind.json.JsonMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        String json = mapper.writeValueAsString(resp);

        assertFalse(json.contains("rutaAlmacenamiento"), "El JSON nunca debe exponer la ruta física del archivo");
        assertFalse(json.contains("claveAsignada"), "El JSON nunca debe exponer contraseñas internas");
        assertTrue(json.contains("tieneCertificado"));
        assertTrue(json.contains("numeroSerie"));
    }
}
