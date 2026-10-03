package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.CrearComprobanteRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.CrearNotaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ComprobanteJsonDeserializationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("Debe deserializar JSON de Factura de Postman con items anidados y codigo opcional")
    void testDeserializarFacturaPostman() throws Exception {
        String jsonPostman = """
        {
          "apikey": "empresa-key-secret",
          "ruc": "20000000001",
          "comprobante": {
            "tipo_documento": "01",
            "serie": "F001",
            "numero": 1,
            "fecha_emision": "2026-09-24",
            "hora_emision": "10:00:00",
            "forma_pago": "CONTADO",
            "moneda": "PEN",
            "cliente": {
              "tipo_documento": "6",
              "numero_documento": "20555666777",
              "razon_social": "INVERSIONES TECNOLOGICAS DEL PERU S.A.C.",
              "direccion": "Av. Las Begonias 450, San Isidro, Lima"
            },
            "items": [
              {
                "codigo": "PROD-01",
                "descripcion": "Laptop Dell XPS 15",
                "cantidad": 1,
                "unidad_medida": "NIU",
                "valor_unitario": 1000.00,
                "tipo_afectacion_igv": "10"
              },
              {
                "descripcion": "Servicio de instalacion sin codigo",
                "cantidad": 1,
                "unidad_medida": "ZZ",
                "valor_unitario": 200.00,
                "tipo_afectacion_igv": "10"
              }
            ]
          }
        }
        """;

        CrearComprobanteRequest request = mapper.readValue(jsonPostman, CrearComprobanteRequest.class);

        assertNotNull(request);
        assertNotNull(request.getComprobante());
        assertEquals("01", request.getComprobante().getTipo());
        assertEquals("F001", request.getComprobante().getSerie());
        assertEquals(1, request.getComprobante().getNumero());
        assertNotNull(request.getComprobante().getFechaEmision());

        // Verificar que el cliente anidado se resuelve
        assertNotNull(request.getCliente());
        assertEquals("6", request.getCliente().getTipoDocumento());
        assertEquals("20555666777", request.getCliente().getNumeroDocumento());
        assertEquals("INVERSIONES TECNOLOGICAS DEL PERU S.A.C.", request.getCliente().getNombreRazonSocial());

        // Verificar que los items anidados se resuelven
        assertNotNull(request.getItems());
        assertEquals(2, request.getItems().size());
        assertEquals("PROD-01", request.getItems().get(0).getCodigoProducto());
        assertEquals(new BigDecimal("1000.00"), request.getItems().get(0).getValor());
        assertEquals("10", request.getItems().get(0).getCodigoAfectacionSunat());

        // Item 2 sin codigo
        assertNull(request.getItems().get(1).getCodigoProducto());
        assertEquals("Servicio de instalacion sin codigo", request.getItems().get(1).getDescripcion());
        assertEquals(new BigDecimal("200.00"), request.getItems().get(1).getValor());
    }

    @Test
    @DisplayName("Debe deserializar Factura al Credito con cuotas anidadas")
    void testDeserializarFacturaCreditoPostman() throws Exception {
        String jsonCredito = """
        {
          "apikey": "empresa-key",
          "ruc": "20000000001",
          "comprobante": {
            "tipo_documento": "01",
            "serie": "F001",
            "numero": 2,
            "forma_pago": "CREDITO",
            "cliente": {
              "tipo_documento": "6",
              "numero_documento": "20123456789",
              "razon_social": "DISTRIBUIDORA LIMA NORTE S.A."
            },
            "items": [
              {
                "descripcion": "Servidor Cloud",
                "cantidad": 1,
                "valor_unitario": 5000.00,
                "tipo_afectacion_igv": "10"
              }
            ],
            "cuotas": [
              {
                "numero_cuota": 1,
                "monto": 2950.00,
                "fecha_vencimiento": "2026-10-24"
              },
              {
                "numero_cuota": 2,
                "monto": 2950.00,
                "fecha_vencimiento": "2026-11-24"
              }
            ]
          }
        }
        """;

        CrearComprobanteRequest request = mapper.readValue(jsonCredito, CrearComprobanteRequest.class);

        assertNotNull(request.getCuotas());
        assertEquals(2, request.getCuotas().size());
        assertEquals(1, request.getCuotas().get(0).getNumero());
        assertEquals(new BigDecimal("2950.00"), request.getCuotas().get(0).getMonto());
    }

    @Test
    @DisplayName("Debe deserializar Factura con Detraccion anidada")
    void testDeserializarFacturaDetraccionPostman() throws Exception {
        String jsonDetraccion = """
        {
          "comprobante": {
            "tipo_documento": "01",
            "serie": "F001",
            "cliente": {
              "tipo_documento": "6",
              "numero_documento": "20444555666",
              "razon_social": "TRANSPORTES PERU"
            },
            "items": [
              {
                "descripcion": "Flete Nacional",
                "cantidad": 1,
                "valor_unitario": 2000.00,
                "tipo_afectacion_igv": "10"
              }
            ],
            "detraccion": {
              "codigo_bien_servicio": "027",
              "porcentaje": 4.00,
              "monto": 94.40,
              "medio_pago": "001",
              "cuenta_banco_nacion": "00-041-123456"
            }
          }
        }
        """;

        CrearComprobanteRequest request = mapper.readValue(jsonDetraccion, CrearComprobanteRequest.class);

        assertNotNull(request.getDetraccion());
        assertEquals("027", request.getDetraccion().getCodigoBienServicio());
        assertEquals(new BigDecimal("4.00"), request.getDetraccion().getPorcentaje());
        assertEquals(new BigDecimal("94.40"), request.getDetraccion().getMontoDetraccion());
        assertEquals("00-041-123456", request.getDetraccion().getCuentaBancoNacion());
    }

    @Test
    @DisplayName("Debe deserializar Nota de Credito con referencia anidada")
    void testDeserializarNotaPostman() throws Exception {
        String jsonNota = """
        {
          "apikey": "secret",
          "ruc": "20000000001",
          "nota": {
            "tipo_documento": "07",
            "serie": "FC01",
            "numero": 1,
            "fecha_emision": "2026-09-24",
            "hora_emision": "12:00:00",
            "documento_referenciado_id": "ce0ca940-1666-4843-81a5-5c00810ee2ee",
            "motivo_codigo": "01",
            "motivo_descripcion": "Anulacion de la operacion"
          }
        }
        """;

        CrearNotaRequest request = mapper.readValue(jsonNota, CrearNotaRequest.class);

        assertNotNull(request.getNota());
        assertEquals("07", request.getNota().getTipo());
        assertEquals("FC01", request.getNota().getSerie());
        assertNotNull(request.getDocumentoReferencia());
        assertEquals(java.util.UUID.fromString("ce0ca940-1666-4843-81a5-5c00810ee2ee"),
                request.getDocumentoReferencia().getDocumentoReferenciadoId());
    }

    @Test
    @DisplayName("Debe serializar TotalesResponse con calculos completos y omitir total_isc y total_icbper si no tienen valor")
    void testSerializarTotalesSinIscNiIcbper() throws Exception {
        com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal doc =
                com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal.builder()
                .id(java.util.UUID.randomUUID())
                .tipoComprobante(com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoComprobante.FACTURA)
                .serie("F001")
                .numero(1)
                .fechaEmision(java.time.LocalDateTime.now())
                .subtotal(new BigDecimal("1000.00"))
                .totalTributos(new BigDecimal("180.00"))
                .total(new BigDecimal("1180.00"))
                .detalles(java.util.List.of(
                        com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.LineaComprobante.builder()
                                .item(1)
                                .descripcion("Laptop")
                                .cantidad(new BigDecimal("1"))
                                .valorUnitario(new BigDecimal("1000.00"))
                                .subtotal(new BigDecimal("1000.00"))
                                .totalTributos(new BigDecimal("180.00"))
                                .total(new BigDecimal("1180.00"))
                                .tipoAfectacion(com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                                .tributos(java.util.List.of(
                                        com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TributoLinea.builder().codigoTributo("1000").monto(new BigDecimal("180.00")).build()
                                ))
                                .build()
                ))
                .build();

        var resp = com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.mapper.ComprobanteRestMapper.toComprobanteResponse(doc, "20000000001", "0000");

        assertNotNull(resp.getTotales());
        assertEquals(new BigDecimal("1000.00"), resp.getTotales().getTotalGravada());
        assertEquals(new BigDecimal("180.00"), resp.getTotales().getTotalIgv());
        assertEquals(new BigDecimal("1180.00"), resp.getTotales().getTotalPagado());
        assertEquals(new BigDecimal("1180.00"), resp.getTotales().getTotal());
        assertNull(resp.getTotales().getTotalIsc());
        assertNull(resp.getTotales().getTotalIcbper());

        String json = mapper.writeValueAsString(resp);
        assertFalse(json.contains("total_isc"));
        assertFalse(json.contains("total_icbper"));
        assertTrue(json.contains("total_pagado"));
        assertTrue(json.contains("total_gravada"));
        assertTrue(json.contains("total_igv"));
    }

    @Test
    @DisplayName("Debe serializar total_icbper solo cuando el comprobante tenga bolsas gravadas")
    void testSerializarTotalesConIcbper() throws Exception {
        com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal doc =
                com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal.builder()
                .id(java.util.UUID.randomUUID())
                .tipoComprobante(com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoComprobante.BOLETA)
                .serie("B001")
                .numero(1)
                .fechaEmision(java.time.LocalDateTime.now())
                .subtotal(new BigDecimal("10.00"))
                .totalTributos(new BigDecimal("2.30"))
                .total(new BigDecimal("12.30"))
                .detalles(java.util.List.of(
                        com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.LineaComprobante.builder()
                                .item(1)
                                .descripcion("Gaseosa")
                                .cantidad(new BigDecimal("1"))
                                .valorUnitario(new BigDecimal("10.00"))
                                .subtotal(new BigDecimal("10.00"))
                                .totalTributos(new BigDecimal("2.30"))
                                .total(new BigDecimal("12.30"))
                                .tipoAfectacion(com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                                .tributos(java.util.List.of(
                                        com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TributoLinea.builder().codigoTributo("1000").monto(new BigDecimal("1.80")).build(),
                                        com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TributoLinea.builder().codigoTributo("7152").monto(new BigDecimal("0.50")).build()
                                ))
                                .build()
                ))
                .build();

        var resp = com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.mapper.ComprobanteRestMapper.toComprobanteResponse(doc, "20000000001", "0000");

        assertNotNull(resp.getTotales());
        assertEquals(new BigDecimal("0.50"), resp.getTotales().getTotalIcbper());
        assertNull(resp.getTotales().getTotalIsc());

        String json = mapper.writeValueAsString(resp);
        assertTrue(json.contains("\"total_icbper\":0.50"));
        assertFalse(json.contains("total_isc"));
    }

    @Test
    void debeMapearCodigoSucursalCorrectamenteDesdeComprobanteFiscal() {
        com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal doc =
                com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal.builder()
                .id(java.util.UUID.randomUUID())
                .codigoSucursal("0001")
                .tipoComprobante(com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoComprobante.BOLETA)
                .serie("B002")
                .numero(1)
                .fechaEmision(java.time.LocalDateTime.now())
                .subtotal(new BigDecimal("100.00"))
                .totalTributos(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .detalles(java.util.List.of())
                .build();

        var resp = com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.mapper.ComprobanteRestMapper.toComprobanteResponse(doc, "20000000001");

        assertNotNull(resp.getEmisor());
        assertEquals("20000000001", resp.getEmisor().getRuc());
        assertEquals("0001", resp.getEmisor().getCodigoSucursal());
    }

    @Test
    @DisplayName("Debe aceptar datos adicionales/arbitrarios en el JSON sin fallar y permitir almacenar rawPayload")
    void testAceptarDatosAdicionalesYGuardarRawPayload() throws Exception {
        String jsonConCamposExtras = """
        {
          "vendedor": "Juan Pérez",
          "mesa": 14,
          "mozo": "Carlos M.",
          "observacion_personalizada": "Entregar en puerta posterior",
          "datos_impresion_ticket": {
            "pie_de_pagina": "¡Gracias por su preferencia!",
            "wifi_clave": "clientes2026"
          },
          "comprobante": {
            "tipo": "01",
            "serie": "F001",
            "campo_custom_en_comprobante": 999,
            "cliente": {
              "tipo_documento": "6",
              "numero_documento": "20555666777",
              "razon_social": "INVERSIONES TECNOLOGICAS DEL PERU S.A.C.",
              "celular_contacto": "999888777"
            },
            "items": [
              {
                "codigo": "PROD-01",
                "descripcion": "Laptop Dell XPS 15",
                "cantidad": 1,
                "unidad_medida": "NIU",
                "valor_unitario": 1000.00,
                "tipo_afectacion_igv": "10",
                "color": "Plata espacial",
                "garantia_meses": 24
              }
            ]
          }
        }
        """;

        CrearComprobanteRequest request = mapper.readValue(jsonConCamposExtras, CrearComprobanteRequest.class);
        request.setRawPayload(jsonConCamposExtras);

        assertNotNull(request);
        assertEquals("01", request.getComprobante().getTipo());
        assertEquals("F001", request.getComprobante().getSerie());
        assertNull(request.getComprobante().getNumero()); // Número no vino en el JSON, debe ser null para autogeneración
        assertEquals("20555666777", request.getCliente().getNumeroDocumento());
        assertEquals(1, request.getItems().size());
        assertEquals("PROD-01", request.getItems().get(0).getCodigoProducto());
        assertEquals(jsonConCamposExtras, request.getRawPayload());
    }
}

