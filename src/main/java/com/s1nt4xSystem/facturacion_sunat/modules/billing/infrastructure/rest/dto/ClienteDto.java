package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClienteDto {
    @JsonProperty("tipo_documento")
    @JsonAlias({"tipo_documento", "tipo_doc"})
    private String tipoDocumento;

    @JsonProperty("numero_documento")
    @JsonAlias({"numero_documento", "num_doc", "numero"})
    private String numeroDocumento;

    @JsonProperty("nombre_razon_social")
    @JsonAlias({"razon_social", "nombre_razon_social", "nombre"})
    private String nombreRazonSocial;

    private String direccion;
}
