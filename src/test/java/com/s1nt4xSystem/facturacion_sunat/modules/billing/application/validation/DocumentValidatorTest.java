package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.validation;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.dto.ComprobanteItemCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.dto.EmitirComprobanteCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoAfectacionIgv;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaConfigRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.validation.CompanyValidator;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentValidatorTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private EmpresaConfigRepository empresaConfigRepository;

    @Mock
    private SucursalRepository sucursalRepository;

    @Mock
    private SerieRepository serieRepository;

    @Mock
    private CompanyValidator companyValidator;

    private DocumentValidator documentValidator;

    @BeforeEach
    void setUp() {
        documentValidator = new DocumentValidator(
                empresaRepository, empresaConfigRepository, sucursalRepository, serieRepository, companyValidator);
    }

    @Test
    void validateAndGetEmpresaTenant_whenNoneExists_shouldThrow() {
        when(empresaRepository.findAll()).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                documentValidator.validateAndGetEmpresaTenant());
        assertEquals(ErrorCode.COMPANY_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void validateEmisorRucCoincide_whenMismatch_shouldThrow() {
        Empresa empresa = Empresa.builder().ruc("20111111111").build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                documentValidator.validateEmisorRucCoincide("20222222222", empresa));
        assertEquals(ErrorCode.DOCUMENT_EMISOR_RUC_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    void validateSerieFormato_whenInvalidSerie_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                documentValidator.validateSerieFormato(TipoComprobante.FACTURA, "B001"));
        assertEquals(ErrorCode.DOCUMENT_SERIE_INVALID_FORMAT.getCode(), ex.getCode());
    }

    @Test
    void validateSerieFormato_whenValid_shouldSucceed() {
        assertDoesNotThrow(() ->
                documentValidator.validateSerieFormato(TipoComprobante.FACTURA, "F001"));
        assertDoesNotThrow(() ->
                documentValidator.validateSerieFormato(TipoComprobante.BOLETA, "B001"));
    }

    @Test
    void validateAndResolveSerie_whenSucursalNotFound_shouldThrow() {
        UUID sucursalId = UUID.randomUUID();
        EmitirComprobanteCommand cmd = EmitirComprobanteCommand.builder()
                .sucursalId(sucursalId)
                .tipoComprobante("01")
                .serie("F001")
                .build();

        when(sucursalRepository.findById(sucursalId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                documentValidator.validateAndResolveSerie(cmd));
        assertEquals(ErrorCode.DOCUMENT_SUCURSAL_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void validateAndResolveSerie_whenSerieNotFound_shouldThrow() {
        EmitirComprobanteCommand cmd = EmitirComprobanteCommand.builder()
                .tipoComprobante("01")
                .serie("F999")
                .build();

        when(serieRepository.findByTipoComprobanteAndSerie("01", "F999")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                documentValidator.validateAndResolveSerie(cmd));
        assertEquals(ErrorCode.DOCUMENT_SERIE_NOT_CONFIGURED_EMPRESA.getCode(), ex.getCode());
    }

    @Test
    void validateItemPrecios_whenBothNull_shouldThrow() {
        ComprobanteItemCommand item = ComprobanteItemCommand.builder().descripcion("Item Test").build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                documentValidator.validateItemPrecios(item, null, null));
        assertEquals(ErrorCode.DOCUMENT_ITEM_PRICE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void validatePrecioBolsaIcbper_whenBelowTasa_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                documentValidator.validatePrecioBolsaIcbper(
                        TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA,
                        new BigDecimal("0.40"),
                        new BigDecimal("0.50")));
        assertEquals(ErrorCode.DOCUMENT_BOLSA_PRICE_BELOW_ICBPER.getCode(), ex.getCode());
    }

    @Test
    void validateTipoNota_whenInvalid_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                documentValidator.validateTipoNota("01"));
        assertEquals(ErrorCode.DOCUMENT_NOTA_TYPE_INVALID.getCode(), ex.getCode());
    }
}
