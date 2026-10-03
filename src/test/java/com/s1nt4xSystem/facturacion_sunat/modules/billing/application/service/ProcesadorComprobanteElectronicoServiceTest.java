package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.dto.CertificadoDigitalResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaConfigRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.CertificadoDigitalService;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.CertificadoDigitalServiceImpl;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.adapter.DocumentoSunatPersistenceAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity.DocumentoArchivoEntity;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity.DocumentoSunatEntity;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.repository.DocumentoArchivoJpaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.repository.DocumentoSunatJpaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.signature.adapter.XmlDSigFirmaAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.storage.adapter.LocalDiskArchivoStorageAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.xml.adapter.FreeMarkerXmlGeneratorAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesadorComprobanteElectronicoServiceTest {

    @Mock
    private SunatSoapPort sunatSoapPort;

    @Mock
    private DocumentoRepositoryPort documentoRepositoryPort;

    @Mock
    private EmpresaConfigRepository empresaConfigRepository;

    @Mock
    private DocumentoSunatJpaRepository sunatJpaRepository;

    @Mock
    private DocumentoArchivoJpaRepository archivoJpaRepository;

    private ProcesadorComprobanteElectronicoService procesadorService;

    private CertificadoDigitalService certificadoDigitalService;
    private GeneradorXmlPort generadorXmlPort;
    private FirmaDigitalPort firmaDigitalPort;
    private DocumentoArchivoStoragePort documentoArchivoStoragePort;
    private DocumentoSunatPersistencePort documentoSunatPersistencePort;

    private Empresa empresa;
    private EmpresaConfig config;
    private Sucursal sucursal;
    private ComprobanteFiscal comprobante;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        String ruc = "20000000001";
        String password = "demoPassword123";

        // Almacenamiento y certificado real de prueba
        certificadoDigitalService = new CertificadoDigitalServiceImpl(tempDir.toString());
        documentoArchivoStoragePort = new LocalDiskArchivoStorageAdapter(tempDir.toString());
        generadorXmlPort = new FreeMarkerXmlGeneratorAdapter();
        firmaDigitalPort = new XmlDSigFirmaAdapter(certificadoDigitalService);
        documentoSunatPersistencePort = new DocumentoSunatPersistenceAdapter(sunatJpaRepository, archivoJpaRepository);

        byte[] pemBytes;
        try (InputStream is = getClass().getResourceAsStream("/certificates/certificado_demo.pem")) {
            assertNotNull(is);
            pemBytes = is.readAllBytes();
        }

        MockMultipartFile file = new MockMultipartFile("file", "demo.pem", "application/x-pem-file", pemBytes);
        CertificadoDigitalResponse certResp = certificadoDigitalService.procesarYGuardar(file, password, ruc);

        procesadorService = new ProcesadorComprobanteElectronicoService(
                generadorXmlPort,
                firmaDigitalPort,
                sunatSoapPort,
                documentoArchivoStoragePort,
                documentoSunatPersistencePort,
                documentoRepositoryPort,
                empresaConfigRepository
        );

        UUID empresaId = UUID.randomUUID();
        empresa = Empresa.builder()
                .id(empresaId)
                .ruc(ruc)
                .razonSocial("EMPRESA INTEGRACION S.A.C.")
                .direccionFiscal("AV. TEST 123")
                .entorno("dev")
                .build();

        config = EmpresaConfig.builder()
                .id(UUID.randomUUID())
                .empresa(empresa)
                .userSol("MODDATOS")
                .passSol("moddatos")
                .certificado(certResp.getRutaAlmacenamiento())
                .certificadoPass(password)
                .build();

        sucursal = Sucursal.builder()
                .id(UUID.randomUUID())
                .codigo("0000")
                .nombreSucursal("Casa Matriz")
                .build();

        UUID docId = UUID.randomUUID();
        LineaComprobante linea = LineaComprobante.builder()
                .item(1)
                .codigoProducto("PROD-01")
                .descripcion("LICENCIA SAAS")
                .cantidad(BigDecimal.ONE)
                .unidadMedida("NIU")
                .valorUnitario(new BigDecimal("100.00"))
                .precioUnitario(new BigDecimal("118.00"))
                .tipoAfectacion(TipoAfectacionIgv.GRAVADO_OPERACION_ONEROSA)
                .subtotal(new BigDecimal("100.00"))
                .totalTributos(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .tributos(List.of(
                        TributoLinea.builder()
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("100.00"))
                                .porcentaje(new BigDecimal("18.00"))
                                .monto(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        comprobante = ComprobanteFiscal.builder()
                .id(docId)
                .tipoComprobante(TipoComprobante.FACTURA)
                .serie("F001")
                .numero(1)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .clienteTipoDoc("6")
                .clienteNumeroDoc("20555555551")
                .clienteNombre("CLIENTE FINAL S.A.")
                .formaPago("CONTADO")
                .subtotal(new BigDecimal("100.00"))
                .totalTributos(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .detalles(List.of(linea))
                .tributosGlobales(List.of(
                        TributoLinea.builder()
                                .codigoTributo("1000")
                                .nombreTributo("IGV")
                                .tipoTributo("VAT")
                                .baseImponible(new BigDecimal("100.00"))
                                .monto(new BigDecimal("18.00"))
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("Debe ejecutar el flujo completo síncrono: generar XML, firmar XMLDSig, enviar a SUNAT, guardar CDR y actualizar estado")
    void testFlujoCompletoFirmaYEnvioSunat() {
        config.setEnvioAsincrono(false);
        when(empresaConfigRepository.findByEmpresaId(empresa.getId())).thenReturn(Optional.of(config));

        // Mockear respuesta SUNAT
        SunatSoapResult mockSoapResult = SunatSoapResult.builder()
                .response(DocumentoSunatResponse.builder()
                        .estadoSunat("ACEPTADO")
                        .codigoRespuestaSunat("0")
                        .mensajeSunat("La Factura F001-00000001 ha sido aceptada")
                        .fechaEnvio(LocalDateTime.now())
                        .fechaRespuesta(LocalDateTime.now())
                        .build())
                .cdrZip("fake_cdr_zip_bytes".getBytes(StandardCharsets.UTF_8))
                .cdrNombreArchivo("R-20000000001-01-F001-00000001.zip")
                .build();

        when(sunatSoapPort.enviarComprobante(any(), eq("20000000001-01-F001-00000001"), eq(empresa), eq(config)))
                .thenReturn(mockSoapResult);

        when(sunatJpaRepository.findByDocumentoId(comprobante.getId()))
                .thenReturn(Optional.of(DocumentoSunatEntity.builder().documentoId(comprobante.getId()).build()));

        when(sunatJpaRepository.saveAndFlush(any(DocumentoSunatEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(archivoJpaRepository.saveAndFlush(any(DocumentoArchivoEntity.class)))
                .thenAnswer(inv -> {
                    DocumentoArchivoEntity entity = inv.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        // Ejecución
        ComprobanteFiscal resultado = procesadorService.procesarFirmaYEnvio(comprobante, empresa, sucursal);

        assertNotNull(resultado);
        assertEquals("ACEPTADO", resultado.getEstadoSunat());
        assertEquals("0", resultado.getCodigoSunat());
        assertEquals("La Factura F001-00000001 ha sido aceptada", resultado.getMensajeSunat());

        // Verificar que se guardó el hash_cpe y se mantuvo el estado_interno como REGISTRADO
        verify(documentoRepositoryPort, times(1))
                .actualizarHashYEstado(eq(comprobante.getId()), any(String.class), eq("REGISTRADO"));

        // Verificar que se persistió la respuesta de SUNAT en documento_sunat
        verify(sunatJpaRepository, times(1)).saveAndFlush(any(DocumentoSunatEntity.class));

        // Verificar que se registraron exactamente 2 archivos: XML firmado y CDR ZIP oficial
        verify(archivoJpaRepository, times(2)).saveAndFlush(any(DocumentoArchivoEntity.class));
    }

    @Test
    @DisplayName("Debe ejecutar el flujo asíncrono: firmar XML, guardar XML y responder inmediatamente")
    void testFlujoAsincrono() {
        config.setEnvioAsincrono(true);
        when(empresaConfigRepository.findByEmpresaId(empresa.getId())).thenReturn(Optional.of(config));

        when(archivoJpaRepository.saveAndFlush(any(DocumentoArchivoEntity.class)))
                .thenAnswer(inv -> {
                    DocumentoArchivoEntity entity = inv.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        ComprobanteFiscal resultado = procesadorService.procesarFirmaYEnvio(comprobante, empresa, sucursal);

        assertNotNull(resultado);
        assertNotNull(resultado.getHashCpe());
        assertNotNull(resultado.getXmlUrl());
        // En modo asíncrono no espera a SUNAT en el hilo principal
        assertEquals("REGISTRADO", resultado.getEstadoInterno());
        verify(documentoRepositoryPort, times(1))
                .actualizarHashYEstado(eq(comprobante.getId()), any(String.class), eq("REGISTRADO"));
    }
}
