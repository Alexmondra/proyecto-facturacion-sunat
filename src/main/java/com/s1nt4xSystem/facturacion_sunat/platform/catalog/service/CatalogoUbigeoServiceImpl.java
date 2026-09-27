package com.s1nt4xSystem.facturacion_sunat.platform.catalog.service;

import com.s1nt4xSystem.facturacion_sunat.platform.catalog.dto.UbigeoResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoUbigeo;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository.CatalogoUbigeoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CatalogoUbigeoServiceImpl implements CatalogoUbigeoService {

    private final CatalogoUbigeoRepository ubigeoRepository;

    public CatalogoUbigeoServiceImpl(CatalogoUbigeoRepository ubigeoRepository) {
        this.ubigeoRepository = ubigeoRepository;
    }

    @Override
    public List<UbigeoResponse> getAllUbigeos() {
        return ubigeoRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEstado()))
                .map(UbigeoResponse::fromEntity)
                .toList();
    }

    @Override
    public List<UbigeoResponse> searchUbigeos(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllUbigeos();
        }
        return ubigeoRepository.searchUbigeos(query.trim()).stream()
                .map(UbigeoResponse::fromEntity)
                .toList();
    }

    @Override
    public List<String> getDepartamentos() {
        return ubigeoRepository.findDistinctDepartamentos();
    }

    @Override
    public List<String> getProvincias(String departamento) {
        if (departamento == null || departamento.trim().isEmpty()) {
            return List.of();
        }
        return ubigeoRepository.findDistinctProvinciasByDepartamento(departamento.trim());
    }

    @Override
    public List<UbigeoResponse> getDistritos(String departamento, String provincia) {
        if (departamento == null || provincia == null) {
            return List.of();
        }
        return ubigeoRepository.findByDepartamentoAndProvincia(departamento.trim(), provincia.trim()).stream()
                .map(UbigeoResponse::fromEntity)
                .toList();
    }

    @Override
    public Optional<CatalogoUbigeo> findByCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return ubigeoRepository.findByCodigoAndEstadoTrue(codigo.trim());
    }
}
