package com.s1nt4xSystem.facturacion_sunat.infrastructure.sunat;

/**
 * Constantes con las URLs oficiales de los WebServices de SUNAT.
 * Permite centralizar los endpoints para los diferentes entornos (Beta, Homologación, Producción)
 * y tipos de comprobantes (Facturación, Guías de Remisión, Retenciones/Percepciones y Consulta CDR).
 */
public final class SunatEndpoints {

    private SunatEndpoints() {
        // Clase de constantes, prevenir instanciación
    }

    /**
     * FACTURACION ELECTRONICA (Factura, Boleta, Notas de Crédito y Débito)
     */
    public static final String FE_BETA = "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService";
    public static final String FE_HOMOLOGACION = "https://www.sunat.gob.pe/ol-ti-itcpgem-sqa/billService";
    public static final String FE_PRODUCCION = "https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService";
    public static final String FE_CONSULTA_CDR = "https://e-factura.sunat.gob.pe/ol-it-wsconscpegem/billConsultService";

    /**
     * GUIA DE REMISION
     * Nota: SUNAT recomienda el uso de su API REST para GRE, pero mantiene soporte SOAP tradicional.
     */
    public static final String GUIA_BETA = "https://e-beta.sunat.gob.pe/ol-ti-itemision-guia-gem-beta/billService";
    public static final String GUIA_PRODUCCION = "https://e-guiaremision.sunat.gob.pe/ol-ti-itemision-guia-gem/billService";

    /**
     * RETENCION Y PERCEPCION
     */
    public static final String RETENCION_BETA = "https://e-beta.sunat.gob.pe/ol-ti-itemision-otroscpe-gem-beta/billService";
    public static final String RETENCION_PRODUCCION = "https://e-factura.sunat.gob.pe/ol-ti-itemision-otroscpe-gem/billService";

    /**
     * WSDL Endpoint
     */
    public static final String WSDL_ENDPOINT = "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService?wsdl";
}
