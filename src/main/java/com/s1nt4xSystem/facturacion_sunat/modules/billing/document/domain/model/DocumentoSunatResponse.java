package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoSunatResponse {
    private String ticket;
    private String estadoSunat; // e.g., ACEPTADO, RECHAZADO, OBSERVADO, EXCEPCION
    private String codigoRespuestaSunat; // e.g., "0", "01xx", "2xxx"
    private String mensajeSunat;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaRespuesta;
}
