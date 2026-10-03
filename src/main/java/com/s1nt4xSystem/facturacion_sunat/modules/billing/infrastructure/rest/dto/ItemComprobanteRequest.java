package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemComprobanteRequest {

    @JsonProperty("codigo_producto")
    @JsonAlias({"codigo", "codigo_producto"})
    private String codigoProducto;

    @NotBlank(message = "La descripción del ítem es obligatoria")
    private String descripcion;

    @JsonProperty("unidad_medida")
    @Builder.Default
    private String unidadMedida = "NIU";

    @JsonProperty("codigo_afectacion_sunat")
    @JsonAlias({"tipo_afectacion_igv", "codigo_afectacion_sunat", "tipo_afectacion", "afectacion"})
    @NotBlank(message = "El código de afectación SUNAT es obligatorio (10, 20, 30, etc.)")
    private String codigoAfectacionSunat;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
    private BigDecimal cantidad;

    @NotNull(message = "El valor del producto es obligatorio")
    @JsonProperty("valor")
    @JsonAlias({"valor_unitario", "precio_unitario", "precio", "valor"})
    private BigDecimal valor;

    @Builder.Default
    private Boolean icbper = false;

    @JsonProperty("cantidad_bolsas_icbper")
    @JsonAlias({"cantidad_bolsas", "cantidadBolsasIcbper", "bolsas"})
    private Integer cantidadBolsasIcbper;

    private IscDto isc;

    private BigDecimal descuento;
}
