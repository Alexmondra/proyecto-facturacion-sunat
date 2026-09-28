package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.LineaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoAfectacionIgv;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TotalesAfectacion;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TributoLinea;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Servicio de dominio puro para el cálculo matemático oficial de tributos SUNAT.
 * Desacoplado de base de datos y frameworks.
 */
public class CalculadoraFiscalSunat {

    public static final BigDecimal TASA_IGV_PORCENTAJE = new BigDecimal("18.00");
    public static final BigDecimal FACTOR_IGV = new BigDecimal("0.18");
    public static final BigDecimal MONTO_UNITARIO_ICBPER = new BigDecimal("0.50"); // 50 centavos por bolsa
    public static final int ESCALA_MONEDA = 2;
    public static final int ESCALA_CALCULO = 4;

    public record ConfiguracionTasas(
            BigDecimal tasaIgvPorcentaje,
            BigDecimal montoUnitarioIcbper
    ) {
        public static ConfiguracionTasas porDefecto() {
            return new ConfiguracionTasas(TASA_IGV_PORCENTAJE, MONTO_UNITARIO_ICBPER);
        }
    }

    public record CalculoLineaInput(
            Integer item,
            String codigoProducto,
            String descripcion,
            BigDecimal cantidad,
            String unidadMedida,
            BigDecimal valorUnitario,      // Precio sin IGV
            BigDecimal precioUnitario,     // Precio con IGV (opcional si se provee valorUnitario)
            TipoAfectacionIgv tipoAfectacion,
            BigDecimal descuento,          // Descuento por ítem
            Integer cantidadBolsasIcbper   // Cantidad de bolsas plásticas gravadas con ICBPER
    ) {}

    public record ResultadoCalculo(
            List<LineaComprobante> lineas,
            List<TotalesAfectacion> totalesAfectacion,
            List<TributoLinea> tributosGlobales,
            BigDecimal totalValorVenta,    // Subtotal sin tributos
            BigDecimal totalTributos,      // Suma de IGV + ICBPER
            BigDecimal totalDescuentos,
            BigDecimal importeTotal        // Monto final a pagar
    ) {}

    /**
     * Sobrecarga retrocompatible que utiliza las tasas generales por defecto.
     */
    public ResultadoCalculo calcular(List<CalculoLineaInput> inputs, BigDecimal descuentoGlobal) {
        return calcular(inputs, descuentoGlobal, ConfiguracionTasas.porDefecto());
    }

    /**
     * Calcula los tributos, subtotales y totales para una lista de líneas utilizando tasas dinámicas.
     */
    public ResultadoCalculo calcular(List<CalculoLineaInput> inputs, BigDecimal descuentoGlobal, ConfiguracionTasas tasas) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("El comprobante debe tener al menos una línea de detalle");
        }

        ConfiguracionTasas config = tasas != null ? tasas : ConfiguracionTasas.porDefecto();
        BigDecimal tasaIgv = config.tasaIgvPorcentaje() != null ? config.tasaIgvPorcentaje() : TASA_IGV_PORCENTAJE;
        BigDecimal factorIgv = tasaIgv.divide(new BigDecimal("100"), ESCALA_CALCULO, RoundingMode.HALF_UP);
        BigDecimal montoUnitarioIcbper = config.montoUnitarioIcbper() != null ? config.montoUnitarioIcbper() : MONTO_UNITARIO_ICBPER;

        List<LineaComprobante> lineasCalculadas = new ArrayList<>();
        Map<TipoAfectacionIgv, BigDecimal> basesPorTipo = new EnumMap<>(TipoAfectacionIgv.class);
        Map<TipoAfectacionIgv, BigDecimal> tributosPorTipo = new EnumMap<>(TipoAfectacionIgv.class);

        BigDecimal sumaIgvGlobal = BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
        BigDecimal sumaIcbperGlobal = BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
        int sumaBolsasGlobal = 0;
        BigDecimal sumaSubtotalGlobal = BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
        BigDecimal sumaDescuentos = (descuentoGlobal != null ? descuentoGlobal : BigDecimal.ZERO).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);

        for (CalculoLineaInput input : inputs) {
            LineaCalculadaItem itemResult = calcularLinea(input, factorIgv, tasaIgv, montoUnitarioIcbper);
            lineasCalculadas.add(itemResult.linea());

            TipoAfectacionIgv afectacion = input.tipoAfectacion();
            BigDecimal baseLinea = itemResult.baseImponible();
            BigDecimal igvLinea = itemResult.montoIgv();
            BigDecimal icbperLinea = itemResult.montoIcbper();

            basesPorTipo.merge(afectacion, baseLinea, BigDecimal::add);
            tributosPorTipo.merge(afectacion, igvLinea, BigDecimal::add);

            if (!afectacion.isGratuito()) {
                sumaSubtotalGlobal = sumaSubtotalGlobal.add(baseLinea);
                sumaIgvGlobal = sumaIgvGlobal.add(igvLinea);
            }
            sumaIcbperGlobal = sumaIcbperGlobal.add(icbperLinea);
            if (input.cantidadBolsasIcbper() != null && input.cantidadBolsasIcbper() > 0) {
                sumaBolsasGlobal += input.cantidadBolsasIcbper();
            }

            if (input.descuento() != null && input.descuento().compareTo(BigDecimal.ZERO) > 0) {
                sumaDescuentos = sumaDescuentos.add(input.descuento());
            }
        }

        // Agrupación de totales por tipo de afectación SUNAT
        List<TotalesAfectacion> totalesAfectacion = new ArrayList<>();
        for (Map.Entry<TipoAfectacionIgv, BigDecimal> entry : basesPorTipo.entrySet()) {
            TipoAfectacionIgv tipo = entry.getKey();
            BigDecimal base = entry.getValue().setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
            BigDecimal tributo = tributosPorTipo.getOrDefault(tipo, BigDecimal.ZERO).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
            BigDecimal totalAfectacion = tipo.isGratuito() ? BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP) : base.add(tributo);

            String tipoTotalNombre = switch (tipo) {
                case GRAVADO_OPERACION_ONEROSA -> "GRAVADO";
                case EXONERADO_OPERACION_ONEROSA -> "EXONERADO";
                case INAFECTO_OPERACION_ONEROSA -> "INAFECTO";
                case EXPORTACION_BIENES_SERVICIOS -> "EXPORTACION";
                default -> tipo.isGratuito() ? "GRATUITO" : "OTROS";
            };

            totalesAfectacion.add(TotalesAfectacion.builder()
                    .tipoAfectacionCodigo(tipo.getCodigo())
                    .tipoTotal(tipoTotalNombre)
                    .baseImponible(base)
                    .montoTributo(tributo)
                    .montoTotal(totalAfectacion)
                    .build());
        }

        // Tributos globales
        List<TributoLinea> tributosGlobales = new ArrayList<>();
        if (sumaIgvGlobal.compareTo(BigDecimal.ZERO) > 0) {
            tributosGlobales.add(TributoLinea.builder()
                    .tributoId(1L)
                    .codigoTributo("1000")
                    .nombreTributo("IGV")
                    .tipoTributo("VAT")
                    .baseImponible(basesPorTipo.getOrDefault(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA, BigDecimal.ZERO).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .porcentaje(tasaIgv.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .monto(sumaIgvGlobal.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .build());
        }

        if (sumaIcbperGlobal.compareTo(BigDecimal.ZERO) > 0) {
            tributosGlobales.add(TributoLinea.builder()
                    .tributoId(3L)
                    .codigoTributo("7152")
                    .nombreTributo("ICBPER")
                    .tipoTributo("OTH")
                    .baseImponible(BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .porcentaje(BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .cantidadBase(BigDecimal.valueOf(sumaBolsasGlobal))
                    .monto(sumaIcbperGlobal.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .build());
        }

        BigDecimal totalTributosFinal = sumaIgvGlobal.add(sumaIcbperGlobal).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
        BigDecimal totalValorVentaFinal = sumaSubtotalGlobal.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
        BigDecimal importeTotalFinal = totalValorVentaFinal.add(totalTributosFinal).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);

        return new ResultadoCalculo(
                lineasCalculadas,
                totalesAfectacion,
                tributosGlobales,
                totalValorVentaFinal,
                totalTributosFinal,
                sumaDescuentos.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP),
                importeTotalFinal
        );
    }

    private record LineaCalculadaItem(
            LineaComprobante linea,
            BigDecimal baseImponible,
            BigDecimal montoIgv,
            BigDecimal montoIcbper
    ) {}

    private LineaCalculadaItem calcularLinea(CalculoLineaInput input, BigDecimal factorIgv, BigDecimal tasaIgv, BigDecimal montoUnitarioIcbper) {
        BigDecimal cantidad = input.cantidad();
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        BigDecimal valorUnitario = input.valorUnitario();
        BigDecimal precioUnitario = input.precioUnitario();

        TipoAfectacionIgv afectacion = input.tipoAfectacion() != null
                ? input.tipoAfectacion()
                : TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA;

        // Si se provee precio con IGV pero no valor sin IGV, se calcula hacia atrás
        if (valorUnitario == null && precioUnitario != null) {
            if (afectacion.isGravaIgv() && factorIgv.compareTo(BigDecimal.ZERO) > 0) {
                valorUnitario = precioUnitario.divide(BigDecimal.ONE.add(factorIgv), ESCALA_CALCULO, RoundingMode.HALF_UP);
            } else {
                valorUnitario = precioUnitario;
            }
        } else if (valorUnitario != null && precioUnitario == null) {
            if (afectacion.isGravaIgv() && factorIgv.compareTo(BigDecimal.ZERO) > 0) {
                precioUnitario = valorUnitario.multiply(BigDecimal.ONE.add(factorIgv)).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
            } else {
                precioUnitario = valorUnitario.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
            }
        }

        if (valorUnitario == null) {
            throw new IllegalArgumentException("Debe proporcionar al menos el valor unitario o precio unitario");
        }

        BigDecimal descuento = (input.descuento() != null ? input.descuento() : BigDecimal.ZERO).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
        BigDecimal baseImponibleBruta = cantidad.multiply(valorUnitario);
        BigDecimal baseImponible = baseImponibleBruta.subtract(descuento);
        if (baseImponible.compareTo(BigDecimal.ZERO) < 0) {
            baseImponible = BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
        }

        List<TributoLinea> tributosLinea = new ArrayList<>();
        BigDecimal montoIgv = BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);

        // 1. Cálculo del IGV si corresponde
        if (afectacion.isGravaIgv()) {
            montoIgv = baseImponible.multiply(factorIgv).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
            tributosLinea.add(TributoLinea.builder()
                    .tributoId(1L)
                    .codigoTributo(afectacion.getCodigoTributoSunat())
                    .nombreTributo(afectacion.getNombreTributoSunat())
                    .tipoTributo(afectacion.getTipoTributoSunat())
                    .baseImponible(baseImponible.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .porcentaje(tasaIgv.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .monto(montoIgv)
                    .build());
        } else {
            // Exonerado o Inafecto
            tributosLinea.add(TributoLinea.builder()
                    .tributoId(1L)
                    .codigoTributo(afectacion.getCodigoTributoSunat())
                    .nombreTributo(afectacion.getNombreTributoSunat())
                    .tipoTributo(afectacion.getTipoTributoSunat())
                    .baseImponible(baseImponible.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .porcentaje(BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .monto(BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .build());
        }

        // 2. Cálculo de ICBPER si aplica (Bolsas plásticas)
        BigDecimal montoIcbper = BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
        if (input.cantidadBolsasIcbper() != null && input.cantidadBolsasIcbper() > 0) {
            montoIcbper = montoUnitarioIcbper.multiply(BigDecimal.valueOf(input.cantidadBolsasIcbper()))
                    .setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
            tributosLinea.add(TributoLinea.builder()
                    .tributoId(3L)
                    .codigoTributo("7152")
                    .nombreTributo("ICBPER")
                    .tipoTributo("OTH")
                    .baseImponible(BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .porcentaje(BigDecimal.ZERO.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP))
                    .cantidadBase(BigDecimal.valueOf(input.cantidadBolsasIcbper()))
                    .monto(montoIcbper)
                    .build());
        }

        BigDecimal totalTributosLinea = montoIgv.add(montoIcbper).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);
        BigDecimal subtotalLinea = baseImponible.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);

        // En transferencias gratuitas, el total a cobrar de la línea es 0 (o el ICBPER si grava bolsas)
        BigDecimal totalLinea = afectacion.isGratuito()
                ? montoIcbper
                : subtotalLinea.add(totalTributosLinea).setScale(ESCALA_MONEDA, RoundingMode.HALF_UP);

        LineaComprobante linea = LineaComprobante.builder()
                .item(input.item())
                .codigoProducto(input.codigoProducto())
                .descripcion(input.descripcion())
                .cantidad(cantidad)
                .unidadMedida(input.unidadMedida() != null ? input.unidadMedida() : "NIU")
                .valorUnitario(valorUnitario.setScale(ESCALA_CALCULO, RoundingMode.HALF_UP))
                .precioUnitario(precioUnitario != null ? precioUnitario.setScale(ESCALA_MONEDA, RoundingMode.HALF_UP) : null)
                .tipoAfectacion(afectacion)
                .descuento(descuento)
                .subtotal(subtotalLinea)
                .totalTributos(totalTributosLinea)
                .total(totalLinea)
                .tributos(tributosLinea)
                .build();

        return new LineaCalculadaItem(linea, subtotalLinea, montoIgv, montoIcbper);
    }
}
