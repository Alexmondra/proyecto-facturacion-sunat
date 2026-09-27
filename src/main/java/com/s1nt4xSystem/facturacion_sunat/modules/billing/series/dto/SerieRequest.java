package com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerieRequest {

    @NotNull(message = "El ID de la sucursal es obligatorio")
    private UUID sucursalId;

    @NotBlank(message = "El tipo de comprobante es obligatorio (01: Factura, 03: Boleta, 07: NC, 08: ND, 09: GRE)")
    @Size(max = 5, message = "El tipo de comprobante no puede exceder 5 caracteres")
    private String tipoComprobante;

    @NotBlank(message = "La serie es obligatoria (ej. F001, B001, T001)")
    @Pattern(regexp = "^[FBTE0-9][A-Z0-9]{3}$", message = "La serie debe tener formato válido SUNAT de 4 caracteres (ej. F001, B001, T001)")
    private String serie;

    @Builder.Default
    private Integer correlativo = 0;
}
