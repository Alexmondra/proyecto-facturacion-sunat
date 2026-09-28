package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ResultadoFirma;

public interface FirmaDigitalPort {
    /**
     * Firma digitalmente un documento XML UBL 2.1 con el certificado digital de la empresa.
     * Inserta el nodo ds:Signature en ext:UBLExtensions y extrae el DigestValue (Hash CPE).
     *
     * @param xmlBytes Arreglo de bytes del XML original
     * @param rutaCertificado Ruta relativa al certificado digital almacenado (.pfx)
     * @param passwordCertificado Contraseña para descifrar la clave privada
     * @return Objeto con el XML firmado y el Hash CPE calculado
     */
    ResultadoFirma firmarXml(byte[] xmlBytes, String rutaCertificado, String passwordCertificado);
}
