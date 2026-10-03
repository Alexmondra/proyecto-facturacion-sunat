package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenciaCommand {
    private String tipoRelacion;
    private UUID documentoReferenciadoId;

    @NotBlank(message = "El tipo de documento referenciado es obligatorio (01, 03)")
    private String tipoDocumentoRef;

    @NotBlank(message = "La serie del documento referenciado es obligatoria")
    private String serieRef;

    @NotNull(message = "El número del documento referenciado es obligatorio")
    private Integer numeroRef;

    @NotBlank(message = "El código de motivo de emisión es obligatorio")
    private String motivoCodigo;

    private String motivoDescripcion;
    private LocalDate fechaEmisionRef;
}
