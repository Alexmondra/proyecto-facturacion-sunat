package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.signature.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.CertificadoDigitalService;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ResultadoFirma;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.FirmaDigitalPort;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class XmlDSigFirmaAdapter implements FirmaDigitalPort {

    private final CertificadoDigitalService certificadoDigitalService;

    @Override
    public ResultadoFirma firmarXml(byte[] xmlBytes, String rutaCertificado, String passwordCertificado) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException(
                    com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode.DOCUMENT_XML_EMPTY);
        }

        try {
            // 1. Cargar KeyStore PKCS12 (o fallback demo si no está configurado)
            KeyStore keyStore = certificadoDigitalService.cargarKeyStore(rutaCertificado, passwordCertificado);

            Enumeration<String> aliases = keyStore.aliases();
            String alias = null;
            PrivateKey privateKey = null;
            X509Certificate cert = null;
            char[] passChars = passwordCertificado != null ? passwordCertificado.toCharArray() : new char[0];

            while (aliases.hasMoreElements()) {
                String currentAlias = aliases.nextElement();
                if (keyStore.isKeyEntry(currentAlias)) {
                    Key key = null;
                    try {
                        key = keyStore.getKey(currentAlias, passChars);
                    } catch (Exception ex) {
                        try {
                            key = keyStore.getKey(currentAlias, "".toCharArray());
                        } catch (Exception ignored) {
                        }
                    }
                    if (key == null && passChars.length > 0) {
                        try {
                            key = keyStore.getKey(currentAlias, "".toCharArray());
                        } catch (Exception ignored) {
                        }
                    }
                    if (key instanceof PrivateKey pk) {
                        alias = currentAlias;
                        privateKey = pk;
                        cert = (X509Certificate) keyStore.getCertificate(alias);
                        break;
                    }
                }
            }

            if (privateKey == null || cert == null) {
                throw new com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException(
                        com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode.DOCUMENT_XML_SIGN_NO_KEY);
            }

            // 2. Parsear el XML a Document W3C DOM con soporte de namespaces
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));

            // 3. Localizar el nodo de inserción: ext:ExtensionContent dentro de ext:UBLExtensions
            NodeList extensionNodes = doc.getElementsByTagNameNS(
                    "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2", "ExtensionContent");
            if (extensionNodes.getLength() == 0) {
                extensionNodes = doc.getElementsByTagName("ext:ExtensionContent");
            }
            if (extensionNodes.getLength() == 0) {
                throw new com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException(
                        com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode.DOCUMENT_XML_SIGN_NO_EXTENSION);
            }
            Node targetNode = extensionNodes.item(0);

            // 4. Configurar la firma digital XMLDSig
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            // Transformaciones requeridas por SUNAT: Enveloped Signature
            List<Transform> transformList = new ArrayList<>();
            transformList.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));

            // Algoritmo de Digest SHA-256
            DigestMethod dm = fac.newDigestMethod(DigestMethod.SHA256, null);
            Reference ref = fac.newReference("", dm, transformList, null, null);

            // Algoritmo de Canonicalización W3C Inclusive C14N y Firma RSA-SHA256
            CanonicalizationMethod cm = fac.newCanonicalizationMethod(
                    CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null);
            SignatureMethod sm = fac.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null);
            SignedInfo si = fac.newSignedInfo(cm, sm, Collections.singletonList(ref));

            // Inclusión del Certificado X.509 en KeyInfo
            KeyInfoFactory kif = fac.getKeyInfoFactory();
            X509Data x509Data = kif.newX509Data(Collections.singletonList(cert));
            KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509Data));

            // Crear el contexto de firma e insertar el nodo con prefijo ds:
            XMLSignature signature = fac.newXMLSignature(si, ki, null, "SignSUNAT", null);
            DOMSignContext signContext = new DOMSignContext(privateKey, targetNode);
            signContext.setDefaultNamespacePrefix("ds");

            // Ejecutar la firma digital
            signature.sign(signContext);

            // 5. Extraer el Hash CPE (DigestValue de la referencia calculada)
            String hashCpe;
            if (ref.getDigestValue() != null) {
                hashCpe = Base64.getEncoder().encodeToString(ref.getDigestValue());
            } else {
                NodeList digestNodes = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "DigestValue");
                if (digestNodes.getLength() > 0) {
                    hashCpe = digestNodes.item(0).getTextContent().trim();
                } else {
                    hashCpe = "";
                }
            }

            // 6. Serializar Document DOM firmado a arreglo de bytes UTF-8
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            trans.setOutputProperty(OutputKeys.INDENT, "no");
            trans.transform(new DOMSource(doc), new StreamResult(baos));

            byte[] xmlFirmado = baos.toByteArray();
            log.info("XML firmado con éxito. Hash CPE calculado: {}", hashCpe);

            return ResultadoFirma.builder()
                    .xmlFirmado(xmlFirmado)
                    .hashCpe(hashCpe)
                    .build();

        } catch (DomainException de) {
            throw de;
        } catch (Exception e) {
            log.error("Fallo crítico durante la firma digital XMLDSig: {}", e.getMessage(), e);
            throw new DomainException("Error al firmar digitalmente el XML UBL 2.1: " + e.getMessage());
        }
    }
}
