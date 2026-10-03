package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.LineaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.DocumentoRepositoryPort;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.template.model.PlantillaImpresion;
import com.s1nt4xSystem.facturacion_sunat.modules.core.template.repository.PlantillaImpresionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImpresionComprobanteServiceTest {

    @Mock
    private DocumentoRepositoryPort documentoRepositoryPort;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private SucursalRepository sucursalRepository;

    @Mock
    private PlantillaImpresionRepository plantillaImpresionRepository;

    private QrCodeGeneratorService qrCodeGeneratorService;
    private ObjectMapper objectMapper;
    private ImpresionComprobanteService impresionComprobanteService;

    private UUID documentoId;
    private UUID empresaId;
    private UUID sucursalId;
    private ComprobanteFiscal comprobanteMock;
    private Empresa empresaMock;
    private Sucursal sucursalMock;

    @BeforeEach
    void setUp() {
        qrCodeGeneratorService = new QrCodeGeneratorService();
        objectMapper = new ObjectMapper();
        impresionComprobanteService = new ImpresionComprobanteService(
                documentoRepositoryPort,
                empresaRepository,
                sucursalRepository,
                plantillaImpresionRepository,
                qrCodeGeneratorService,
                objectMapper
        );

        documentoId = UUID.randomUUID();
        empresaId = UUID.randomUUID();
        sucursalId = UUID.randomUUID();

        empresaMock = Empresa.builder()
                .id(empresaId)
                .ruc("20123456789")
                .razonSocial("INVERSIONES TECH S.A.C.")
                .direccionFiscal("Av. Los Pinos 123, Lima")
                .build();

        sucursalMock = Sucursal.builder()
                .id(sucursalId)
                .codigo("0000")
                .nombreSucursal("Casa Matriz")
                .direccion("Av. Los Pinos 123, Lima")
                .telefono("01-4445555")
                .build();

        LineaComprobante item1 = LineaComprobante.builder()
                .item(1)
                .codigoProducto("PROD-01")
                .descripcion("Mouse Óptico USB")
                .unidadMedida("NIU")
                .cantidad(new BigDecimal("2.00"))
                .valorUnitario(new BigDecimal("21.19"))
                .precioUnitario(new BigDecimal("25.00"))
                .subtotal(new BigDecimal("42.38"))
                .total(new BigDecimal("50.00"))
                .build();

        comprobanteMock = ComprobanteFiscal.builder()
                .id(documentoId)
                .empresaId(empresaId)
                .sucursalId(sucursalId)
                .tipoComprobante(TipoComprobante.BOLETA)
                .serie("B001")
                .numero(45)
                .fechaEmision(LocalDateTime.now())
                .moneda("PEN")
                .formaPago("CONTADO")
                .clienteTipoDoc("1")
                .clienteNumeroDoc("76543210")
                .clienteNombre("JUAN PEREZ")
                .clienteDireccion("Calle Luna 456")
                .subtotal(new BigDecimal("42.38"))
                .totalTributos(new BigDecimal("7.62"))
                .total(new BigDecimal("50.00"))
                .hashCpe("abc123hashCpe==")
                .detalles(List.of(item1))
                .build();
    }

    @Test
    @DisplayName("Debe generar HTML del Ticket con medidas @page y auto-impresión window.print()")
    void generarTicketHtml_exito() {
        when(documentoRepositoryPort.buscarPorId(documentoId)).thenReturn(Optional.of(comprobanteMock));
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaMock));
        when(sucursalRepository.findById(sucursalId)).thenReturn(Optional.of(sucursalMock));
        when(plantillaImpresionRepository.resolverPlantilla(eq(empresaId), eq(sucursalId), eq("TICKET_80")))
                .thenReturn(Optional.of(PlantillaImpresion.builder()
                        .tipoFormato("TICKET_80")
                        .layout("{\"formato\": {\"ancho_total_mm\": 80.0, \"margen_lateral_mm\": 2.0}}")
                        .build()));

        String html = impresionComprobanteService.generarTicketHtml(documentoId, "TICKET_80");

        assertNotNull(html);
        assertTrue(html.contains("size: 80.0mm auto;"), "Debe contener el tamaño del papel térmico de 80mm");
        assertTrue(html.contains("INVERSIONES TECH S.A.C."), "Debe contener la razón social");
        assertTrue(html.contains("B001-00000045"), "Debe contener serie y correlativo formateado");
        assertTrue(html.contains("Mouse Óptico USB"), "Debe contener la descripción del ítem");
        assertTrue(html.contains("data:image/png;base64,"), "Debe contener el QR generado en Base64");
        assertTrue(html.contains("window.print()"), "Debe contener la instrucción de auto-impresión");
    }

    @Test
    @DisplayName("Debe generar PDF de Ticket continuo de 80mm con firma mágica %PDF-")
    void generarPdfTicket_exito() {
        when(documentoRepositoryPort.buscarPorId(documentoId)).thenReturn(Optional.of(comprobanteMock));
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaMock));
        when(sucursalRepository.findById(sucursalId)).thenReturn(Optional.of(sucursalMock));

        byte[] pdfBytes = impresionComprobanteService.generarPdf(documentoId, "TICKET_80");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header, "El archivo debe iniciar con la cabecera mágica de PDF");
    }

    @Test
    @DisplayName("Debe generar PDF A4 con estructura fiscal formal")
    void generarPdfA4_exito() {
        when(documentoRepositoryPort.buscarPorId(documentoId)).thenReturn(Optional.of(comprobanteMock));
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaMock));
        when(sucursalRepository.findById(sucursalId)).thenReturn(Optional.of(sucursalMock));

        byte[] pdfBytes = impresionComprobanteService.generarPdf(documentoId, "A4");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header, "El archivo debe iniciar con la cabecera mágica de PDF");
    }

    @Test
    @DisplayName("Debe resolver dinámicamente los enlaces de impresión con HTML completo y URLs públicas")
    void resolverEnlacesImpresion_dinamico() {
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaMock));
        when(documentoRepositoryPort.buscarPorId(documentoId)).thenReturn(Optional.of(comprobanteMock));
        when(sucursalRepository.findById(sucursalId)).thenReturn(Optional.of(sucursalMock));

        PlantillaImpresion pTicket = PlantillaImpresion.builder()
                .tipoFormato("TICKET_80")
                .estado(true)
                .build();
        PlantillaImpresion pA4 = PlantillaImpresion.builder()
                .tipoFormato("A4")
                .estado(true)
                .build();

        when(plantillaImpresionRepository.findByEmpresaIdAndEstadoTrue(empresaId))
                .thenReturn(List.of(pTicket, pA4));

        Map<String, String> enlaces = impresionComprobanteService.resolverEnlacesImpresion(
                documentoId, empresaId, sucursalId, "tenants/20123456789/xml/doc.xml", null);

        assertNotNull(enlaces);
        assertTrue(enlaces.containsKey("html_TICKET_80"));
        assertTrue(enlaces.get("html_TICKET_80").contains("<!DOCTYPE html>"), "html_TICKET_80 debe contener el código HTML listo para imprimir");
        assertTrue(enlaces.containsKey("pdf_TICKET_80"));
        assertEquals("/api/v1/public/comprobantes/20123456789/" + documentoId + "/pdf?formato=TICKET_80", enlaces.get("pdf_TICKET_80"));

        assertTrue(enlaces.containsKey("html_A4"));
        assertTrue(enlaces.get("html_A4").contains("<!DOCTYPE html>"), "html_A4 debe contener el código HTML listo para imprimir");
        assertTrue(enlaces.containsKey("pdf_A4"));
        assertEquals("/api/v1/public/comprobantes/20123456789/" + documentoId + "/pdf?formato=A4", enlaces.get("pdf_A4"));

        assertTrue(enlaces.containsKey("xml_url"));
        assertEquals("/api/v1/tenant/comprobantes/" + documentoId + "/xml", enlaces.get("xml_url"));
        assertFalse(enlaces.containsKey("cdr_url"), "No debe tener cdr_url si aún no responde SUNAT");
    }

    @Test
    @DisplayName("Debe incluir enlace protegido de cdr_url cuando el comprobante ya tiene CDR")
    void resolverEnlacesImpresion_conCdr() {
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresaMock));
        when(documentoRepositoryPort.buscarPorId(documentoId)).thenReturn(Optional.of(comprobanteMock));
        when(sucursalRepository.findById(sucursalId)).thenReturn(Optional.of(sucursalMock));
        when(plantillaImpresionRepository.findByEmpresaIdAndEstadoTrue(empresaId)).thenReturn(List.of());

        Map<String, String> enlaces = impresionComprobanteService.resolverEnlacesImpresion(
                documentoId, empresaId, sucursalId, "tenants/20123456789/xml/doc.xml", "tenants/20123456789/cdr/R-doc.zip");

        assertNotNull(enlaces);
        assertTrue(enlaces.containsKey("cdr_url"));
        assertEquals("/api/v1/tenant/comprobantes/" + documentoId + "/cdr", enlaces.get("cdr_url"));
    }
}
