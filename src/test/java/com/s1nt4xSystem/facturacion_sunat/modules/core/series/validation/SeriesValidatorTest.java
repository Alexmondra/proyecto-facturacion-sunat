package com.s1nt4xSystem.facturacion_sunat.modules.core.series.validation;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeriesValidatorTest {

    @Mock
    private SerieRepository serieRepository;

    @InjectMocks
    private SeriesValidator seriesValidator;

    private UUID empresaId;
    private Empresa empresa;
    private Sucursal sucursal;

    @BeforeEach
    void setUp() {
        empresaId = UUID.randomUUID();
        empresa = Empresa.builder().id(empresaId).ruc("20100070970").build();
        sucursal = Sucursal.builder().id(UUID.randomUUID()).empresa(empresa).codigo("0000").build();
    }

    @Test
    @DisplayName("Debe lanzar excepción si la sucursal pertenece a otra empresa")
    void validateCreateSerie_sucursalAjena() {
        Empresa otraEmpresa = Empresa.builder().id(UUID.randomUUID()).build();
        Sucursal sucursalAjena = Sucursal.builder().empresa(otraEmpresa).build();

        assertThatThrownBy(() -> seriesValidator.validateCreateSerie(empresaId, sucursalAjena, "01", "F001", 0))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SERIE_BRANCH_NOT_OWNED));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la serie ya está registrada para ese tipo")
    void validateCreateSerie_serieDuplicada() {
        when(serieRepository.existsByTipoComprobanteAndSerie("01", "F001")).thenReturn(true);

        assertThatThrownBy(() -> seriesValidator.validateCreateSerie(empresaId, sucursal, "01", "F001", 0))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SERIE_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("Debe permitir serie válida y disponible")
    void validateCreateSerie_valida() {
        when(serieRepository.existsByTipoComprobanteAndSerie("01", "F001")).thenReturn(false);

        assertThatCode(() -> seriesValidator.validateCreateSerie(empresaId, sucursal, "01", "F001", 0))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("No debe permitir cambiar sucursal si la serie tiene comprobantes emitidos")
    void validateUpdateSerie_noPermiteCambiarSucursalConEmisiones() {
        Serie serie = Serie.builder()
                .sucursal(sucursal)
                .tipoComprobante("01")
                .serie("F001")
                .correlativo(10)
                .build();

        Sucursal nuevaSucursal = Sucursal.builder().id(UUID.randomUUID()).empresa(empresa).build();

        assertThatThrownBy(() -> seriesValidator.validateUpdateSerie(serie, empresaId, nuevaSucursal, "01", "F001", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SERIE_CANNOT_CHANGE_BRANCH_WITH_EMISSIONS));
    }

    @Test
    @DisplayName("No debe permitir cambiar código de serie si ya tiene comprobantes emitidos")
    void validateUpdateSerie_noPermiteCambiarCodigoConEmisiones() {
        Serie serie = Serie.builder()
                .sucursal(sucursal)
                .tipoComprobante("01")
                .serie("F001")
                .correlativo(5)
                .build();

        assertThatThrownBy(() -> seriesValidator.validateUpdateSerie(serie, empresaId, null, "01", "F002", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SERIE_CANNOT_CHANGE_CODE_WITH_EMISSIONS));
    }

    @Test
    @DisplayName("No debe permitir eliminar serie con comprobantes emitidos")
    void validateCanDelete_noPermiteConEmisiones() {
        Serie serie = Serie.builder().correlativo(1).build();

        assertThatThrownBy(() -> seriesValidator.validateCanDelete(serie))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.SERIE_CANNOT_DELETE_WITH_EMISSIONS));
    }
}
