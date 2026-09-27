package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class TotalesAfectacion {

    private final String tipoAfectacionCodigo;
    private final String tipoTotal; // "GRAVADO", "EXONERADO", "INAFECTO", "GRATUITO", "EXPORTACION"
    private final BigDecimal baseImponible;
    private final BigDecimal montoTributo;
    private final BigDecimal montoTotal;
}
