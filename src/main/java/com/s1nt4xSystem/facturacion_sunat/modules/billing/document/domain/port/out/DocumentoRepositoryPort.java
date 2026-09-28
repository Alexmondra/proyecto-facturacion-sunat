package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;

import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepositoryPort {
    ComprobanteFiscal guardar(ComprobanteFiscal comprobante);
    Optional<ComprobanteFiscal> buscarPorClaveIdempotencia(String claveIdempotencia);
    Optional<ComprobanteFiscal> buscarPorId(UUID id);
    Optional<ComprobanteFiscal> buscarPorEmision(UUID empresaId, String tipoComprobante, String serie, Integer numero);
    void actualizarHashYEstado(UUID id, String hashCpe, String estadoInterno);
}
