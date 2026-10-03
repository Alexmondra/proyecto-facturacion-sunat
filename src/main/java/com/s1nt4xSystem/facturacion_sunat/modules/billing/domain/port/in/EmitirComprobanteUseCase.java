package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.dto.EmitirComprobanteCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.CrearComprobanteRequest;

public interface EmitirComprobanteUseCase {
    ComprobanteFiscal emitir(EmitirComprobanteCommand command);
    ComprobanteFiscal emitir(CrearComprobanteRequest request);
}
