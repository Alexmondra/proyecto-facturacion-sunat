package com.s1nt4xSystem.facturacion_sunat.modules.core.company.validation;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.CertificadoDigitalService;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyValidatorTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private CertificadoDigitalService certificadoDigitalService;

    @InjectMocks
    private CompanyValidator companyValidator;

    private Empresa empresa;
    private EmpresaConfig config;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc("20100070970")
                .razonSocial("EMPRESA TEST S.A.C.")
                .build();

        config = EmpresaConfig.builder()
                .empresa(empresa)
                .userSol("MODDATOS")
                .passSol("moddatos")
                .certificado("tenants/20100070970/certificates/certificate.pfx")
                .certificadoPass("12345678")
                .modoEmision("PROPIO")
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"20100070970", "10445566778", "20601234567"})
    @DisplayName("Debe validar exitosamente RUCs peruanos válidos")
    void validateRucFormat_valido(String ruc) {
        assertThatCode(() -> companyValidator.validateRucFormat(ruc))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "2010007097", "30100070970", "2010007097A", "abc"})
    @DisplayName("Debe lanzar COMPANY_INVALID_RUC si el RUC tiene formato incorrecto")
    void validateRucFormat_invalido(String ruc) {
        assertThatThrownBy(() -> companyValidator.validateRucFormat(ruc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COMPANY_INVALID_RUC);
                    assertThat(be.getCode()).isEqualTo(19003);
                });
    }

    @Test
    @DisplayName("Debe lanzar COMPANY_RUC_ALREADY_EXISTS si el RUC ya existe")
    void validateRucUnique_yaExiste() {
        when(empresaRepository.existsByRuc("20100070970")).thenReturn(true);

        assertThatThrownBy(() -> companyValidator.validateRucUnique("20100070970"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COMPANY_RUC_ALREADY_EXISTS);
                    assertThat(be.getCode()).isEqualTo(19002);
                });
    }

    @Test
    @DisplayName("Debe lanzar COMPANY_RAZON_SOCIAL_REQUIRED si la razón social está vacía")
    void validateRazonSocial_vacia() {
        assertThatThrownBy(() -> companyValidator.validateRazonSocial("  "))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COMPANY_RAZON_SOCIAL_REQUIRED);
                });
    }

    @Test
    @DisplayName("Debe validar modo de emisión PSE requiriendo idPse")
    void validateModoEmision_pseRequiereId() {
        assertThatThrownBy(() -> companyValidator.validateModoEmision("PSE", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COMPANY_PSE_REQUIRED);
                });
    }

    @Test
    @DisplayName("Debe validar archivo de certificado con extensiones permitidas")
    void validateCertificateUpload_extensionInvalida() {
        assertThatThrownBy(() -> companyValidator.validateCertificateUpload("cert.txt", "123456"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COMPANY_INVALID_CERTIFICATE_FILE);
                });
    }

    @Test
    @DisplayName("validateForEmission: debe omitir validaciones fiscales para tickets internos")
    void validateForEmission_ticketInterno() {
        assertThatCode(() -> companyValidator.validateForEmission(empresa, null, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateForEmission: debe fallar si falta usuario SOL")
    void validateForEmission_faltaSol() {
        config.setUserSol(null);

        assertThatThrownBy(() -> companyValidator.validateForEmission(empresa, config, true))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COMPANY_SOL_CREDENTIALS_MISSING);
                    assertThat(be.getCode()).isEqualTo(19006);
                });
    }

    @Test
    @DisplayName("validateForEmission: debe fallar si el archivo de certificado no existe en disco")
    void validateForEmission_faltaCertificadoEnDisco() {
        when(certificadoDigitalService.existeCertificado(anyString())).thenReturn(false);

        assertThatThrownBy(() -> companyValidator.validateForEmission(empresa, config, true))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.COMPANY_CERTIFICATE_NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo(19005);
                });
    }

    @Test
    @DisplayName("validateForEmission: debe pasar si todos los requisitos fiscales están completos")
    void validateForEmission_exitoso() {
        when(certificadoDigitalService.existeCertificado(anyString())).thenReturn(true);

        assertThatCode(() -> companyValidator.validateForEmission(empresa, config, true))
                .doesNotThrowAnyException();
    }
}
