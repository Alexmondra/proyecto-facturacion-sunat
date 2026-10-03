package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.soap;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.CertificadoDigitalServiceImpl;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.signature.adapter.XmlDSigFirmaAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.soap.adapter.SunatSoapClientAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.xml.adapter.FreeMarkerXmlGeneratorAdapter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SunatLiveTest {

    @Test
    void testLiveSendToSunatBeta() {
        int randomCorrelativo = 1000 + new Random().nextInt(80000);
        String serie = "F001";
        String ruc = "20000000001";

        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc(ruc)
                .razonSocial("EMPRESA DE PRUEBA SUNAT S.A.")
                .direccionFiscal("AV. GARCILASO DE LA VEGA 1456, LIMA")
                .entorno("beta")
                .build();

        Sucursal sucursal = Sucursal.builder()
                .id(UUID.randomUUID())
                .codigo("0000")
                .nombreSucursal("Matriz")
                .ubigeo("150101")
                .direccion("AV. GARCILASO DE LA VEGA 1456")
                .build();

        EmpresaConfig config = EmpresaConfig.builder()
                .id(UUID.randomUUID())
                .userSol("MODDATOS")
                .passSol("moddatos")
                .build();

        LineaComprobante linea = LineaComprobante.builder()
                .item(1)
                .codigoProducto("PROD-01")
                .descripcion("SERVICIO DE CONSULTORÍA TÉCNICA")
                .cantidad(BigDecimal.ONE)
                .unidadMedida("ZZ")
                .valorUnitario(new BigDecimal("100.00"))
                .precioUnitario(new BigDecimal("118.00"))
                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                .subtotal(new BigDecimal("100.00"))
                .totalTributos(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .tributos(List.of(
                        TributoLinea.builder()
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("100.00"))
                                .porcentaje(new BigDecimal("18.00"))
                                .monto(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        ComprobanteFiscal comprobante = ComprobanteFiscal.builder()
                .id(UUID.randomUUID())
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie(serie)
                .numero(randomCorrelativo)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .tipoOperacion("0101")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20600000001")
                .clienteNombre("CLIENTE DE PRUEBAS SAC")
                .clienteDireccion("AV LOS HEROES 123")
                .formaPago("CONTADO")
                .subtotal(new BigDecimal("100.00"))
                .totalTributos(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .detalles(List.of(linea))
                .totalesAfectacion(List.of(
                        TotalesAfectacion.builder()
                                .tipoAfectacionCodigo("10")
                                .tipoTotal("GRAVADO")
                                .baseImponible(new BigDecimal("100.00"))
                                .montoTributo(new BigDecimal("18.00"))
                                .montoTotal(new BigDecimal("118.00"))
                                .build()
                ))
                .tributosGlobales(List.of(
                        TributoLinea.builder()
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("100.00"))
                                .monto(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        // 1. Generar XML
        FreeMarkerXmlGeneratorAdapter xmlGen = new FreeMarkerXmlGeneratorAdapter();
        byte[] xmlBytes = xmlGen.generarXml(comprobante, empresa, sucursal);
        System.out.println("=== XML GENERADO (" + xmlBytes.length + " bytes) ===");

        // 2. Firmar XML con fallback demo
        CertificadoDigitalServiceImpl certService = new CertificadoDigitalServiceImpl("storage");
        XmlDSigFirmaAdapter firmaAdapter = new XmlDSigFirmaAdapter(certService);
        ResultadoFirma firmaResult = firmaAdapter.firmarXml(xmlBytes, null, null);
        System.out.println("=== XML FIRMADO (Hash: " + firmaResult.getHashCpe() + ") ===");

        // 3. Enviar a SUNAT Beta
        SunatSoapClientAdapter soapClient = new SunatSoapClientAdapter(null, null, null);
        String nombreBase = String.format("%s-%s-%s-%08d", ruc, "01", serie, randomCorrelativo);
        SunatSoapResult soapResult = soapClient.enviarComprobante(firmaResult.getXmlFirmado(), nombreBase, empresa, config);

        System.out.println("=== RESPUESTA SUNAT ===");
        if (soapResult.getResponse() != null) {
            System.out.println("Estado SUNAT: " + soapResult.getResponse().getEstadoSunat());
            System.out.println("Código SUNAT: " + soapResult.getResponse().getCodigoRespuestaSunat());
            System.out.println("Mensaje SUNAT: " + soapResult.getResponse().getMensajeSunat());
        }
        System.out.println("CDR ZIP bytes: " + (soapResult.getCdrZip() != null ? soapResult.getCdrZip().length : 0));
        System.out.println("CDR XML bytes: " + (soapResult.getCdrXml() != null ? soapResult.getCdrXml().length : 0));

        assertNotNull(soapResult);
        assertNotNull(soapResult.getResponse());
        org.junit.jupiter.api.Assertions.assertEquals("ACEPTADO", soapResult.getResponse().getEstadoSunat());
        org.junit.jupiter.api.Assertions.assertEquals("0", soapResult.getResponse().getCodigoRespuestaSunat());
        assertNotNull(soapResult.getCdrZip());
        org.junit.jupiter.api.Assertions.assertTrue(soapResult.getCdrZip().length > 0, "El ZIP del CDR no debe estar vacío");
    }
}
