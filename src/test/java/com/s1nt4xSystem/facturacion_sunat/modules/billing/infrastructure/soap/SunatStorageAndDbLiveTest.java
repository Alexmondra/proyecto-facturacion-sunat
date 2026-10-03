package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.soap;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaConfigRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.CertificadoDigitalServiceImpl;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service.ProcesadorComprobanteElectronicoService;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.DocumentoRepositoryPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.adapter.DocumentoSunatPersistenceAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity.DocumentoArchivoEntity;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.entity.DocumentoSunatEntity;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.repository.DocumentoArchivoJpaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.persistence.repository.DocumentoSunatJpaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.signature.adapter.XmlDSigFirmaAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.soap.adapter.SunatSoapClientAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.storage.adapter.LocalDiskArchivoStorageAdapter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.xml.adapter.FreeMarkerXmlGeneratorAdapter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SunatStorageAndDbLiveTest {

    @Test
    void testProcesadorGuardaCdrEnStorageYPersistence() {
        int randomCorrelativo = 1000 + new Random().nextInt(80000);
        String serie = "B001";
        String ruc = "20000000001";

        Empresa empresa = Empresa.builder()
                .id(UUID.randomUUID())
                .ruc(ruc)
                .razonSocial("EMPRESA DE PRUEBA SUNAT S.A.")
                .direccionFiscal("AV. GARCILASO DE LA VEGA 1456, LIMA")
                .entorno("beta")
                .build();

        Sucursal sucursal = Sucursal.builder()
                .id(UUID.randomUUID())
                .codigo("0000")
                .nombreSucursal("Matriz")
                .ubigeo("150101")
                .direccion("AV. GARCILASO DE LA VEGA 1456")
                .build();

        EmpresaConfig config = EmpresaConfig.builder()
                .id(UUID.randomUUID())
                .empresa(empresa)
                .userSol("MODDATOS")
                .passSol("moddatos")
                .certificado("tenants/20000000001/certificates/certificate.pfx")
                .certificadoPass("1234")
                .build();

        UUID docId = UUID.randomUUID();

        LineaComprobante linea = LineaComprobante.builder()
                .item(1)
                .codigoProducto("PROD-01")
                .descripcion("VENTA DE MERCADERIA")
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

        ComprobanteFiscal comprobante = ComprobanteFiscal.builder()
                .id(docId)
                .tipoComprobante(TipoComprobante.BOLETA)
                .serie(serie)
                .numero(randomCorrelativo)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .tipoOperacion("0101")
                .clienteTipoDoc("1")
                .clienteNumeroDoc("12345678")
                .clienteNombre("JUAN PEREZ")
                .clienteDireccion("JR LIMA 123")
                .formaPago("CONTADO")
                .subtotal(new BigDecimal("100.00"))
                .totalTributos(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .detalles(List.of(linea))
                .totalesAfectacion(List.of(
                        TotalesAfectacion.builder()
                                .tipoAfectacionCodigo("10")
                                .tipoTotal("GRAVADO")
                                .baseImponible(new BigDecimal("100.00"))
                                .montoTributo(new BigDecimal("18.00"))
                                .montoTotal(new BigDecimal("118.00"))
                                .build()
                ))
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

        // Configurar dependencias reales
        FreeMarkerXmlGeneratorAdapter xmlGen = new FreeMarkerXmlGeneratorAdapter();
        CertificadoDigitalServiceImpl certService = new CertificadoDigitalServiceImpl("storage");
        XmlDSigFirmaAdapter firmaAdapter = new XmlDSigFirmaAdapter(certService);
        SunatSoapClientAdapter soapClient = new SunatSoapClientAdapter(null, null, null);
        LocalDiskArchivoStorageAdapter storageAdapter = new LocalDiskArchivoStorageAdapter("storage");

        // Mocks de repositorio para aislamiento
        DocumentoSunatJpaRepository sunatRepo = mock(DocumentoSunatJpaRepository.class);
        DocumentoArchivoJpaRepository archivoRepo = mock(DocumentoArchivoJpaRepository.class);
        when(archivoRepo.saveAndFlush(any())).thenAnswer(inv -> {
            DocumentoArchivoEntity ent = inv.getArgument(0);
            ent.setId(UUID.randomUUID());
            return ent;
        });
        when(sunatRepo.saveAndFlush(any())).thenAnswer(inv -> {
            DocumentoSunatEntity ent = inv.getArgument(0);
            ent.setId(UUID.randomUUID());
            return ent;
        });
        DocumentoSunatPersistenceAdapter persistenceAdapter = new DocumentoSunatPersistenceAdapter(sunatRepo, archivoRepo);

        DocumentoRepositoryPort docRepoPort = mock(DocumentoRepositoryPort.class);
        EmpresaConfigRepository configRepo = mock(EmpresaConfigRepository.class);
        when(configRepo.findByEmpresaId(empresa.getId())).thenReturn(Optional.of(config));

        ProcesadorComprobanteElectronicoService procesador = new ProcesadorComprobanteElectronicoService(
                xmlGen,
                firmaAdapter,
                soapClient,
                storageAdapter,
                persistenceAdapter,
                docRepoPort,
                configRepo
        );

        // Ejecutar procesamiento completo
        ComprobanteFiscal resultado = procesador.procesarFirmaYEnvio(comprobante, empresa, sucursal);

        assertNotNull(resultado);
        System.out.println("=== RESULTADO PROCESADOR ===");
        System.out.println("Estado SUNAT: " + resultado.getEstadoSunat());
        System.out.println("Código SUNAT: " + resultado.getCodigoSunat());
        System.out.println("Mensaje SUNAT: " + resultado.getMensajeSunat());
        System.out.println("XML URL: " + resultado.getXmlUrl());
        System.out.println("CDR URL: " + resultado.getCdrUrl());

        assertEquals("ACEPTADO", resultado.getEstadoSunat());
        assertEquals("0", resultado.getCodigoSunat());
        assertNotNull(resultado.getCdrUrl());

        // Verificar que los archivos físicos EXISTEN en el storage en disco (XML firmado y CDR ZIP oficial)
        String baseNombre = String.format("%s-03-%s-%08d", ruc, serie, randomCorrelativo);
        Path xmlPath = Paths.get("storage/tenants", ruc, "xml", baseNombre + ".xml");
        Path cdrZipPath = Paths.get("storage/tenants", ruc, "cdr", "R-" + baseNombre + ".zip");

        assertTrue(Files.exists(xmlPath), "El archivo XML firmado debe existir físicamente en disco");
        assertTrue(Files.exists(cdrZipPath), "El archivo CDR ZIP debe existir físicamente en disco");

        try {
            assertTrue(Files.size(xmlPath) > 0, "El XML firmado no debe ser de 0 bytes");
            assertTrue(Files.size(cdrZipPath) > 0, "El CDR ZIP no debe ser de 0 bytes");
            System.out.println("Tamaño XML: " + Files.size(xmlPath) + " bytes");
            System.out.println("Tamaño CDR ZIP: " + Files.size(cdrZipPath) + " bytes");
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Verificar que se invocó persistencia para XML firmado y CDR ZIP oficial
        verify(archivoRepo, times(2)).saveAndFlush(any());
        verify(sunatRepo, times(1)).saveAndFlush(any());
    }
}
