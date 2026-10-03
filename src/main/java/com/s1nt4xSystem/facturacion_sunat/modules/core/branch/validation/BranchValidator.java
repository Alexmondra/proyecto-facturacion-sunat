package com.s1nt4xSystem.facturacion_sunat.modules.core.branch.validation;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoUbigeo;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository.CatalogoUbigeoRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Validador centralizado de reglas de negocio para el módulo de Sucursales (Branch).
 * Encapsula la validación de código único, pertenencia a la empresa (multi-tenant),
 * existencia de ubigeo y restricciones sobre la sucursal principal '0000'.
 */
@Component
public class BranchValidator {

    public static final String CODIGO_MATRIZ = "0000";

    private final SucursalRepository sucursalRepository;
    private final CatalogoUbigeoRepository catalogoUbigeoRepository;

    public BranchValidator(SucursalRepository sucursalRepository,
                           CatalogoUbigeoRepository catalogoUbigeoRepository) {
        this.sucursalRepository = sucursalRepository;
        this.catalogoUbigeoRepository = catalogoUbigeoRepository;
    }

    /**
     * Valida la creación de una nueva sucursal comprobando duplicidad de código.
     */
    public void validateNewBranch(String codigo, UUID empresaId) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException(ErrorCode.BRANCH_INVALID_CODE);
        }
        String cleanCodigo = codigo.trim();
        if (sucursalRepository.existsByEmpresaIdAndCodigo(empresaId, cleanCodigo)
                || sucursalRepository.existsByCodigo(cleanCodigo)) {
            throw new BusinessException(ErrorCode.BRANCH_CODE_ALREADY_EXISTS, cleanCodigo);
        }
    }

    /**
     * Valida que la sucursal pertenezca a la empresa del tenant activo.
     */
    public void validateBranchOwnership(Sucursal sucursal, UUID empresaId) {
        if (sucursal != null && sucursal.getEmpresa() != null && empresaId != null
                && !sucursal.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException(ErrorCode.BRANCH_NOT_OWNED);
        }
    }

    /**
     * Valida el cambio de código de una sucursal existente.
     */
    public void validateUpdateCode(Sucursal sucursal, String newCodigo, UUID empresaId) {
        if (newCodigo == null || newCodigo.isBlank()) {
            return;
        }
        String cleanCodigo = newCodigo.trim();
        if (!sucursal.getCodigo().equalsIgnoreCase(cleanCodigo)
                && (sucursalRepository.existsByEmpresaIdAndCodigo(empresaId, cleanCodigo)
                || sucursalRepository.existsByCodigo(cleanCodigo))) {
            throw new BusinessException(ErrorCode.BRANCH_CODE_IN_USE, cleanCodigo);
        }
    }

    /**
     * Valida que el código de ubigeo exista y esté activo en el catálogo nacional.
     */
    public CatalogoUbigeo validateUbigeo(String ubigeo) {
        if (ubigeo == null || ubigeo.trim().isEmpty()) {
            return null;
        }
        String cleanUbigeo = ubigeo.trim();
        return catalogoUbigeoRepository.findByCodigoAndEstadoTrue(cleanUbigeo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_UBIGEO_NOT_FOUND, cleanUbigeo));
    }

    /**
     * Valida que no se intente desactivar la sucursal principal ('0000').
     */
    public void validateStatusChange(Sucursal sucursal, Boolean activo) {
        if (Boolean.FALSE.equals(activo) && CODIGO_MATRIZ.equalsIgnoreCase(sucursal.getCodigo())) {
            throw new BusinessException(ErrorCode.BRANCH_CANNOT_DISABLE_MAIN);
        }
    }

    /**
     * Valida que no se elimine la sucursal principal ('0000').
     */
    public void validateCanDelete(Sucursal sucursal) {
        if (CODIGO_MATRIZ.equalsIgnoreCase(sucursal.getCodigo())) {
            throw new BusinessException(ErrorCode.BRANCH_CANNOT_DELETE_MAIN);
        }
    }
}
