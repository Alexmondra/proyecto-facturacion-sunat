package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.CorrelativoServicePort;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.repository.SerieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorrelativoServiceAdapter implements CorrelativoServicePort {

    private final SerieRepository serieRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer obtenerSiguienteCorrelativo(UUID sucursalId, String tipoComprobante, String serie) {
        Serie serieEntity = serieRepository.findForUpdate(sucursalId, tipoComprobante, serie)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("La serie '%s' para el tipo '%s' no está configurada en la sucursal '%s'",
                                serie, tipoComprobante, sucursalId)));

        int siguiente = (serieEntity.getCorrelativo() == null ? 0 : serieEntity.getCorrelativo()) + 1;
        serieEntity.setCorrelativo(siguiente);
        serieRepository.save(serieEntity);

        return siguiente;
    }
}
