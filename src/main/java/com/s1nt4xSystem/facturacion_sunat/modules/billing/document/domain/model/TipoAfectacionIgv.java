package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model;

import lombok.Getter;

@Getter
public enum TipoAfectacionIgv {
    GRAVADO_OPERACION_ONEROSA("10", "Gravado - Operación Onerosa", true, false, "1000", "IGV", "VAT"),
    GRAVADO_RETIRO_PREMIO("11", "Gravado - Retiro por premio", true, true, "9996", "GRA", "FRE"),
    GRAVADO_RETIRO_DONACION("12", "Gravado - Retiro por donación", true, true, "9996", "GRA", "FRE"),
    GRAVADO_RETIRO("13", "Gravado - Retiro", true, true, "9996", "GRA", "FRE"),
    GRAVADO_RETIRO_PUBLICIDAD("14", "Gravado - Retiro por publicidad", true, true, "9996", "GRA", "FRE"),
    GRAVADO_BONIFICACIONES("15", "Gravado - Bonificaciones", true, true, "9996", "GRA", "FRE"),
    GRAVADO_RETIRO_TRABAJADORES("16", "Gravado - Retiro por entrega a trabajadores", true, true, "9996", "GRA", "FRE"),

    EXONERADO_OPERACION_ONEROSA("20", "Exonerado - Operación Onerosa", false, false, "9997", "EXO", "VAT"),
    EXONERADO_TRANSFERENCIA_GRATUITA("21", "Exonerado - Transferencia Gratuita", false, true, "9996", "GRA", "FRE"),

    INAFECTO_OPERACION_ONEROSA("30", "Inafecto - Operación Onerosa", false, false, "9998", "INA", "FRE"),
    INAFECTO_RETIRO_BONIFICACION("31", "Inafecto - Retiro por Bonificación", false, true, "9996", "GRA", "FRE"),
    INAFECTO_RETIRO("32", "Inafecto - Retiro", false, true, "9996", "GRA", "FRE"),
    INAFECTO_RETIRO_MUESTRAS_MEDICAS("33", "Inafecto - Retiro por Muestras Médicas", false, true, "9996", "GRA", "FRE"),
    INAFECTO_RETIRO_CONVENIO_COLECTIVO("34", "Inafecto - Retiro por Convenio Colectivo", false, true, "9996", "GRA", "FRE"),
    INAFECTO_RETIRO_PREMIO("35", "Inafecto - Retiro por Premio", false, true, "9996", "GRA", "FRE"),
    INAFECTO_RETIRO_PUBLICIDAD("36", "Inafecto - Retiro por Publicidad", false, true, "9996", "GRA", "FRE"),

    EXPORTACION_BIENES_SERVICIOS("40", "Exportación de Bienes o Servicios", false, false, "9995", "EXP", "FRE");

    private final String codigo;
    private final String descripcion;
    private final boolean gravaIgv;
    private final boolean gratuito;
    private final String codigoTributoSunat;
    private final String nombreTributoSunat;
    private final String tipoTributoSunat;

    TipoAfectacionIgv(String codigo, String descripcion, boolean gravaIgv, boolean gratuito,
                     String codigoTributoSunat, String nombreTributoSunat, String tipoTributoSunat) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.gravaIgv = gravaIgv;
        this.gratuito = gratuito;
        this.codigoTributoSunat = codigoTributoSunat;
        this.nombreTributoSunat = nombreTributoSunat;
        this.tipoTributoSunat = tipoTributoSunat;
    }

    public static TipoAfectacionIgv fromCodigo(String codigo) {
        for (TipoAfectacionIgv tipo : values()) {
            if (tipo.getCodigo().equalsIgnoreCase(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de afectación al IGV no soportado: " + codigo);
    }
}
