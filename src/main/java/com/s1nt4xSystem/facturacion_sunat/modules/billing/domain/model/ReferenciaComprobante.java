package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ReferenciaComprobante {

    private final UUID id;
    private final String tipoRelacion; // Catálogo 19
    private final UUID documentoReferenciadoId;
    private final String tipoDocumentoRef; // "01", "03"
    private final String serieRef; // "F001"
    private final Integer numeroRef; // 123
    private final String motivoCodigo; // Catálogo 09 (NC) o Catálogo 10 (ND)
    private final String motivoDescripcion;
    private final LocalDate fechaEmisionRef;
    private final String monedaRef;
}
