package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.LineaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoAfectacionIgv;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TotalesAfectacion;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service.CalculadoraFiscalSunat.CalculoLineaInput;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service.CalculadoraFiscalSunat.ResultadoCalculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraFiscalSunatTest {

    private CalculadoraFiscalSunat calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new CalculadoraFiscalSunat();
    }

    @Test
    @DisplayName("Debe calcular correctamente una factura con un ítem gravado al 18%")
    void testCalculoLineaGravadaSimple() {
        CalculoLineaInput input = new CalculoLineaInput(
                1,
                "PROD-001",
                "Laptop HP Pavilion",
                new BigDecimal("2"),
                "NIU",
                new BigDecimal("1000.00"), // Base = 2000.00
                null,
                TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA,
                BigDecimal.ZERO,
                null
        );

        ResultadoCalculo resultado = calculadora.calcular(List.of(input), BigDecimal.ZERO);

        assertNotNull(resultado);
        assertEquals(1, resultado.lineas().size());

        LineaComprobante linea = resultado.lineas().get(0);
        assertEquals(new BigDecimal("2000.00"), linea.getSubtotal());
        assertEquals(new BigDecimal("360.00"), linea.getTotalTributos());
        assertEquals(new BigDecimal("2360.00"), linea.getTotal());
        assertEquals(new BigDecimal("1180.00"), linea.getPrecioUnitario());

        // Verificación de totales globales
        assertEquals(new BigDecimal("2000.00"), resultado.totalValorVenta());
        assertEquals(new BigDecimal("360.00"), resultado.totalTributos());
        assertEquals(new BigDecimal("2360.00"), resultado.importeTotal());

        // Verificación de resumen por afectación
        TotalesAfectacion gravado = resultado.totalesAfectacion().stream()
                .filter(t -> TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA.getCodigo().equals(t.getTipoAfectacionCodigo()))
                .findFirst()
                .orElse(null);
        assertNotNull(gravado);
        assertEquals(new BigDecimal("2000.00"), gravado.getBaseImponible());
        assertEquals(new BigDecimal("360.00"), gravado.getMontoTributo());
        assertEquals(new BigDecimal("2360.00"), gravado.getMontoTotal());
    }

    @Test
    @DisplayName("Debe calcular correctamente con ítem exonerado e inafecto (sin IGV)")
    void testCalculoExoneradoEInafecto() {
        CalculoLineaInput itemExonerado = new CalculoLineaInput(
                1,
                "LIB-01",
                "Libro de Programación Java",
                new BigDecimal("1"),
                "NIU",
                new BigDecimal("150.00"),
                null,
                TipoAfectacionIgv.EXONERADO_OPERACION_ONEROSA,
                BigDecimal.ZERO,
                null
        );

        CalculoLineaInput itemInafecto = new CalculoLineaInput(
                2,
                "SERV-01",
                "Servicio Educativo Inafecto",
                new BigDecimal("1"),
                "ZZ",
                new BigDecimal("300.00"),
                null,
                TipoAfectacionIgv.INAFECTO_OPERACION_ONEROSA,
                BigDecimal.ZERO,
                null
        );

        ResultadoCalculo resultado = calculadora.calcular(List.of(itemExonerado, itemInafecto), BigDecimal.ZERO);

        LineaComprobante linea1 = resultado.lineas().get(0);
        assertEquals(new BigDecimal("0.00"), linea1.getTotalTributos());
        assertEquals(new BigDecimal("150.00"), linea1.getTotal());

        LineaComprobante linea2 = resultado.lineas().get(1);
        assertEquals(new BigDecimal("0.00"), linea2.getTotalTributos());
        assertEquals(new BigDecimal("300.00"), linea2.getTotal());

        assertEquals(new BigDecimal("450.00"), resultado.totalValorVenta());
        assertEquals(new BigDecimal("0.00"), resultado.totalTributos());
        assertEquals(new BigDecimal("450.00"), resultado.importeTotal());
    }

    @Test
    @DisplayName("Ítems gratuitos no deben sumar al total a pagar del comprobante")
    void testCalculoItemGratuito() {
        CalculoLineaInput itemVenta = new CalculoLineaInput(
                1,
                "TEL-01",
                "Smartphone",
                new BigDecimal("1"),
                "NIU",
                new BigDecimal("500.00"),
                null,
                TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA,
                BigDecimal.ZERO,
                null
        );

        CalculoLineaInput itemRegalo = new CalculoLineaInput(
                2,
                "REG-01",
                "Funda de regalo",
                new BigDecimal("1"),
                "NIU",
                new BigDecimal("30.00"),
                null,
                TipoAfectacionIgv.GRAVADO_RETIRO_PREMIO, // Gratuita
                BigDecimal.ZERO,
                null
        );

        ResultadoCalculo resultado = calculadora.calcular(List.of(itemVenta, itemRegalo), BigDecimal.ZERO);

        // Venta onerosa: Base 500.00 + IGV 90.00 = 590.00
        // Regalo gratuito: Base 30.00 + IGV 5.40 -> El total de la línea es 0 a cobrar
        assertEquals(new BigDecimal("500.00"), resultado.totalValorVenta());
        assertEquals(new BigDecimal("590.00"), resultado.importeTotal());

        // Debe registrarse en el resumen de afectaciones la gratuita
        TotalesAfectacion gratis = resultado.totalesAfectacion().stream()
                .filter(t -> TipoAfectacionIgv.GRAVADO_RETIRO_PREMIO.getCodigo().equals(t.getTipoAfectacionCodigo()))
                .findFirst()
                .orElse(null);
        assertNotNull(gratis);
        assertEquals(new BigDecimal("30.00"), gratis.getBaseImponible());
    }

    @Test
    @DisplayName("Debe calcular correctamente el ICBPER para bolsas plásticas")
    void testCalculoIcbper() {
        CalculoLineaInput bolsa = new CalculoLineaInput(
                1,
                "BOLSA-01",
                "Bolsa plástica biodegradable",
                new BigDecimal("3"), // 3 bolsas x S/ 0.50 = S/ 1.50
                "NIU",
                new BigDecimal("0.10"),
                null,
                TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA,
                BigDecimal.ZERO,
                3
        );

        ResultadoCalculo resultado = calculadora.calcular(List.of(bolsa), BigDecimal.ZERO);

        LineaComprobante lineaBolsa = resultado.lineas().get(0);
        // Base = 0.30, IGV = 0.05, ICBPER = 1.50 -> Tributos = 1.55, Total = 1.85
        assertEquals(new BigDecimal("1.55"), lineaBolsa.getTotalTributos());
        assertEquals(new BigDecimal("1.85"), lineaBolsa.getTotal());
        assertEquals(new BigDecimal("1.85"), resultado.importeTotal());
        assertEquals(new BigDecimal("1.55"), resultado.totalTributos());
    }

    @Test
    @DisplayName("Debe calcular correctamente comprobante con tasa IGV 0% (Régimen especial Amazonía)")
    void testCalculoTasaAmazoniaCeroPorciento() {
        CalculoLineaInput itemAmazonia = new CalculoLineaInput(
                1,
                "MAD-01",
                "Madera Cedro de la Selva",
                new BigDecimal("5"),
                "NIU",
                new BigDecimal("200.00"), // Subtotal = 1000.00
                null,
                TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA,
                BigDecimal.ZERO,
                null
        );

        CalculadoraFiscalSunat.ConfiguracionTasas configAmazonia = 
                new CalculadoraFiscalSunat.ConfiguracionTasas(BigDecimal.ZERO, new BigDecimal("0.50"));

        ResultadoCalculo resultado = calculadora.calcular(List.of(itemAmazonia), BigDecimal.ZERO, configAmazonia);

        LineaComprobante linea = resultado.lineas().get(0);
        assertEquals(new BigDecimal("1000.00"), linea.getSubtotal());
        assertEquals(new BigDecimal("0.00"), linea.getTotalTributos());
        assertEquals(new BigDecimal("1000.00"), linea.getTotal());
        assertEquals(new BigDecimal("1000.00"), resultado.totalValorVenta());
        assertEquals(new BigDecimal("0.00"), resultado.totalTributos());
        assertEquals(new BigDecimal("1000.00"), resultado.importeTotal());
    }

    @Test
    @DisplayName("Debe calcular correctamente con tasas dinámicas personalizadas (ej. IGV 10% e ICBPER 0.60)")
    void testCalculoTasasDinamicasPersonalizadas() {
        CalculoLineaInput producto = new CalculoLineaInput(
                1,
                "MENU-01",
                "Almuerzo Ejecutivo Restaurante",
                new BigDecimal("2"),
                "NIU",
                new BigDecimal("50.00"), // Base = 100.00
                null,
                TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA,
                BigDecimal.ZERO,
                2 // 2 bolsas x 0.60 = 1.20
        );

        CalculadoraFiscalSunat.ConfiguracionTasas tasasPersonalizadas = 
                new CalculadoraFiscalSunat.ConfiguracionTasas(new BigDecimal("10.00"), new BigDecimal("0.60"));

        ResultadoCalculo resultado = calculadora.calcular(List.of(producto), BigDecimal.ZERO, tasasPersonalizadas);

        LineaComprobante linea = resultado.lineas().get(0);
        // Base = 100.00, IGV (10%) = 10.00, ICBPER (2 x 0.60) = 1.20 -> Tributos = 11.20, Total = 111.20
        assertEquals(new BigDecimal("100.00"), linea.getSubtotal());
        assertEquals(new BigDecimal("11.20"), linea.getTotalTributos());
        assertEquals(new BigDecimal("111.20"), linea.getTotal());
        assertEquals(new BigDecimal("111.20"), resultado.importeTotal());
        assertEquals(new BigDecimal("11.20"), resultado.totalTributos());
    }
}
