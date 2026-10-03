package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out;

import java.util.UUID;

public interface CorrelativoServicePort {
    /**
     * Obtiene e incrementa de forma atómica y segura contra concurrencia el correlativo de una serie.
     *
     * @param sucursalId Identificador de la sucursal
     * @param tipoComprobante Código de tipo de comprobante (01, 03, 07, 08)
     * @param serie Serie alfanumérica (ej. F001, B001)
     * @return El número de correlativo asignado
     */
    Integer obtenerSiguienteCorrelativo(UUID sucursalId, String tipoComprobante, String serie);
}
