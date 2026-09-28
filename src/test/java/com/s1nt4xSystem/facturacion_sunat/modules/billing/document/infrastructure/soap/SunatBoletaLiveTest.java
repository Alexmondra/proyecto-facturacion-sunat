package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.soap;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.CertificadoDigitalServiceImpl;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.signature.adapter.XmlDSigFirmaAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.soap.adapter.SunatSoapClientAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.xml.adapter.FreeMarkerXmlGeneratorAdapter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SunatBoletaLiveTest {

    @Test
    void testLiveSendBoletaToSunatBeta() {
        int randomCorrelativo = 1000 + new Random().nextInt(80000);
        String serie = "B001";
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
                .descripcion("VENTA DE MERCADERIA")
                .cantidad(BigDecimal.ONE)
                .unidadMedida("NIU")
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
                .tipoComprobante(TipoComprobante.BOLETA)
                .serie(serie)
                .numero(randomCorrelativo)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .tipoOperacion("0101")
                .clienteTipoDoc("1")
                .clienteNumeroDoc("12345678")
                .clienteNombre("JUAN PEREZ")
                .clienteDireccion("JR LIMA 123")
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

        FreeMarkerXmlGeneratorAdapter xmlGen = new FreeMarkerXmlGeneratorAdapter();
        byte[] xmlBytes = xmlGen.generarXml(comprobante, empresa, sucursal);

        CertificadoDigitalServiceImpl certService = new CertificadoDigitalServiceImpl("storage");
        XmlDSigFirmaAdapter firmaAdapter = new XmlDSigFirmaAdapter(certService);
        ResultadoFirma firmaResult = firmaAdapter.firmarXml(xmlBytes, null, null);

        SunatSoapClientAdapter soapClient = new SunatSoapClientAdapter(null, null, null);
        String nombreBase = String.format("%s-%s-%s-%08d", ruc, "03", serie, randomCorrelativo);
        SunatSoapResult soapResult = soapClient.enviarComprobante(firmaResult.getXmlFirmado(), nombreBase, empresa, config);

        System.out.println("=== RESPUESTA SUNAT PARA BOLETA ===");
        if (soapResult.getResponse() != null) {
            System.out.println("Estado SUNAT: " + soapResult.getResponse().getEstadoSunat());
            System.out.println("Código SUNAT: " + soapResult.getResponse().getCodigoRespuestaSunat());
            System.out.println("Mensaje SUNAT: " + soapResult.getResponse().getMensajeSunat());
        }
        System.out.println("CDR ZIP bytes: " + (soapResult.getCdrZip() != null ? soapResult.getCdrZip().length : 0));
    }
}
