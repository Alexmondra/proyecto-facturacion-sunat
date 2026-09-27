package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmitirComprobanteCommand {

    private String claveIdempotencia;
    private UUID sucursalId;

    @NotBlank(message = "El tipo de comprobante es obligatorio (01, 03, 07, 08)")
    private String tipoComprobante;

    @NotBlank(message = "La serie es obligatoria (ej. F001, B001)")
    private String serie;

    private Integer numero;

    @Builder.Default
    private String moneda = "PEN";

    @Builder.Default
    private String tipoOperacion = "0101";

    @NotBlank(message = "El tipo de documento del cliente es obligatorio (6=RUC, 1=DNI, 0=Sin Doc)")
    private String clienteTipoDoc;

    @NotBlank(message = "El número de documento del cliente es obligatorio")
    private String clienteNumeroDoc;

    @NotBlank(message = "La razón social o nombre del cliente es obligatorio")
    private String clienteNombre;

    private String clienteDireccion;

    @Builder.Default
    private String formaPago = "CONTADO";

    private BigDecimal descuentoGlobal;

    @NotEmpty(message = "El comprobante debe contener al menos un ítem")
    @Valid
    @Builder.Default
    private List<ComprobanteItemCommand> items = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<CuotaCommand> cuotas = new ArrayList<>();

    @Valid
    private DetraccionCommand detraccion;

    @Valid
    @Builder.Default
    private List<ReferenciaCommand> referencias = new ArrayList<>();
}
