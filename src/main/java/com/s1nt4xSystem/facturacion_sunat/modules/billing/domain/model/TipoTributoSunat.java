package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import lombok.Getter;

/**
 * Catálogo 05 de SUNAT: Códigos oficiales de tipos de tributos.
 * Centraliza los códigos inmutables definidos por la normativa tributaria de SUNAT
 * (coincidente con los registros de la tabla catalogo_tributos).
 */
@Getter
public enum TipoTributoSunat {

    IGV("1000", "IGV", "VAT", "IMPUESTO GENERAL A LAS VENTAS"),
    ISC("2000", "ISC", "EXC", "IMPUESTO SELECTIVO AL CONSUMO"),
    ICBPER("7152", "ICBPER", "OTH", "IMPUESTO AL CONSUMO DE BOLSAS PLASTICAS"),
    EXPORTACION("9995", "EXP", "FRE", "EXPORTACION DE BIENES Y SERVICIOS"),
    GRATUITO("9996", "GRA", "FRE", "TRANSFERENCIA GRATUITA"),
    EXONERADO("9997", "EXO", "VAT", "OPERACION EXONERADA DEL IGV"),
    INAFECTO("9998", "INA", "FRE", "OPERACION INAFECTA DEL IGV"),
    OTROS("9999", "OTROS", "OTH", "OTROS CONCEPTOS DE PAGO");

    public static final String CODIGO_IGV = "1000";
    public static final String CODIGO_ISC = "2000";
    public static final String CODIGO_ICBPER = "7152";
    public static final String CODIGO_EXPORTACION = "9995";
    public static final String CODIGO_GRATUITO = "9996";
    public static final String CODIGO_EXONERADO = "9997";
    public static final String CODIGO_INAFECTO = "9998";
    public static final String CODIGO_OTROS = "9999";

    private final String codigo;
    private final String nombre;
    private final String tipo;
    private final String descripcion;

    TipoTributoSunat(String codigo, String nombre, String tipo, String descripcion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    public static TipoTributoSunat fromCodigo(String codigo) {
        if (codigo == null) return null;
        for (TipoTributoSunat t : values()) {
            if (t.codigo.equalsIgnoreCase(codigo.trim())) {
                return t;
            }
        }
        return null;
    }

    public boolean coincideCon(String codigoTributo) {
        return this.codigo.equalsIgnoreCase(codigoTributo);
    }
}
