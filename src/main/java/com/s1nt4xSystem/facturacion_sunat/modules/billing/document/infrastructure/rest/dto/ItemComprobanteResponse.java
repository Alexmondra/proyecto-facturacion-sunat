package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto;

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
public class ItemComprobanteResponse {
    private String codigo;
    private String descripcion;
    @JsonProperty("unidad_medida")
    private String unidadMedida;
    @JsonProperty("codigo_afectacion_sunat")
    private String codigoAfectacionSunat;
    private BigDecimal cantidad;
    private BigDecimal valor;
    private Boolean icbper;
    @JsonProperty("cantidad_bolsas_icbper")
    private Integer cantidadBolsasIcbper;
    private IscDto isc;
    private BigDecimal subtotal;
    private BigDecimal igv;
    @JsonProperty("isc_monto")
    private BigDecimal iscMonto;
    @JsonProperty("icbper_monto")
    private BigDecimal icbperMonto;
    private BigDecimal total;
}
