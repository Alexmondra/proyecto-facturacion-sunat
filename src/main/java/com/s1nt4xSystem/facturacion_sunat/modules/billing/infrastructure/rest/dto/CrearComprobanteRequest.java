package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.HasRawPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrearComprobanteRequest implements HasRawPayload {

    @JsonIgnore
    private String rawPayload;

    @Valid
    @Builder.Default
    private EmisorDto emisor = new EmisorDto();

    @Valid
    @NotNull(message = "El bloque comprobante es obligatorio")
    private ComprobanteDataRequest comprobante;

    @Valid
    private ClienteDto cliente;

    @Valid
    @Builder.Default
    private List<ItemComprobanteRequest> items = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<CuotaDto> cuotas = new ArrayList<>();

    @Valid
    private DetraccionDto detraccion;

    @JsonProperty("descuento_global")
    private BigDecimal descuentoGlobal;

    public ClienteDto getCliente() {
        if (this.cliente != null) {
            return this.cliente;
        }
        if (this.comprobante != null) {
            return this.comprobante.getCliente();
        }
        return null;
    }

    public List<ItemComprobanteRequest> getItems() {
        if (this.items != null && !this.items.isEmpty()) {
            return this.items;
        }
        if (this.comprobante != null && this.comprobante.getItems() != null && !this.comprobante.getItems().isEmpty()) {
            return this.comprobante.getItems();
        }
        return this.items != null ? this.items : new ArrayList<>();
    }

    public List<CuotaDto> getCuotas() {
        if (this.cuotas != null && !this.cuotas.isEmpty()) {
            return this.cuotas;
        }
        if (this.comprobante != null && this.comprobante.getCuotas() != null && !this.comprobante.getCuotas().isEmpty()) {
            return this.comprobante.getCuotas();
        }
        return this.cuotas != null ? this.cuotas : new ArrayList<>();
    }

    public DetraccionDto getDetraccion() {
        if (this.detraccion != null) {
            return this.detraccion;
        }
        if (this.comprobante != null) {
            return this.comprobante.getDetraccion();
        }
        return null;
    }

    public BigDecimal getDescuentoGlobal() {
        if (this.descuentoGlobal != null) {
            return this.descuentoGlobal;
        }
        if (this.comprobante != null) {
            return this.comprobante.getDescuentoGlobal();
        }
        return null;
    }
}
