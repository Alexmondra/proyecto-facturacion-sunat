package com.s1nt4xSystem.facturacion_sunat.modules.core.series.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("sucursal_id")
    @JsonAlias({"sucursalId", "sucursal_id"})
    private UUID sucursalId;

    @NotBlank(message = "El tipo de comprobante es obligatorio (01: Factura, 03: Boleta, 07: NC, 08: ND, 09: GRE, 00: Ticket)")
    @Size(max = 5, message = "El tipo de comprobante no puede exceder 5 caracteres")
    @JsonProperty("tipo_comprobante")
    @JsonAlias({"tipoComprobante", "tipo_comprobante", "tipo_documento", "tipo"})
    private String tipoComprobante;

    @NotBlank(message = "La serie es obligatoria (ej. F001, B001, NV01)")
    @Pattern(regexp = "^[A-Za-z0-9]{4}$", message = "La serie debe tener exactamente 4 caracteres alfanuméricos (ej. F001, B001, NV01)")
    private String serie;

    @Builder.Default
    private Integer correlativo = 0;
}
