package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.CrearNotaRequest;

public interface EmitirNotaUseCase {
    ComprobanteFiscal emitirNota(CrearNotaRequest request);
}
