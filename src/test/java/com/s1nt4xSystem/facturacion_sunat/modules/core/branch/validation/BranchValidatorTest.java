package com.s1nt4xSystem.facturacion_sunat.modules.core.branch.validation;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoUbigeo;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository.CatalogoUbigeoRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchValidatorTest {

    @Mock
    private SucursalRepository sucursalRepository;

    @Mock
    private CatalogoUbigeoRepository catalogoUbigeoRepository;

    @InjectMocks
    private BranchValidator branchValidator;

    private UUID empresaId;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        empresaId = UUID.randomUUID();
        empresa = Empresa.builder().id(empresaId).ruc("20100070970").build();
    }

    @Test
    @DisplayName("Debe lanzar excepción si el código es nulo o vacío")
    void validateNewBranch_codigoVacio() {
        assertThatThrownBy(() -> branchValidator.validateNewBranch("", empresaId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BRANCH_INVALID_CODE));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el código ya existe para la empresa")
    void validateNewBranch_codigoDuplicado() {
        when(sucursalRepository.existsByEmpresaIdAndCodigo(empresaId, "0001")).thenReturn(true);

        assertThatThrownBy(() -> branchValidator.validateNewBranch("0001", empresaId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BRANCH_CODE_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("Debe validar exitosamente cuando el código es nuevo")
    void validateNewBranch_valido() {
        when(sucursalRepository.existsByEmpresaIdAndCodigo(empresaId, "0001")).thenReturn(false);
        when(sucursalRepository.existsByCodigo("0001")).thenReturn(false);

        assertThatCode(() -> branchValidator.validateNewBranch("0001", empresaId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe lanzar excepción si la sucursal pertenece a otra empresa")
    void validateBranchOwnership_perteneceAOtraEmpresa() {
        Empresa otraEmpresa = Empresa.builder().id(UUID.randomUUID()).build();
        Sucursal sucursal = Sucursal.builder().empresa(otraEmpresa).build();

        assertThatThrownBy(() -> branchValidator.validateBranchOwnership(sucursal, empresaId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BRANCH_NOT_OWNED));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el ubigeo no existe")
    void validateUbigeo_noExiste() {
        when(catalogoUbigeoRepository.findByCodigoAndEstadoTrue("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchValidator.validateUbigeo("999999"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BRANCH_UBIGEO_NOT_FOUND));
    }

    @Test
    @DisplayName("Debe retornar la entidad si el ubigeo existe y está activo")
    void validateUbigeo_valido() {
        CatalogoUbigeo ubigeo = CatalogoUbigeo.builder().codigo("150101").departamento("Lima").build();
        when(catalogoUbigeoRepository.findByCodigoAndEstadoTrue("150101")).thenReturn(Optional.of(ubigeo));

        CatalogoUbigeo res = branchValidator.validateUbigeo("150101");
        assertThat(res).isNotNull();
        assertThat(res.getCodigo()).isEqualTo("150101");
    }

    @Test
    @DisplayName("No debe permitir desactivar sucursal matriz 0000")
    void validateStatusChange_noPermiteDesactivarMatriz() {
        Sucursal matriz = Sucursal.builder().codigo("0000").activo(true).build();

        assertThatThrownBy(() -> branchValidator.validateStatusChange(matriz, false))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BRANCH_CANNOT_DISABLE_MAIN));
    }

    @Test
    @DisplayName("No debe permitir eliminar sucursal matriz 0000")
    void validateCanDelete_noPermiteEliminarMatriz() {
        Sucursal matriz = Sucursal.builder().codigo("0000").build();

        assertThatThrownBy(() -> branchValidator.validateCanDelete(matriz))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BRANCH_CANNOT_DELETE_MAIN));
    }
}
