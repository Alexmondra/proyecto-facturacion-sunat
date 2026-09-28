package com.s1nt4xSystem.facturacion_sunat.modules.billing.series.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.service.SucursalService;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.service.EmpresaTenantService;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.dto.SerieUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.ResourceNotFoundException;
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

    public SerieServiceImpl(
            SerieRepository serieRepository,
            SucursalService sucursalService,
            EmpresaTenantService empresaTenantService) {
        this.serieRepository = serieRepository;
        this.sucursalService = sucursalService;
        this.empresaTenantService = empresaTenantService;
    }

    @Override
    public SerieResponse createSerie(SerieRequest request) {
        Empresa empresa = empresaTenantService.getEmpresaEntity();
        Sucursal sucursal = sucursalService.getSucursalEntity(request.getSucursalId());

        if (sucursal.getEmpresa() != null && !sucursal.getEmpresa().getId().equals(empresa.getId())) {
            throw new DomainException("La sucursal indicada no pertenece a la empresa actual");
        }

        String serieCode = request.getSerie().trim().toUpperCase();
        String tipoDoc = request.getTipoComprobante().trim();

        // Validaciones de reglas oficiales de SUNAT según TipoComprobante
        com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoComprobante tipoEnum = 
                com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoComprobante.fromCodigo(tipoDoc);
        tipoEnum.validarSerie(serieCode);

        if (serieRepository.existsByTipoComprobanteAndSerie(tipoDoc, serieCode)) {
            throw new DomainException(String.format("La serie '%s' para el tipo '%s' ya está registrada en la empresa (las series no pueden repetirse entre sucursales)", serieCode, tipoDoc));
        }

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
        if (serie.getSucursal() != null && serie.getSucursal().getEmpresa() != null
                && !serie.getSucursal().getEmpresa().getId().equals(empresa.getId())) {
            throw new DomainException("La serie solicitada no pertenece a la empresa actual");
        }
        return serie;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SerieResponse> getSeriesBySucursal(UUID sucursalId) {
        Empresa empresa = empresaTenantService.getEmpresaEntity();
        Sucursal sucursal = sucursalService.getSucursalEntity(sucursalId);
        if (sucursal.getEmpresa() != null && !sucursal.getEmpresa().getId().equals(empresa.getId())) {
            throw new DomainException("La sucursal indicada no pertenece a la empresa actual");
        }

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

        if (request.getSucursalId() != null && !request.getSucursalId().equals(serie.getSucursal().getId())) {
            if (serie.getCorrelativo() > 0) {
                throw new DomainException("No se puede cambiar la sucursal de una serie que ya tiene comprobantes emitidos");
            }
            Sucursal newSucursal = sucursalService.getSucursalEntity(request.getSucursalId());
            if (newSucursal.getEmpresa() != null && !newSucursal.getEmpresa().getId().equals(empresa.getId())) {
                throw new DomainException("La sucursal indicada no pertenece a la empresa actual");
            }
            serie.setSucursal(newSucursal);
        }

        String newSerieCode = request.getSerie() != null ? request.getSerie().trim().toUpperCase() : serie.getSerie();
        String newTipoDoc = request.getTipoComprobante() != null ? request.getTipoComprobante().trim() : serie.getTipoComprobante();

        boolean serieChanged = !newSerieCode.equalsIgnoreCase(serie.getSerie());
        boolean tipoChanged = !newTipoDoc.equals(serie.getTipoComprobante());

        if ((serieChanged || tipoChanged) && serie.getCorrelativo() > 0) {
            throw new DomainException("No se puede cambiar el código o tipo de comprobante de una serie que ya tiene comprobantes emitidos");
        }

        if (serieChanged || tipoChanged) {
            com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoComprobante tipoEnum = 
                    com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoComprobante.fromCodigo(newTipoDoc);
            tipoEnum.validarSerie(newSerieCode);

            Optional<Serie> serieExistente = serieRepository.findByTipoComprobanteAndSerie(newTipoDoc, newSerieCode);
            if (serieExistente.isPresent() && !serieExistente.get().getId().equals(serie.getId())) {
                throw new DomainException(String.format("La serie '%s' para el tipo '%s' ya está registrada en la empresa (las series no pueden repetirse entre sucursales)", newSerieCode, newTipoDoc));
            }
            serie.setSerie(newSerieCode);
            serie.setTipoComprobante(newTipoDoc);
        }

        if (request.getCorrelativo() != null) {
            if (request.getCorrelativo() < 0) {
                throw new DomainException("El correlativo no puede ser negativo");
            }
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
        if (nuevoCorrelativo == null || nuevoCorrelativo < 0) {
            throw new DomainException("El correlativo no puede ser negativo");
        }
        Serie serie = getSerieEntity(id);
        serie.setCorrelativo(nuevoCorrelativo);
        return SerieResponse.fromEntity(serieRepository.save(serie));
    }

    @Override
    public void deleteSerie(UUID id) {
        Serie serie = getSerieEntity(id);
        if (serie.getCorrelativo() > 0) {
            throw new DomainException("No se puede eliminar una serie que ya ha emitido comprobantes (correlativo > 0)");
        }
        serieRepository.delete(serie);
    }
}
