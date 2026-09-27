package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model;

import lombok.Getter;

@Getter
public enum TipoComprobante {
    FACTURA("01", "Factura Electrónica", "F"),
    BOLETA("03", "Boleta de Venta Electrónica", "B"),
    NOTA_CREDITO("07", "Nota de Crédito Electrónica", "F"), // También puede empezar con B
    NOTA_DEBITO("08", "Nota de Débito Electrónica", "F"),
    GUIA_REMISION_REMITENTE("09", "Guía de Remisión Remitente", "T");

    private final String codigo;
    private final String descripcion;
    private final String prefijoSerieDefault;

    TipoComprobante(String codigo, String descripcion, String prefijoSerieDefault) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.prefijoSerieDefault = prefijoSerieDefault;
    }

    public static TipoComprobante fromCodigo(String codigo) {
        for (TipoComprobante tipo : values()) {
            if (tipo.getCodigo().equalsIgnoreCase(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de comprobante no soportado o inválido: " + codigo);
    }
}
