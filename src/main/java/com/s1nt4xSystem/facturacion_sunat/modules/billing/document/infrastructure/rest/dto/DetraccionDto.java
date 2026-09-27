package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
public class DetraccionDto {
    @JsonProperty("codigo_bien_servicio")
    @NotBlank(message = "El código del bien o servicio sujeto a detracción es obligatorio")
    private String codigoBienServicio;

    private BigDecimal porcentaje;

    @JsonProperty("monto_detraccion")
    @JsonAlias({"monto", "monto_detraccion"})
    private BigDecimal montoDetraccion;

    @JsonProperty("medio_pago")
    @Builder.Default
    private String medioPago = "001";

    @JsonProperty("cuenta_banco_nacion")
    @JsonAlias({"cuenta_banco_nacion", "cuenta"})
    private String cuentaBancoNacion;
}
