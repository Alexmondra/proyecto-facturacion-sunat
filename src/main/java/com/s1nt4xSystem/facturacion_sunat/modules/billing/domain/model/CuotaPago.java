package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class CuotaPago {

    private final Integer numeroCuota;
    private final LocalDate fechaVencimiento;
    private final BigDecimal monto;
    @Builder.Default
    private final String moneda = "PEN";
}
