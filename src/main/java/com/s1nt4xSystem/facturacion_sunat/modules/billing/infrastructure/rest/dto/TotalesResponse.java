package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TotalesResponse {

    @JsonProperty("total_gravada")
    private BigDecimal totalGravada;

    @JsonProperty("total_exonerada")
    private BigDecimal totalExonerada;

    @JsonProperty("total_inafecta")
    private BigDecimal totalInafecta;

    @JsonProperty("total_gratuita")
    private BigDecimal totalGratuita;

    @JsonProperty("total_exportacion")
    private BigDecimal totalExportacion;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    @JsonProperty("total_igv")
    private BigDecimal totalIgv;

    @JsonProperty("total_isc")
    private BigDecimal totalIsc;

    @JsonProperty("total_icbper")
    private BigDecimal totalIcbper;

    @JsonProperty("total_tributos")
    private BigDecimal totalTributos;

    @JsonProperty("total_otros_cargos")
    private BigDecimal totalOtrosCargos;

    @JsonProperty("total_descuentos")
    private BigDecimal totalDescuentos;

    @JsonProperty("total_detraccion")
    private BigDecimal totalDetraccion;

    @JsonProperty("total_pagado")
    private BigDecimal totalPagado;

    @JsonProperty("total")
    private BigDecimal total;
}
