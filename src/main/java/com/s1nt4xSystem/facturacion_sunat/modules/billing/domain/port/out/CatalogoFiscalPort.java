package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;

import java.math.BigDecimal;
import java.util.Optional;

public interface CatalogoFiscalPort {

    record DetraccionInfo(
            String codigoBienServicio,
            String descripcion,
            BigDecimal porcentaje,
            BigDecimal montoMinimo
    ) {}

    /**
     * Obtiene la tasa general vigente del IGV (por defecto 18.00).
     */
    BigDecimal obtenerTasaIgvVigente();

    /**
     * Obtiene el monto unitario vigente del ICBPER por bolsa plástica (ej. 0.50).
     */
    BigDecimal obtenerTasaIcbperVigente();

    /**
     * Determina la tasa de IGV aplicable evaluando la sucursal y su ubigeo (Amazonía).
     */
    BigDecimal determinarTasaIgvPorSucursal(Sucursal sucursal);

    /**
     * Busca la configuración oficial de una detracción por código de bien/servicio de SUNAT.
     */
    Optional<DetraccionInfo> buscarDetraccion(String codigoBienServicio);
}
