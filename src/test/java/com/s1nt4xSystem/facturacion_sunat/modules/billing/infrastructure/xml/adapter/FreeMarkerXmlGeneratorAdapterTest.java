package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.xml.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FreeMarkerXmlGeneratorAdapterTest {

    private FreeMarkerXmlGeneratorAdapter adapter;
    private Empresa empresa;
    private Sucursal sucursal;

    @BeforeEach
    void setUp() {
        adapter = new FreeMarkerXmlGeneratorAdapter();

        empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20600000001")
                .razonSocial("EMPRESA DEMO S.A.C.")
                .direccionFiscal("CALLE LOS NEGOCIOS 123, LIMA")
                .entorno("dev")
                .build();

        sucursal = Sucursal.builder()
                .id(UUID.randomUUID())
                .codigo("0000")
                .nombreSucursal("Casa Matriz")
                .ubigeo("150101")
                .direccion("CALLE LOS NEGOCIOS 123")
                .build();
    }

    @Test
    @DisplayName("Debe generar XML UBL 2.1 válido para Factura Electrónica (01)")
    void testGenerarXmlFactura() {
        LineaComprobante linea = LineaComprobante.builder()
                .item(1)
                .codigoProducto("P001")
                .descripcion("SERVICIO DE CONSULTORÍA IT")
                .cantidad(new BigDecimal("1.00"))
                .unidadMedida("ZZ")
                .valorUnitario(new BigDecimal("100.00"))
                .precioUnitario(new BigDecimal("118.00"))
                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                .subtotal(new BigDecimal("100.00"))
                .totalTributos(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .tributos(List.of(
                        TributoLinea.builder()
                                .tributoId(1L)
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("100.00"))
                                .porcentaje(new BigDecimal("18.00"))
                                .monto(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        ComprobanteFiscal factura = ComprobanteFiscal.builder()
                .id(UUID.randomUUID())
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .numero(1)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .tipoOperacion("0101")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20555555551")
                .clienteNombre("CLIENTE CORPORATIVO S.A.")
                .clienteDireccion("AV. JAVIER PRADO 456, SAN ISIDRO")
                .formaPago("CONTADO")
                .subtotal(new BigDecimal("100.00"))
                .totalTributos(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .detalles(List.of(linea))
                .tributosGlobales(List.of(
                        TributoLinea.builder()
                                .tributoId(1L)
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("100.00"))
                                .porcentaje(new BigDecimal("18.00"))
                                .monto(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        byte[] xmlBytes = adapter.generarXml(factura, empresa, sucursal);
        assertNotNull(xmlBytes);
        assertTrue(xmlBytes.length > 0);

        String xml = new String(xmlBytes, StandardCharsets.UTF_8);
        assertTrue(xml.contains("<cbc:UBLVersionID>2.1</cbc:UBLVersionID>"));
        assertTrue(xml.contains("<cbc:CustomizationID>2.0</cbc:CustomizationID>"));
        assertTrue(xml.contains("<cbc:ID>F001-00000001</cbc:ID>"));
        assertTrue(xml.contains("<cbc:InvoiceTypeCode listAgencyName=\"PE:SUNAT\" listID=\"0101\" listName=\"Tipo de Documento\" listURI=\"urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01\">01</cbc:InvoiceTypeCode>"));
        assertTrue(xml.contains("<cbc:ID schemeAgencyName=\"PE:SUNAT\" schemeID=\"6\" schemeName=\"Documento de Identidad\" schemeURI=\"urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06\">20600000001</cbc:ID>"));
        assertTrue(xml.contains("EMPRESA DEMO S.A.C."));
        assertTrue(xml.contains("CLIENTE CORPORATIVO S.A."));
        assertTrue(xml.contains("SERVICIO DE CONSULTORÍA IT"));
        assertTrue(xml.contains("<ext:ExtensionContent>"));
    }

    @Test
    @DisplayName("Debe generar XML UBL 2.1 válido para Nota de Crédito (07)")
    void testGenerarXmlNotaCredito() {
        LineaComprobante linea = LineaComprobante.builder()
                .item(1)
                .descripcion("DEVOLUCIÓN DE MERCADERÍA")
                .cantidad(new BigDecimal("1.00"))
                .unidadMedida("NIU")
                .valorUnitario(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("50.00"))
                .totalTributos(new BigDecimal("9.00"))
                .total(new BigDecimal("59.00"))
                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                .build();

        ReferenciaComprobante ref = ReferenciaComprobante.builder()
                .tipoRelacion("afecta")
                .tipoDocumentoRef("01")
                .serieRef("F001")
                .numeroRef(10)
                .motivoCodigo("01")
                .motivoDescripcion("ANULACIÓN DE LA OPERACIÓN")
                .build();

        ComprobanteFiscal notaCredito = ComprobanteFiscal.builder()
                .id(UUID.randomUUID())
                .tipoComprobante(TipoComprobante.NOTA_CREDITO)
                .serie("FC01")
                .numero(1)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20555555551")
                .clienteNombre("CLIENTE CORPORATIVO S.A.")
                .subtotal(new BigDecimal("50.00"))
                .totalTributos(new BigDecimal("9.00"))
                .total(new BigDecimal("59.00"))
                .detalles(List.of(linea))
                .referencias(List.of(ref))
                .build();

        byte[] xmlBytes = adapter.generarXml(notaCredito, empresa, sucursal);
        assertNotNull(xmlBytes);

        String xml = new String(xmlBytes, StandardCharsets.UTF_8);
        assertTrue(xml.contains("<CreditNote xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2\""));
        assertTrue(xml.contains("<cbc:ID>FC01-00000001</cbc:ID>"));
        assertTrue(xml.contains("<cbc:ResponseCode listAgencyName=\"PE:SUNAT\" listName=\"Tipo de nota de credito\" listURI=\"urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo09\">01</cbc:ResponseCode>"));
        assertTrue(xml.contains("ANULACIÓN DE LA OPERACIÓN"));
        assertTrue(xml.contains("<cbc:ID>F001-10</cbc:ID>"));
    }

    @Test
    @DisplayName("Debe generar XML UBL 2.1 válido para Boleta con ICBPER (Bolsas plásticas)")
    void testGenerarXmlBoletaConIcbper() {
        LineaComprobante lineaBolsa = LineaComprobante.builder()
                .item(1)
                .codigoProducto("BOLSA01")
                .descripcion("BOLSA PLÁSTICA")
                .cantidad(new BigDecimal("2.00"))
                .unidadMedida("NIU")
                .valorUnitario(new BigDecimal("0.10"))
                .precioUnitario(new BigDecimal("0.12"))
                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                .subtotal(new BigDecimal("0.20"))
                .totalTributos(new BigDecimal("1.04")) // 0.04 IGV + 1.00 ICBPER
                .total(new BigDecimal("1.24"))
                .tributos(List.of(
                        TributoLinea.builder()
                                .tributoId(1L)
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("0.20"))
                                .porcentaje(new BigDecimal("18.00"))
                                .monto(new BigDecimal("0.04"))
                                .build(),
                        TributoLinea.builder()
                                .tributoId(3L)
                                .codigoTributo("7152")
                                .nombreTributo("ICBPER")
                                .tipoTributo("OTH")
                                .baseImponible(BigDecimal.ZERO)
                                .porcentaje(BigDecimal.ZERO)
                                .cantidadBase(new BigDecimal("2"))
                                .monto(new BigDecimal("1.00"))
                                .build()
                ))
                .build();

        ComprobanteFiscal boleta = ComprobanteFiscal.builder()
                .id(UUID.randomUUID())
                .tipoComprobante(TipoComprobante.BOLETA)
                .serie("B001")
                .numero(99)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .tipoOperacion("0101")
                .clienteTipoDoc("1")
                .clienteNumeroDoc("12345678")
                .clienteNombre("JUAN PEREZ")
                .formaPago("CONTADO")
                .subtotal(new BigDecimal("0.20"))
                .totalTributos(new BigDecimal("1.04"))
                .total(new BigDecimal("1.24"))
                .detalles(List.of(lineaBolsa))
                .tributosGlobales(List.of(
                        TributoLinea.builder()
                                .tributoId(1L)
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("0.20"))
                                .porcentaje(new BigDecimal("18.00"))
                                .monto(new BigDecimal("0.04"))
                                .build(),
                        TributoLinea.builder()
                                .tributoId(3L)
                                .codigoTributo("7152")
                                .nombreTributo("ICBPER")
                                .tipoTributo("OTH")
                                .baseImponible(BigDecimal.ZERO)
                                .porcentaje(BigDecimal.ZERO)
                                .cantidadBase(new BigDecimal("2"))
                                .monto(new BigDecimal("1.00"))
                                .build()
                ))
                .build();

        byte[] xmlBytes = adapter.generarXml(boleta, empresa, sucursal);
        assertNotNull(xmlBytes);

        String xml = new String(xmlBytes, StandardCharsets.UTF_8);
        assertTrue(xml.contains("<cbc:ID>B001-00000099</cbc:ID>"));
        // Validación cabecera TaxTotal único con suma de tributos 1.04
        assertTrue(xml.contains("<cac:TaxTotal>"));
        assertTrue(xml.contains("<cbc:TaxAmount currencyID=\"PEN\">1.04</cbc:TaxAmount>"));
        // Validación ICBPER cabecera
        assertTrue(xml.contains("<cbc:ID schemeAgencyName=\"PE:SUNAT\" schemeID=\"UN/ECE 5153\" schemeName=\"Codigo de tributos\">7152</cbc:ID>"));
        assertTrue(xml.contains("<cbc:Name>ICBPER</cbc:Name>"));
        assertTrue(xml.contains("<cbc:TaxTypeCode>OTH</cbc:TaxTypeCode>"));
        // Validación ICBPER línea con BaseUnitMeasure y PerUnitAmount
        assertTrue(xml.contains("<cbc:BaseUnitMeasure unitCode=\"NIU\">2</cbc:BaseUnitMeasure>"));
        assertTrue(xml.contains("<cbc:PerUnitAmount currencyID=\"PEN\">0.50</cbc:PerUnitAmount>"));
    }
}
