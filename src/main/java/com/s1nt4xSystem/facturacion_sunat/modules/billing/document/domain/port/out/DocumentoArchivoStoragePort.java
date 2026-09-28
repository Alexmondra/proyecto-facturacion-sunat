package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out;

public interface DocumentoArchivoStoragePort {
    /**
     * Guarda físicamente un archivo en el almacenamiento del tenant.
     *
     * @param ruc RUC del tenant emisor
     * @param subcarpeta Subcarpeta de destino (ej: "xml", "cdr")
     * @param nombreArchivo Nombre del archivo con su extensión (ej: 20600000001-01-F001-1.xml)
     * @param contenido Contenido binario del archivo
     * @return Ruta relativa donde se almacenó el archivo
     */
    String guardarArchivo(String ruc, String subcarpeta, String nombreArchivo, byte[] contenido);

    /**
     * Lee el contenido binario de un archivo a partir de su ruta relativa.
     */
    byte[] leerArchivo(String rutaRelativa);
}
