package com.s1nt4xSystem.facturacion_sunat.modules.core.company.service;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.EmpresaConfigRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.EmpresaConfigResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.EmpresaResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.EmpresaTenantUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaConfigRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaTenantServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private EmpresaConfigRepository empresaConfigRepository;

    @Mock
    private CertificadoDigitalService certificadoDigitalService;

    @Mock
    private com.s1nt4xSystem.facturacion_sunat.modules.core.company.validation.CompanyValidator companyValidator;

    @InjectMocks
    private EmpresaTenantServiceImpl empresaTenantService;

    private Empresa empresaMock;
    private UUID empresaId;

    @BeforeEach
    void setUp() {
        empresaId = UUID.randomUUID();
        empresaMock = Empresa.builder()
                .id(empresaId)
                .ruc("20100070970")
                .razonSocial("SUPERMERCADOS RETAIL DEL PERU S.A.C.")
                .direccionFiscal("AV. JAVIER PRADO ESTE 4500")
                .entorno("PSE")
                .incluidoTributo(true)
                .build();
    }

    @Test
    @DisplayName("Debe obtener los datos de la empresa actual del tenant")
    void testGetEmpresa() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaMock));

        EmpresaResponse response = empresaTenantService.getEmpresa();

        assertNotNull(response);
        assertEquals("20100070970", response.getRuc());
        assertEquals("SUPERMERCADOS RETAIL DEL PERU S.A.C.", response.getRazonSocial());
        assertEquals("PSE", response.getEntorno());
    }

    @Test
    @DisplayName("Debe auto-crear e inicializar config si no existe en la BD sin arrojar 404")
    void testGetConfigAutoCreacionSiNoExiste() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaMock));
        when(empresaConfigRepository.findByEmpresaId(empresaId)).thenReturn(Optional.empty());
        when(empresaConfigRepository.save(any(EmpresaConfig.class))).thenAnswer(inv -> {
            EmpresaConfig c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        EmpresaConfigResponse configResponse = empresaTenantService.getConfig();

        assertNotNull(configResponse);
        assertEquals("PROPIO", configResponse.getModoEmision());
        assertTrue(configResponse.getEnvioAsincrono());
        assertEquals(empresaId, configResponse.getEmpresaId());
        verify(empresaConfigRepository, times(1)).save(any(EmpresaConfig.class));
    }

    @Test
    @DisplayName("Debe guardar y actualizar la configuracion fiscal con modo DIRECTO_SUNAT normalizado a PROPIO y webhook")
    void testSaveOrUpdateConfig() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaMock));
        when(empresaConfigRepository.findByEmpresaId(empresaId)).thenReturn(Optional.empty());
        when(empresaConfigRepository.save(any(EmpresaConfig.class))).thenAnswer(inv -> {
            EmpresaConfig c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        EmpresaConfigRequest request = EmpresaConfigRequest.builder()
                .modoEmision("DIRECTO_SUNAT")
                .envioAsincrono(false)
                .webhookUrl("https://mi-erp.com/webhook")
                .tipoCertificado("PFX")
                .certificado("CERT_BASE64")
                .certificadoPass("Clave123")
                .userSol("MODDATOS")
                .passSol("moddatos")
                .numeroCuentaDetraccion("00-045-123456")
                .build();

        EmpresaConfigResponse response = empresaTenantService.saveOrUpdateConfig(request);

        assertNotNull(response);
        assertEquals("PROPIO", response.getModoEmision());
        assertFalse(response.getEnvioAsincrono());
        assertEquals("https://mi-erp.com/webhook", response.getWebhookUrl());
        assertEquals("PFX", response.getTipoCertificado());
        assertTrue(response.isHasCertificado());
        assertEquals("MODDATOS", response.getUserSol());
        assertEquals("00-045-123456", response.getNumeroCuentaDetraccion());
    }

    @Test
    @DisplayName("Debe actualizar el entorno fiscal normalizado a mayusculas")
    void testUpdateEnvironment() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaMock));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));

        EmpresaResponse response = empresaTenantService.updateEnvironment("beta");

        assertNotNull(response);
        assertEquals("BETA", response.getEntorno());
    }

    @Test
    @DisplayName("Debe actualizar datos comerciales de la empresa")
    void testUpdateEmpresa() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaMock));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));

        EmpresaTenantUpdateRequest request = EmpresaTenantUpdateRequest.builder()
                .razonSocial("NUEVA RAZON SOCIAL S.A.C.")
                .direccionFiscal("CALLE NUEVA 123")
                .incluidoTributo(true)
                .entorno("produccion")
                .build();

        EmpresaResponse response = empresaTenantService.updateEmpresa(request);

        assertNotNull(response);
        assertEquals("NUEVA RAZON SOCIAL S.A.C.", response.getRazonSocial());
        assertEquals("CALLE NUEVA 123", response.getDireccionFiscal());
        assertEquals("PRODUCCION", response.getEntorno());
    }

    @Test
    @DisplayName("Debe procesar y almacenar el certificado digital y actualizar la configuración fiscal")
    void testUploadCertificate() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaMock));
        when(empresaConfigRepository.findByEmpresaId(empresaId)).thenReturn(Optional.of(
                EmpresaConfig.builder().empresa(empresaMock).build()
        ));
        when(empresaConfigRepository.save(any(EmpresaConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        org.springframework.web.multipart.MultipartFile mockFile = mock(org.springframework.web.multipart.MultipartFile.class);
        com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse mockCertResponse =
                com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse.builder()
                        .ruc("20100070970")
                        .sujeto("CN=TU EMPRESA")
                        .emisor("CN=LLAMA PE")
                        .formatoOriginal("PEM")
                        .rutaAlmacenamiento("tenants/20100070970/certificates/certificate.pfx")
                        .diasRestantes(365L)
                        .vencido(false)
                        .build();

        when(certificadoDigitalService.procesarYGuardar(mockFile, "12345678", "20100070970"))
                .thenReturn(mockCertResponse);

        com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse result =
                empresaTenantService.uploadCertificate(mockFile, "12345678");

        assertNotNull(result);
        assertEquals("20100070970", result.getRuc());
        assertEquals("tenants/20100070970/certificates/certificate.pfx", result.getRutaAlmacenamiento());
        verify(empresaConfigRepository, times(1)).save(any(EmpresaConfig.class));
    }

    @Test
    @DisplayName("Debe obtener la información del certificado digital existente")
    void testGetCertificateInfo() {
        when(empresaRepository.findAll()).thenReturn(List.of(empresaMock));
        EmpresaConfig config = EmpresaConfig.builder()
                .empresa(empresaMock)
                .tipoCertificado("PFX")
                .certificado("tenants/20100070970/certificates/certificate.pfx")
                .certificadoPass("12345678")
                .build();
        when(empresaConfigRepository.findByEmpresaId(empresaId)).thenReturn(Optional.of(config));

        com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse mockCertResponse =
                com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse.builder()
                        .ruc("20100070970")
                        .sujeto("CN=TU EMPRESA")
                        .emisor("CN=LLAMA PE")
                        .formatoOriginal("PFX")
                        .rutaAlmacenamiento("tenants/20100070970/certificates/certificate.pfx")
                        .diasRestantes(200L)
                        .vencido(false)
                        .build();

        when(certificadoDigitalService.obtenerMetadatos(config.getCertificado(), config.getCertificadoPass(), "20100070970"))
                .thenReturn(mockCertResponse);

        com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse result =
                empresaTenantService.getCertificateInfo();

        assertNotNull(result);
        assertEquals("20100070970", result.getRuc());
        assertFalse(result.getVencido());
    }
}
