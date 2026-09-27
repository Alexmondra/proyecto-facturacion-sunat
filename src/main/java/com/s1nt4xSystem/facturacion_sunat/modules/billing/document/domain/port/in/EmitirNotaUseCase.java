package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.CrearNotaRequest;

public interface EmitirNotaUseCase {
    ComprobanteFiscal emitirNota(CrearNotaRequest request);
}
