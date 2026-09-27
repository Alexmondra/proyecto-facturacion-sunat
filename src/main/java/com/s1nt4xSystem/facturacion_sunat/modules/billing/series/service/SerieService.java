package com.s1nt4xSystem.facturacion_sunat.modules.billing.series.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie;

import java.util.List;
import java.util.UUID;

public interface SerieService {
    SerieResponse createSerie(SerieRequest request);
    SerieResponse getSerieById(UUID id);
    Serie getSerieEntity(UUID id);
    List<SerieResponse> getSeriesBySucursal(UUID sucursalId);
    List<SerieResponse> getAllSeries();
    SerieResponse updateSerie(UUID id, SerieUpdateRequest request);
    SerieResponse updateSerie(UUID id, SerieRequest request);
    SerieResponse updateCorrelativo(UUID id, Integer nuevoCorrelativo);
    void deleteSerie(UUID id);
}
