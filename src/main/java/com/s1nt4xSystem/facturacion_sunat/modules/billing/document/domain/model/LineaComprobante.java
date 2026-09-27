package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class LineaComprobante {

    private final Integer item;
    private final String codigoProducto;
    private final String descripcion;
    private final BigDecimal cantidad;
    private final String unidadMedida; // "NIU", "ZZ", etc.
    private final BigDecimal valorUnitario; // Sin tributos
    private final BigDecimal precioUnitario; // Con tributos
    private final TipoAfectacionIgv tipoAfectacion;
    private final BigDecimal descuento; // Descuento por ítem

    // Cálculos resultantes del motor fiscal
    private final BigDecimal subtotal; // cantidad * valorUnitario (- descuento si aplica)
    private final BigDecimal totalTributos; // Suma de IGV + ICBPER
    private final BigDecimal total; // subtotal + totalTributos

    @Builder.Default
    private final List<TributoLinea> tributos = new ArrayList<>();
}
