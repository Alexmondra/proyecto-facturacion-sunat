package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.SunatSoapResult;

public interface SunatSoapPort {
    /**
     * Empaqueta el XML firmado en un archivo ZIP y lo envía mediante el servicio SOAP 'sendBill' a SUNAT.
     * Recibe la respuesta, descomprime el CDR XML y extrae el estado y código de respuesta oficial.
     *
     * @param xmlFirmado Bytes del documento XML firmado
     * @param nombreArchivoBase Nombre base del comprobante (ej: 20600000001-01-F001-00000001)
     * @param empresa Datos de la empresa (RUC, entorno dev/prod)
     * @param config Configuración con credenciales SOL (user_sol, pass_sol)
     * @return Resultado con el estado de SUNAT y los bytes del archivo ZIP del CDR
     */
    SunatSoapResult enviarComprobante(byte[] xmlFirmado, String nombreArchivoBase, Empresa empresa, EmpresaConfig config);
}
