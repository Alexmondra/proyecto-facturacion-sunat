package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.signature.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto.CertificadoDigitalResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.CertificadoDigitalService;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.CertificadoDigitalServiceImpl;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.xml.adapter.FreeMarkerXmlGeneratorAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class XmlDSigFirmaAdapterTest {

    private CertificadoDigitalService certificadoDigitalService;
    private XmlDSigFirmaAdapter firmaAdapter;
    private FreeMarkerXmlGeneratorAdapter xmlGeneratorAdapter;

    private String rutaCertificado;
    private String passwordCertificado;
    private String ruc;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        certificadoDigitalService = new CertificadoDigitalServiceImpl(tempDir.toString());
        firmaAdapter = new XmlDSigFirmaAdapter(certificadoDigitalService);
        xmlGeneratorAdapter = new FreeMarkerXmlGeneratorAdapter();

        ruc = "20000000001";
        passwordCertificado = "demoPassword123";

        // Cargar el certificado PEM de prueba y convertirlo a PKCS12 (.pfx)
        byte[] pemBytes;
        try (InputStream is = getClass().getResourceAsStream("/certificates/certificado_demo.pem")) {
            assertNotNull(is, "El certificado demo debe existir en src/test/resources/certificates/certificado_demo.pem");
            pemBytes = is.readAllBytes();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "certificado_demo.pem",
                "application/x-pem-file",
                pemBytes
        );

        CertificadoDigitalResponse certResp = certificadoDigitalService.procesarYGuardar(file, passwordCertificado, ruc);
        rutaCertificado = certResp.getRutaAlmacenamiento();
    }

    @Test
    @DisplayName("Debe firmar exitosamente el XML UBL 2.1 con XMLDSig y calcular el Hash CPE")
    void testFirmarXmlUblValido() throws Exception {
        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc(ruc)
                .razonSocial("EMPRESA DEMO S.A.C.")
                .direccionFiscal("CALLE LOS NEGOCIOS 123, LIMA")
                .entorno("dev")
                .build();

        Sucursal sucursal = Sucursal.builder()
                .id(UUID.randomUUID())
                .codigo("0000")
                .nombreSucursal("Casa Matriz")
                .ubigeo("150101")
                .direccion("CALLE LOS NEGOCIOS 123")
                .build();

        LineaComprobante linea = LineaComprobante.builder()
                .item(1)
                .codigoProducto("PROD-01")
                .descripcion("DESARROLLO DE SOFTWARE")
                .cantidad(new BigDecimal("1.00"))
                .unidadMedida("ZZ")
                .valorUnitario(new BigDecimal("500.00"))
                .precioUnitario(new BigDecimal("590.00"))
                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                .subtotal(new BigDecimal("500.00"))
                .totalTributos(new BigDecimal("90.00"))
                .total(new BigDecimal("590.00"))
                .tributos(List.of(
                        TributoLinea.builder()
                                .tributoId(1L)
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("500.00"))
                                .porcentaje(new BigDecimal("18.00"))
                                .monto(new BigDecimal("90.00"))
                                .build()
                ))
                .build();

        ComprobanteFiscal factura = ComprobanteFiscal.builder()
                .id(UUID.randomUUID())
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .numero(100)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20999999999")
                .clienteNombre("CLIENTE TEST S.A.C.")
                .formaPago("CONTADO")
                .subtotal(new BigDecimal("500.00"))
                .totalTributos(new BigDecimal("90.00"))
                .total(new BigDecimal("590.00"))
                .detalles(List.of(linea))
                .tributosGlobales(List.of(
                        TributoLinea.builder()
                                .tributoId(1L)
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("500.00"))
                                .porcentaje(new BigDecimal("18.00"))
                                .monto(new BigDecimal("90.00"))
                                .build()
                ))
                .build();

        byte[] xmlSinFirmar = xmlGeneratorAdapter.generarXml(factura, empresa, sucursal);
        assertNotNull(xmlSinFirmar);

        // Ejecutar firma digital
        ResultadoFirma resultado = firmaAdapter.firmarXml(xmlSinFirmar, rutaCertificado, passwordCertificado);

        assertNotNull(resultado);
        assertNotNull(resultado.getXmlFirmado());
        assertNotNull(resultado.getHashCpe());
        assertFalse(resultado.getHashCpe().isBlank(), "El Hash CPE debe ser generado");

        String xmlFirmadoStr = new String(resultado.getXmlFirmado(), StandardCharsets.UTF_8);
        assertTrue(xmlFirmadoStr.contains("<ds:Signature"));
        assertTrue(xmlFirmadoStr.contains("<ds:SignedInfo>"));
        assertTrue(xmlFirmadoStr.contains("<ds:SignatureValue>"));
        assertTrue(xmlFirmadoStr.contains("<ds:DigestValue>" + resultado.getHashCpe() + "</ds:DigestValue>"));
        assertTrue(xmlFirmadoStr.contains("<ds:X509Certificate>"));

        // Validación criptográfica con W3C XMLSignature DOMValidateContext
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document docFirmado = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(resultado.getXmlFirmado()));

        NodeList sigNodes = docFirmado.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        assertEquals(1, sigNodes.getLength(), "Debe existir exactamente un nodo de firma ds:Signature");

        KeyStore ks = certificadoDigitalService.cargarKeyStore(rutaCertificado, passwordCertificado);
        X509Certificate cert = (X509Certificate) ks.getCertificate("certificate");

        DOMValidateContext valContext = new DOMValidateContext(cert.getPublicKey(), sigNodes.item(0));
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);

        boolean coreValidity = signature.validate(valContext);
        assertTrue(coreValidity, "La firma digital XMLDSig debe ser criptográficamente válida");
    }
}
