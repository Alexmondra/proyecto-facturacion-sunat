package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResultadoFirma {
    private final byte[] xmlFirmado;
    private final String hashCpe;
}
