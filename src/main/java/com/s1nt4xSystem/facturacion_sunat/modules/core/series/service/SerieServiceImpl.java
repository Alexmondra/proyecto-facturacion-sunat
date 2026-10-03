package com.s1nt4xSystem.facturacion_sunat.modules.core.series.service;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.service.SucursalService;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.EmpresaTenantService;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.dto.SerieRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.dto.SerieResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.dto.SerieUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.validation.SeriesValidator;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.DomainException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SerieServiceImpl implements SerieService {

    private final SerieRepository serieRepository;
    private final SucursalService sucursalService;
    private final EmpresaTenantService empresaTenantService;
    private final SeriesValidator seriesValidator;

    @org.springframework.beans.factory.annotation.Autowired
    public SerieServiceImpl(
            SerieRepository serieRepository,
            SucursalService sucursalService,
            EmpresaTenantService empresaTenantService,
            SeriesValidator seriesValidator) {
        this.serieRepository = serieRepository;
        this.sucursalService = sucursalService;
        this.empresaTenantService = empresaTenantService;
        this.seriesValidator = seriesValidator;
    }

    public SerieServiceImpl(
            SerieRepository serieRepository,
            SucursalService sucursalService,
            EmpresaTenantService empresaTenantService) {
        this(serieRepository, sucursalService, empresaTenantService,
                new SeriesValidator(serieRepository));
    }

    @Override
    public SerieResponse createSerie(SerieRequest request) {
        Empresa empresa = empresaTenantService.getEmpresaEntity();
        Sucursal sucursal = sucursalService.getSucursalEntity(request.getSucursalId());

        String serieCode = request.getSerie().trim().toUpperCase();
        String tipoDoc = request.getTipoComprobante().trim();

        seriesValidator.validateCreateSerie(empresa.getId(), sucursal, tipoDoc, serieCode, request.getCorrelativo());

        Serie serie = Serie.builder()
                .sucursal(sucursal)
                .tipoComprobante(tipoDoc)
                .serie(serieCode)
                .correlativo(request.getCorrelativo() != null ? request.getCorrelativo() : 0)
                .build();

        return SerieResponse.fromEntity(serieRepository.save(serie));
    }

    @Override
    @Transactional(readOnly = true)
    public SerieResponse getSerieById(UUID id) {
        return SerieResponse.fromEntity(getSerieEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Serie getSerieEntity(UUID id) {
        Serie serie = serieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serie", id));
        Empresa empresa = empresaTenantService.getEmpresaEntity();
        seriesValidator.validateSerieOwnership(serie, empresa.getId());
        return serie;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SerieResponse> getSeriesBySucursal(UUID sucursalId) {
        Empresa empresa = empresaTenantService.getEmpresaEntity();
        Sucursal sucursal = sucursalService.getSucursalEntity(sucursalId);
        seriesValidator.validateSucursalOwnership(sucursal, empresa.getId());

        return serieRepository.findBySucursalId(sucursalId).stream()
                .map(SerieResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SerieResponse> getAllSeries() {
        Empresa empresa = empresaTenantService.getEmpresaEntity();
        return serieRepository.findAll().stream()
                .filter(s -> s.getSucursal() == null || s.getSucursal().getEmpresa() == null
                        || s.getSucursal().getEmpresa().getId().equals(empresa.getId()))
                .map(SerieResponse::fromEntity)
                .toList();
    }

    @Override
    public SerieResponse updateSerie(UUID id, SerieUpdateRequest request) {
        Serie serie = getSerieEntity(id);
        Empresa empresa = empresaTenantService.getEmpresaEntity();

        Sucursal newSucursal = null;
        if (request.getSucursalId() != null && !request.getSucursalId().equals(serie.getSucursal().getId())) {
            newSucursal = sucursalService.getSucursalEntity(request.getSucursalId());
        }

        String newSerieCode = request.getSerie() != null ? request.getSerie().trim().toUpperCase() : serie.getSerie();
        String newTipoDoc = request.getTipoComprobante() != null ? request.getTipoComprobante().trim() : serie.getTipoComprobante();

        seriesValidator.validateUpdateSerie(serie, empresa.getId(), newSucursal, newTipoDoc, newSerieCode, request.getCorrelativo());

        if (newSucursal != null) {
            serie.setSucursal(newSucursal);
        }
        serie.setSerie(newSerieCode);
        serie.setTipoComprobante(newTipoDoc);

        if (request.getCorrelativo() != null) {
            serie.setCorrelativo(request.getCorrelativo());
        }

        return SerieResponse.fromEntity(serieRepository.save(serie));
    }

    @Override
    public SerieResponse updateSerie(UUID id, SerieRequest request) {
        SerieUpdateRequest updateReq = SerieUpdateRequest.builder()
                .sucursalId(request.getSucursalId())
                .tipoComprobante(request.getTipoComprobante())
                .serie(request.getSerie())
                .correlativo(request.getCorrelativo())
                .build();
        return updateSerie(id, updateReq);
    }

    @Override
    public SerieResponse updateCorrelativo(UUID id, Integer nuevoCorrelativo) {
        seriesValidator.validateUpdateCorrelativo(nuevoCorrelativo);
        Serie serie = getSerieEntity(id);
        serie.setCorrelativo(nuevoCorrelativo);
        return SerieResponse.fromEntity(serieRepository.save(serie));
    }

    @Override
    public void deleteSerie(UUID id) {
        Serie serie = getSerieEntity(id);
        seriesValidator.validateCanDelete(serie);
        serieRepository.delete(serie);
    }
}
