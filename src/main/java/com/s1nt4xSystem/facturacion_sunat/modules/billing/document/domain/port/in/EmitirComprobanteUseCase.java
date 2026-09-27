package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto.EmitirComprobanteCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.CrearComprobanteRequest;

public interface EmitirComprobanteUseCase {
    ComprobanteFiscal emitir(EmitirComprobanteCommand command);
    ComprobanteFiscal emitir(CrearComprobanteRequest request);
}
