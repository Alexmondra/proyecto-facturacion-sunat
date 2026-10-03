package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class DetraccionFiscal {

    private final String codigoBienServicio; // Catálogo 54 SUNAT (ej. "022")
    private final BigDecimal porcentaje; // ej. 10.00
    private final BigDecimal montoDetraccion;
    @Builder.Default
    private final String moneda = "PEN";
    private final String medioPago; // "001" (Depósito en cuenta)
    private final String numeroCuentaDetraccion; // Cuenta Banco de la Nación
}
