package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.soap.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.DocumentoSunatResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.SunatSoapResult;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.SunatSoapPort;
import com.s1nt4xSystem.facturacion_sunat.infrastructure.sunat.SunatEndpoints;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component
@Slf4j
public class SunatSoapClientAdapter implements SunatSoapPort {

    private final String betaUrl;
    private final String prodUrl;
    private final String customSunatUrl;
    private final HttpClient httpClient;

    public SunatSoapClientAdapter(
            @Value("${app.sunat.soap.beta-url:#{null}}") String betaUrl,
            @Value("${app.sunat.soap.prod-url:#{null}}") String prodUrl,
            @Value("${app.sunat.soap.custom-url:}") String customSunatUrl) {
        this.betaUrl = (betaUrl != null && !betaUrl.isBlank()) ? betaUrl : SunatEndpoints.FE_BETA;
        this.prodUrl = (prodUrl != null && !prodUrl.isBlank()) ? prodUrl : SunatEndpoints.FE_PRODUCCION;
        this.customSunatUrl = customSunatUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public SunatSoapResult enviarComprobante(byte[] xmlFirmado, String nombreArchivoBase, Empresa empresa, EmpresaConfig config) {
        LocalDateTime fechaEnvio = LocalDateTime.now();

        if (config == null || config.getUserSol() == null || config.getPassSol() == null) {
            throw new DomainException("La empresa no tiene configuradas las credenciales SOL (usuario y clave SOL)");
        }

        // 1. Determinar URL del endpoint de SUNAT según el entorno del tenant
        String endpointUrl;
        if (customSunatUrl != null && !customSunatUrl.isBlank()) {
            endpointUrl = customSunatUrl.trim();
        } else if ("prod".equalsIgnoreCase(empresa.getEntorno()) || "produccion".equalsIgnoreCase(empresa.getEntorno())) {
            endpointUrl = prodUrl;
        } else if ("homologacion".equalsIgnoreCase(empresa.getEntorno()) || "qa".equalsIgnoreCase(empresa.getEntorno())) {
            endpointUrl = SunatEndpoints.FE_HOMOLOGACION;
        } else {
            endpointUrl = betaUrl;
        }

        try {
            // 2. Comprimir el XML firmado en un archivo ZIP
            byte[] zipBytes = comprimirEnZip(nombreArchivoBase + ".xml", xmlFirmado);
            String zipBase64 = Base64.getEncoder().encodeToString(zipBytes);

            // 3. Formar el usuario SOL completo: {RUC}{userSol}
            String usuarioSolCompleto = empresa.getRuc().trim() + config.getUserSol().trim().toUpperCase();
            String claveSol = config.getPassSol().trim();

            // 4. Construir Envelope SOAP 1.1 con WS-Security UsernameToken
            String soapEnvelope = buildSoapEnvelope(usuarioSolCompleto, claveSol, nombreArchivoBase + ".zip", zipBase64);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "")
                    .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope, StandardCharsets.UTF_8))
                    .build();

            log.info("Enviando comprobante {} a SUNAT (Endpoint: {})...", nombreArchivoBase, endpointUrl);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            LocalDateTime fechaRespuesta = LocalDateTime.now();

            String responseBody = response.body();
            int statusCode = response.statusCode();

            String faultCode = extraerValorTag(responseBody, "faultcode");
            String faultString = extraerValorTag(responseBody, "faultstring");

            if (statusCode == 200 && (faultCode == null || faultCode.isBlank())) {
                // Extracción exitosa del CDR en Base64
                String cdrBase64 = extraerValorTag(responseBody, "applicationResponse");
                if (cdrBase64 == null || cdrBase64.isBlank()) {
                    cdrBase64 = extraerValorTag(responseBody, "content");
                }

                if (cdrBase64 != null && !cdrBase64.isBlank()) {
                    byte[] cdrZipBytes = Base64.getDecoder().decode(cdrBase64.trim().replaceAll("\\s+", ""));
                    String cdrNombreArchivo = "R-" + nombreArchivoBase + ".zip";

                    // Extraer y parsear el XML de la Constancia de Recepción (CDR) directamente en memoria
                    CdrParseResult cdrResult = parsearCdr(cdrZipBytes);

                    String estadoSunat = "0".equals(cdrResult.codigoRespuesta()) ? "ACEPTADO"
                            : (cdrResult.codigoRespuesta() != null && cdrResult.codigoRespuesta().startsWith("01")) ? "OBSERVADO" : "RECHAZADO";

                    DocumentoSunatResponse sunatResp = DocumentoSunatResponse.builder()
                            .estadoSunat(estadoSunat)
                            .codigoRespuestaSunat(cdrResult.codigoRespuesta())
                            .mensajeSunat(cdrResult.descripcion())
                            .fechaEnvio(fechaEnvio)
                            .fechaRespuesta(fechaRespuesta)
                            .build();

                    log.info("Comprobante {} procesado por SUNAT: Estado={}, Código={}, Detalle={}",
                            nombreArchivoBase, estadoSunat, cdrResult.codigoRespuesta(), cdrResult.descripcion());

                    return SunatSoapResult.builder()
                            .response(sunatResp)
                            .cdrZip(cdrZipBytes)
                            .cdrNombreArchivo(cdrNombreArchivo)
                            .build();
                } else {
                    log.warn("SUNAT respondió HTTP 200 para {} pero no se encontró tag applicationResponse/content. Respuesta completa: {}",
                            nombreArchivoBase, responseBody);
                    return SunatSoapResult.builder()
                            .response(DocumentoSunatResponse.builder()
                                    .estadoSunat("ACEPTADO_SIN_CDR")
                                    .codigoRespuestaSunat("0")
                                    .mensajeSunat("Comprobante enviado pero la respuesta no incluyó contenido de CDR")
                                    .fechaEnvio(fechaEnvio)
                                    .fechaRespuesta(fechaRespuesta)
                                    .build())
                            .build();
                }

            } else {
                // Error SOAP Fault (bien sea HTTP 500 o HTTP 200 con soap:Fault) o HTTP diferente de 200
                String mensajeError = (faultString != null && !faultString.isBlank()) ? faultString : "Error HTTP " + statusCode + " en WebService SUNAT";
                String codigoError = (faultCode != null && !faultCode.isBlank()) ? faultCode : String.valueOf(statusCode);

                log.warn("SUNAT rechazó comprobante {}: [{}] {}", nombreArchivoBase, codigoError, mensajeError);

                return SunatSoapResult.builder()
                        .response(DocumentoSunatResponse.builder()
                                .estadoSunat("RECHAZADO")
                                .codigoRespuestaSunat(codigoError)
                                .mensajeSunat(mensajeError)
                                .fechaEnvio(fechaEnvio)
                                .fechaRespuesta(fechaRespuesta)
                                .build())
                        .build();
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DomainException("Envío interrumpido a SUNAT: " + ie.getMessage());
        } catch (Exception e) {
            log.error("Excepción al conectar con WebService SUNAT: {}", e.getMessage(), e);
            return SunatSoapResult.builder()
                    .response(DocumentoSunatResponse.builder()
                            .estadoSunat("ERROR_CONEXION")
                            .codigoRespuestaSunat("EXCEPCION")
                            .mensajeSunat("No se pudo establecer conexión con SUNAT: " + e.getMessage())
                            .fechaEnvio(fechaEnvio)
                            .fechaRespuesta(LocalDateTime.now())
                            .build())
                    .build();
        }
    }

    private byte[] comprimirEnZip(String nombreArchivoXml, byte[] xmlContent) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(nombreArchivoXml);
            zos.putNextEntry(entry);
            zos.write(xmlContent);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private CdrParseResult parsearCdr(byte[] cdrZipBytes) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(cdrZipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".xml")) {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    Document doc = dbf.newDocumentBuilder().parse(zis);

                    String responseCode = getFirstElementText(doc, "ResponseCode");
                    String description = getFirstElementText(doc, "Description");

                    return new CdrParseResult(
                            responseCode != null ? responseCode : "0",
                            description != null ? description : "Comprobante recibido satisfactoriamente por SUNAT"
                    );
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo parsear el XML dentro del CDR ZIP en memoria: {}", e.getMessage());
        }
        return new CdrParseResult("0", "Constancia de recepción recibida");
    }

    private String getFirstElementText(Document doc, String tagName) {
        NodeList nl = doc.getElementsByTagNameNS("*", tagName);
        if (nl.getLength() == 0) {
            nl = doc.getElementsByTagName(tagName);
        }
        if (nl.getLength() > 0) {
            return nl.item(0).getTextContent().trim();
        }
        return null;
    }

    private String extraerValorTag(String xml, String tagName) {
        if (xml == null || xml.isBlank()) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "<(?:[a-zA-Z0-9_.-]+:)?" + tagName + "(?:\\s+[^>]*)?>(.*?)</(?:[a-zA-Z0-9_.-]+:)?" + tagName + ">",
                java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String buildSoapEnvelope(String usuarioSol, String claveSol, String zipFileName, String zipBase64) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ser=\"http://service.sunat.gob.pe\" " +
                "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
                "    <soapenv:Header>\n" +
                "        <wsse:Security>\n" +
                "            <wsse:UsernameToken>\n" +
                "                <wsse:Username>" + escapeXml(usuarioSol) + "</wsse:Username>\n" +
                "                <wsse:Password>" + escapeXml(claveSol) + "</wsse:Password>\n" +
                "            </wsse:UsernameToken>\n" +
                "        </wsse:Security>\n" +
                "    </soapenv:Header>\n" +
                "    <soapenv:Body>\n" +
                "        <ser:sendBill>\n" +
                "            <fileName>" + escapeXml(zipFileName) + "</fileName>\n" +
                "            <contentFile>" + zipBase64 + "</contentFile>\n" +
                "        </ser:sendBill>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record CdrParseResult(String codigoRespuesta, String descripcion) {}
}
