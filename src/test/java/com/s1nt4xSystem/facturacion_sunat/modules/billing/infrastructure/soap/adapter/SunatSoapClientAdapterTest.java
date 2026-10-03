package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.soap.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.SunatSoapResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SunatSoapClientAdapterTest {

    private HttpServer mockSunatServer;
    private int port;
    private SunatSoapClientAdapter adapter;
    private Empresa empresa;
    private EmpresaConfig config;

    @BeforeEach
    void setUp() throws Exception {
        mockSunatServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockSunatServer.getAddress().getPort();
        mockSunatServer.start();

        String mockUrl = "http://localhost:" + port + "/billService";
        adapter = new SunatSoapClientAdapter(mockUrl, mockUrl, mockUrl);

        empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20600000001")
                .razonSocial("EMPRESA TEST")
                .entorno("dev")
                .build();

        config = EmpresaConfig.builder()
                .id(UUID.randomUUID())
                .userSol("MODDATOS")
                .passSol("moddatos")
                .build();
    }

    @AfterEach
    void tearDown() {
        if (mockSunatServer != null) {
            mockSunatServer.stop(0);
        }
    }

    private byte[] createMockCdrZip(String nombreCdrXml, String responseCode, String description) throws Exception {
        String cdrXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ar:ApplicationResponse xmlns:ar=\"urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2\"\n" +
                "                        xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n" +
                "                        xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n" +
                "    <cac:DocumentResponse>\n" +
                "        <cac:Response>\n" +
                "            <cbc:ResponseCode>" + responseCode + "</cbc:ResponseCode>\n" +
                "            <cbc:Description>" + description + "</cbc:Description>\n" +
                "        </cac:Response>\n" +
                "    </cac:DocumentResponse>\n" +
                "</ar:ApplicationResponse>";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(nombreCdrXml);
            zos.putNextEntry(entry);
            zos.write(cdrXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Debe procesar exitosamente la respuesta SOAP de SUNAT extrayendo el CDR y estado ACEPTADO")
    void testEnviarComprobanteAceptado() throws Exception {
        String baseName = "20600000001-01-F001-00000001";
        byte[] cdrZipBytes = createMockCdrZip("R-" + baseName + ".xml", "0", "La Factura número F001-00000001, ha sido aceptada");
        String cdrBase64 = Base64.getEncoder().encodeToString(cdrZipBytes);

        String soapResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soapenv:Body>\n" +
                "        <sendBillResponse xmlns=\"http://service.sunat.gob.pe\">\n" +
                "            <applicationResponse>" + cdrBase64 + "</applicationResponse>\n" +
                "        </sendBillResponse>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        mockSunatServer.createContext("/billService", exchange -> {
            byte[] responseBytes = soapResponseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        byte[] fakeSignedXml = "<Invoice>Firmado</Invoice>".getBytes(StandardCharsets.UTF_8);
        SunatSoapResult result = adapter.enviarComprobante(fakeSignedXml, baseName, empresa, config);

        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals("ACEPTADO", result.getResponse().getEstadoSunat());
        assertEquals("0", result.getResponse().getCodigoRespuestaSunat());
        assertTrue(result.getResponse().getMensajeSunat().contains("ha sido aceptada"));
        assertNotNull(result.getCdrZip());
        assertEquals("R-" + baseName + ".zip", result.getCdrNombreArchivo());
    }

    @Test
    @DisplayName("Debe manejar respuesta SOAP Fault cuando SUNAT rechaza el comprobante")
    void testEnviarComprobanteSoapFaultRechazado() throws Exception {
        String baseName = "20600000001-01-F001-00000002";
        String soapFaultBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soapenv:Body>\n" +
                "        <soapenv:Fault>\n" +
                "            <faultcode>soapenv:Client.2324</faultcode>\n" +
                "            <faultstring>El RUC del emisor no coincide con el archivo enviado</faultstring>\n" +
                "        </soapenv:Fault>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        mockSunatServer.createContext("/billService", exchange -> {
            byte[] responseBytes = soapFaultBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
            exchange.sendResponseHeaders(500, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        byte[] fakeSignedXml = "<Invoice>Firmado</Invoice>".getBytes(StandardCharsets.UTF_8);
        SunatSoapResult result = adapter.enviarComprobante(fakeSignedXml, baseName, empresa, config);

        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals("RECHAZADO", result.getResponse().getEstadoSunat());
        assertTrue(result.getResponse().getCodigoRespuestaSunat().contains("2324"));
        assertTrue(result.getResponse().getMensajeSunat().contains("RUC del emisor"));
        assertNull(result.getCdrZip());
    }
}
