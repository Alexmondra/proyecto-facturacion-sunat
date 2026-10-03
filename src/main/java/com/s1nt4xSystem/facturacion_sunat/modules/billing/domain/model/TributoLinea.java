package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class TributoLinea {

    private final Long tributoId; // 1: IGV, 2: ICBPER, etc.
    private final String codigoTributo; // "1000", "7152", etc.
    private final String nombreTributo; // "IGV", "ICBPER", etc.
    private final String tipoTributo; // "VAT", "OTH", etc.
    private final BigDecimal baseImponible;
    private final BigDecimal porcentaje;
    private final BigDecimal cantidadBase; // para ICBPER (número de bolsas)
    private final BigDecimal monto;
}
