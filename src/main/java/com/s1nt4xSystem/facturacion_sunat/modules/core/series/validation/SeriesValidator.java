package com.s1nt4xSystem.facturacion_sunat.modules.core.series.validation;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Validador centralizado de reglas de negocio para el módulo de Series y Correlativos.
 * Encapsula la validación de formato SUNAT por tipo de comprobante, unicidad en la empresa,
 * pertenencia a la empresa (multi-tenant) e inmutabilidad de series con comprobantes emitidos.
 */
@Component
public class SeriesValidator {

    private final SerieRepository serieRepository;

    public SeriesValidator(SerieRepository serieRepository) {
        this.serieRepository = serieRepository;
    }

    /**
     * Valida la creación de una serie: pertenencia de sucursal, formato oficial y unicidad.
     */
    public void validateCreateSerie(UUID empresaId, Sucursal sucursal, String tipoDoc, String serieCode, Integer correlativo) {
        validateSucursalOwnership(sucursal, empresaId);

        TipoComprobante tipoEnum = TipoComprobante.fromCodigo(tipoDoc);
        tipoEnum.validarSerie(serieCode);

        if (serieRepository.existsByTipoComprobanteAndSerie(tipoDoc, serieCode)) {
            throw new BusinessException(ErrorCode.SERIE_ALREADY_EXISTS, serieCode, tipoDoc);
        }

        if (correlativo != null && correlativo < 0) {
            throw new BusinessException(ErrorCode.SERIE_CORRELATIVO_NEGATIVE);
        }
    }

    /**
     * Valida que la serie solicitada pertenezca a la empresa actual.
     */
    public void validateSerieOwnership(Serie serie, UUID empresaId) {
        if (serie != null && serie.getSucursal() != null && serie.getSucursal().getEmpresa() != null
                && empresaId != null && !serie.getSucursal().getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException(ErrorCode.SERIE_NOT_OWNED);
        }
    }

    /**
     * Valida que la sucursal indicada pertenezca a la empresa actual.
     */
    public void validateSucursalOwnership(Sucursal sucursal, UUID empresaId) {
        if (sucursal != null && sucursal.getEmpresa() != null && empresaId != null
                && !sucursal.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException(ErrorCode.SERIE_BRANCH_NOT_OWNED);
        }
    }

    /**
     * Valida las modificaciones a una serie existente, asegurando que no se alteren
     * series que ya tienen comprobantes emitidos (correlativo > 0).
     */
    public void validateUpdateSerie(Serie serie, UUID empresaId, Sucursal newSucursal,
                                   String newTipoDoc, String newSerieCode, Integer newCorrelativo) {
        // Validación de cambio de sucursal
        if (newSucursal != null && (serie.getSucursal() == null || !newSucursal.getId().equals(serie.getSucursal().getId()))) {
            if (serie.getCorrelativo() > 0) {
                throw new BusinessException(ErrorCode.SERIE_CANNOT_CHANGE_BRANCH_WITH_EMISSIONS);
            }
            validateSucursalOwnership(newSucursal, empresaId);
        }

        // Validación de cambio de serie o tipo de comprobante
        boolean serieChanged = newSerieCode != null && !newSerieCode.equalsIgnoreCase(serie.getSerie());
        boolean tipoChanged = newTipoDoc != null && !newTipoDoc.equals(serie.getTipoComprobante());

        if (serieChanged || tipoChanged) {
            if (serie.getCorrelativo() > 0) {
                throw new BusinessException(ErrorCode.SERIE_CANNOT_CHANGE_CODE_WITH_EMISSIONS);
            }

            TipoComprobante tipoEnum = TipoComprobante.fromCodigo(newTipoDoc);
            tipoEnum.validarSerie(newSerieCode);

            Optional<Serie> serieExistente = serieRepository.findByTipoComprobanteAndSerie(newTipoDoc, newSerieCode);
            if (serieExistente.isPresent() && !serieExistente.get().getId().equals(serie.getId())) {
                throw new BusinessException(ErrorCode.SERIE_ALREADY_EXISTS, newSerieCode, newTipoDoc);
            }
        }

        // Validación de correlativo
        if (newCorrelativo != null && newCorrelativo < 0) {
            throw new BusinessException(ErrorCode.SERIE_CORRELATIVO_NEGATIVE);
        }
    }

    /**
     * Valida actualización directa de correlativo.
     */
    public void validateUpdateCorrelativo(Integer nuevoCorrelativo) {
        if (nuevoCorrelativo == null || nuevoCorrelativo < 0) {
            throw new BusinessException(ErrorCode.SERIE_CORRELATIVO_NEGATIVE);
        }
    }

    /**
     * Valida que no se pueda eliminar una serie con comprobantes emitidos.
     */
    public void validateCanDelete(Serie serie) {
        if (serie != null && serie.getCorrelativo() > 0) {
            throw new BusinessException(ErrorCode.SERIE_CANNOT_DELETE_WITH_EMISSIONS);
        }
    }
}
