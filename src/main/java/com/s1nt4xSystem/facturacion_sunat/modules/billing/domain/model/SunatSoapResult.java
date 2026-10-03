package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SunatSoapResult {
    private final DocumentoSunatResponse response;
    private final byte[] cdrZip;
    private final byte[] cdrXml;
    private final String cdrNombreArchivo;
}
