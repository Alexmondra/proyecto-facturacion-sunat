package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.exception.ReglaFiscalException;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.CuotaPago;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.DetraccionFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.LineaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ReferenciaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoAfectacionIgv;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoComprobante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorReglasFiscalesTest {

    private ValidadorReglasFiscales validador;

    @BeforeEach
    void setUp() {
        validador = new ValidadorReglasFiscales();
    }

    private LineaComprobante crearLineaValida() {
        return LineaComprobante.builder()
                .item(1)
                .descripcion("Consultoría de Software")
                .cantidad(new BigDecimal("1"))
                .valorUnitario(new BigDecimal("1000.00"))
                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                .build();
    }

    @Test
    @DisplayName("Factura válida con RUC de 11 dígitos y serie que inicia con F debe pasar")
    void testFacturaValida() {
        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20601234567")
                .clienteNombre("EMPRESA CLIENTE S.A.C.")
                .formaPago("CONTADO")
                .total(new BigDecimal("1180.00"))
                .detalles(List.of(crearLineaValida()))
                .build();

        assertDoesNotThrow(() -> validador.validar(doc));
    }

    @Test
    @DisplayName("Factura con documento que no es RUC debe fallar")
    void testFacturaConDniDebeFallar() {
        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("1") // DNI
                .clienteNumeroDoc("76543210")
                .clienteNombre("Juan Pérez")
                .formaPago("CONTADO")
                .total(new BigDecimal("1180.00"))
                .detalles(List.of(crearLineaValida()))
                .build();

        ReglaFiscalException ex = assertThrows(ReglaFiscalException.class, () -> validador.validar(doc));
        assertEquals("FACTURA_CLIENTE_TIPO_DOC_INVALIDO", ex.getCodigoError());
    }

    @Test
    @DisplayName("Factura con serie que no empieza con F debe fallar")
    void testFacturaSerieInvalida() {
        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("B001") // Serie de boleta
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20601234567")
                .clienteNombre("EMPRESA CLIENTE S.A.C.")
                .detalles(List.of(crearLineaValida()))
                .build();

        ReglaFiscalException ex = assertThrows(ReglaFiscalException.class, () -> validador.validar(doc));
        assertEquals("FACTURA_SERIE_INVALIDA", ex.getCodigoError());
    }

    @Test
    @DisplayName("Boleta >= 700 con cliente anónimo debe fallar")
    void testBoletaMayorA700AnonimaDebeFallar() {
        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.BOLETA)
                .serie("B001")
                .moneda("PEN")
                .clienteTipoDoc("0") // Sin documento
                .clienteNumeroDoc("00000000")
                .clienteNombre("CLIENTES VARIOS")
                .formaPago("CONTADO")
                .total(new BigDecimal("750.00")) // Supera 700
                .detalles(List.of(crearLineaValida()))
                .build();

        ReglaFiscalException ex = assertThrows(ReglaFiscalException.class, () -> validador.validar(doc));
        assertEquals("BOLETA_IDENTIFICACION_REQUERIDA", ex.getCodigoError());
    }

    @Test
    @DisplayName("Boleta < 700 con cliente sin documento sí debe pasar")
    void testBoletaMenorA700AnonimaDebePasar() {
        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.BOLETA)
                .serie("B001")
                .moneda("PEN")
                .clienteTipoDoc("0")
                .clienteNumeroDoc("00000000")
                .clienteNombre("CLIENTES VARIOS")
                .formaPago("CONTADO")
                .total(new BigDecimal("150.00"))
                .detalles(List.of(crearLineaValida()))
                .build();

        assertDoesNotThrow(() -> validador.validar(doc));
    }

    @Test
    @DisplayName("Venta a crédito sin cuotas debe fallar")
    void testVentaCreditoSinCuotasDebeFallar() {
        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20601234567")
                .clienteNombre("EMPRESA CLIENTE S.A.C.")
                .formaPago("CREDITO")
                .total(new BigDecimal("1000.00"))
                .detalles(List.of(crearLineaValida()))
                .cuotas(Collections.emptyList())
                .build();

        ReglaFiscalException ex = assertThrows(ReglaFiscalException.class, () -> validador.validar(doc));
        assertEquals("CREDITO_CUOTAS_REQUERIDAS", ex.getCodigoError());
    }

    @Test
    @DisplayName("Venta a crédito donde suma de cuotas no cuadra con el total debe fallar")
    void testVentaCreditoCuotasDescuadradasDebeFallar() {
        CuotaPago cuota1 = CuotaPago.builder()
                .numeroCuota(1)
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .monto(new BigDecimal("400.00"))
                .build();

        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20601234567")
                .clienteNombre("EMPRESA CLIENTE S.A.C.")
                .formaPago("CREDITO")
                .total(new BigDecimal("1000.00")) // Falta 600.00
                .detalles(List.of(crearLineaValida()))
                .cuotas(List.of(cuota1))
                .build();

        ReglaFiscalException ex = assertThrows(ReglaFiscalException.class, () -> validador.validar(doc));
        assertEquals("CREDITO_SUMA_CUOTAS_INVALIDA", ex.getCodigoError());
    }

    @Test
    @DisplayName("Venta a crédito con suma de cuotas exacta debe pasar")
    void testVentaCreditoCuotasExactasDebePasar() {
        CuotaPago cuota1 = CuotaPago.builder()
                .numeroCuota(1)
                .fechaVencimiento(LocalDate.now().plusDays(15))
                .monto(new BigDecimal("500.00"))
                .build();
        CuotaPago cuota2 = CuotaPago.builder()
                .numeroCuota(2)
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .monto(new BigDecimal("500.00"))
                .build();

        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20601234567")
                .clienteNombre("EMPRESA CLIENTE S.A.C.")
                .formaPago("CREDITO")
                .total(new BigDecimal("1000.00"))
                .detalles(List.of(crearLineaValida()))
                .cuotas(List.of(cuota1, cuota2))
                .build();

        assertDoesNotThrow(() -> validador.validar(doc));
    }

    @Test
    @DisplayName("Nota de crédito sin referencias debe fallar")
    void testNotaCreditoSinReferenciasDebeFallar() {
        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.NOTA_CREDITO)
                .serie("FC01")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20601234567")
                .clienteNombre("EMPRESA CLIENTE S.A.C.")
                .total(new BigDecimal("100.00"))
                .detalles(List.of(crearLineaValida()))
                .referencias(Collections.emptyList())
                .build();

        ReglaFiscalException ex = assertThrows(ReglaFiscalException.class, () -> validador.validar(doc));
        assertEquals("NOTA_REFERENCIA_REQUERIDA", ex.getCodigoError());
    }

    @Test
    @DisplayName("Comprobante sin líneas debe fallar")
    void testComprobanteSinLineasDebeFallar() {
        ComprobanteFiscal doc = ComprobanteFiscal.builder()
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20601234567")
                .clienteNombre("EMPRESA CLIENTE S.A.C.")
                .detalles(Collections.emptyList())
                .build();

        ReglaFiscalException ex = assertThrows(ReglaFiscalException.class, () -> validador.validar(doc));
        assertEquals("LINEAS_VACIAS", ex.getCodigoError());
    }
}
