package com.s1nt4xSystem.facturacion_sunat.modules.billing.company.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificadoDigitalResponse {
    private String ruc;
    @Builder.Default
    private Boolean tieneCertificado = true;
    private String sujeto;
    private String emisor;
    private String numeroSerie;
    private LocalDateTime validoDesde;
    private LocalDateTime validoHasta;
    private Long diasRestantes;
    private Boolean vencido;
    private String formatoOriginal;
    private String mensaje;

    @JsonIgnore
    private String rutaAlmacenamiento;

    @JsonIgnore
    private String claveAsignada;
}
