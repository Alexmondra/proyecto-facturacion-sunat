package com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SerieUpdateRequest {

    @JsonProperty("sucursal_id")
    @JsonAlias({"sucursalId", "sucursal_id"})
    private UUID sucursalId;

    @Size(max = 5, message = "El tipo de comprobante no puede exceder 5 caracteres")
    @JsonProperty("tipo_comprobante")
    @JsonAlias({"tipoComprobante", "tipo_comprobante", "tipo_documento", "tipo"})
    private String tipoComprobante;

    @Pattern(regexp = "^[A-Za-z0-9]{4}$", message = "La serie debe tener exactamente 4 caracteres alfanuméricos (ej. F001, B001, NV01)")
    private String serie;

    private Integer correlativo;
}
