package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;

import java.util.Optional;
import java.util.UUID;

public interface ConsultarComprobanteUseCase {
    Optional<ComprobanteFiscal> consultarPorId(UUID id);
    Optional<ComprobanteFiscal> consultarPorClaveIdempotencia(String claveIdempotencia);
}
