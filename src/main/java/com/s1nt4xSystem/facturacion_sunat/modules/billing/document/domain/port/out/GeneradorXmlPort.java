package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;

public interface GeneradorXmlPort {
    /**
     * Genera el documento XML UBL 2.1 estándar de SUNAT a partir del agregado ComprobanteFiscal.
     *
     * @param comprobante Datos del comprobante
     * @param empresa Datos de la empresa emisora
     * @param sucursal Datos de la sucursal emisora
     * @return Arreglo de bytes del XML UBL 2.1 sin firmar
     */
    byte[] generarXml(ComprobanteFiscal comprobante, Empresa empresa, Sucursal sucursal);
}
