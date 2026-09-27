package com.s1nt4xSystem.facturacion_sunat.platform.catalog.service;

import com.s1nt4xSystem.facturacion_sunat.platform.catalog.dto.UbigeoResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoUbigeo;

import java.util.List;
import java.util.Optional;

public interface CatalogoUbigeoService {
    List<UbigeoResponse> getAllUbigeos();
    List<UbigeoResponse> searchUbigeos(String query);
    List<String> getDepartamentos();
    List<String> getProvincias(String departamento);
    List<UbigeoResponse> getDistritos(String departamento, String provincia);
    Optional<CatalogoUbigeo> findByCodigo(String codigo);
}
