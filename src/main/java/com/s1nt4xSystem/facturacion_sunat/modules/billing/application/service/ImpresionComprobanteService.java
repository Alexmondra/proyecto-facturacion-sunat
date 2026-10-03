package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.LineaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.DocumentoRepositoryPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.xml.util.NumeroALetrasUtil;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.template.model.PlantillaImpresion;
import com.s1nt4xSystem.facturacion_sunat.modules.core.template.repository.PlantillaImpresionRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class ImpresionComprobanteService {

    private final DocumentoRepositoryPort documentoRepositoryPort;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final PlantillaImpresionRepository plantillaImpresionRepository;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public ImpresionComprobanteService(
            DocumentoRepositoryPort documentoRepositoryPort,
            EmpresaRepository empresaRepository,
            SucursalRepository sucursalRepository,
            PlantillaImpresionRepository plantillaImpresionRepository,
            QrCodeGeneratorService qrCodeGeneratorService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.documentoRepositoryPort = documentoRepositoryPort;
        this.empresaRepository = empresaRepository;
        this.sucursalRepository = sucursalRepository;
        this.plantillaImpresionRepository = plantillaImpresionRepository;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final float MM_TO_PT = 72f / 25.4f; // 2.8346457f

    @Transactional(readOnly = true)
    public Map<String, String> resolverEnlacesImpresion(UUID documentoId, UUID empresaId, UUID sucursalId, String xmlUrl, String cdrUrl) {
        Map<String, String> enlaces = new LinkedHashMap<>();

        String ruc = empresaRepository.findById(empresaId)
                .map(Empresa::getRuc)
                .orElse("00000000000");

        List<PlantillaImpresion> plantillas = plantillaImpresionRepository.findByEmpresaIdAndEstadoTrue(empresaId);
        if (plantillas.isEmpty()) {
            // Valores por defecto universales si aún no hay plantillas activas registradas
            enlaces.put("html_TICKET_80", generarHtml(documentoId, "TICKET_80"));
            enlaces.put("pdf_TICKET_80", "/api/v1/public/comprobantes/" + ruc + "/" + documentoId + "/pdf?formato=TICKET_80");
            enlaces.put("html_A4", generarHtml(documentoId, "A4"));
            enlaces.put("pdf_A4", "/api/v1/public/comprobantes/" + ruc + "/" + documentoId + "/pdf?formato=A4");
        } else {
            for (PlantillaImpresion p : plantillas) {
                String fmt = p.getTipoFormato() != null ? p.getTipoFormato().trim().toUpperCase() : "A4";
                enlaces.put("html_" + fmt, generarHtml(documentoId, fmt));
                enlaces.put("pdf_" + fmt, "/api/v1/public/comprobantes/" + ruc + "/" + documentoId + "/pdf?formato=" + fmt);
            }
        }

        if (xmlUrl != null && !xmlUrl.isBlank()) {
            enlaces.put("xml_url", "/api/v1/tenant/comprobantes/" + documentoId + "/xml");
        }
        if (cdrUrl != null && !cdrUrl.isBlank()) {
            enlaces.put("cdr_url", "/api/v1/tenant/comprobantes/" + documentoId + "/cdr");
        }

        return enlaces;
    }

    @Transactional(readOnly = true)
    public String generarHtml(UUID documentoId, String tipoFormato) {
        String formato = (tipoFormato != null && !tipoFormato.isBlank()) ? tipoFormato.trim().toUpperCase() : "TICKET_80";
        if (formato.startsWith("TICKET")) {
            return generarTicketHtml(documentoId, formato);
        } else if ("A4".equalsIgnoreCase(formato)) {
            return generarA4Html(documentoId);
        } else {
            return generarTicketHtml(documentoId, formato);
        }
    }

    @Transactional(readOnly = true)
    public String generarTicketHtml(UUID documentoId, String tipoFormato) {
        ComprobanteFiscal doc = documentoRepositoryPort.buscarPorId(documentoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, documentoId));

        Empresa empresa = empresaRepository.findById(doc.getEmpresaId())
                .orElse(Empresa.builder().ruc("00000000000").razonSocial("EMPRESA").build());
        Sucursal sucursal = sucursalRepository.findById(doc.getSucursalId())
                .orElse(Sucursal.builder().codigo("0000").nombreSucursal("Casa Matriz").direccion(empresa.getDireccionFiscal()).build());

        String formatoBuscado = (tipoFormato != null && !tipoFormato.isBlank()) ? tipoFormato.toUpperCase() : "TICKET_80";
        Optional<PlantillaImpresion> plantillaOpt = plantillaImpresionRepository.resolverPlantilla(
                doc.getEmpresaId(), doc.getSucursalId(), formatoBuscado);

        double anchoTotalMm = 80.0;
        double margenLateralMm = 2.0;

        if (plantillaOpt.isPresent() && plantillaOpt.get().getLayout() != null) {
            try {
                JsonNode root = objectMapper.readTree(plantillaOpt.get().getLayout());
                JsonNode fmtNode = root.path("formato");
                if (fmtNode.has("ancho_total_mm")) {
                    anchoTotalMm = fmtNode.path("ancho_total_mm").asDouble(80.0);
                }
                if (fmtNode.has("margen_lateral_mm")) {
                    margenLateralMm = fmtNode.path("margen_lateral_mm").asDouble(2.0);
                }
            } catch (Exception e) {
                log.warn("No se pudo parsear el layout de la plantilla, usando medidas estándar: {}", e.getMessage());
            }
        } else if ("TICKET_58".equalsIgnoreCase(formatoBuscado)) {
            anchoTotalMm = 58.0;
            margenLateralMm = 1.5;
        }

        double anchoUtilMm = anchoTotalMm - (margenLateralMm * 2);

        String qrSunatCadena = qrCodeGeneratorService.generarCadenaQrSunat(doc, empresa.getRuc());
        String qrBase64 = qrCodeGeneratorService.generarQrBase64(qrSunatCadena, 180, 180);
        String montoLetras = NumeroALetrasUtil.convertir(doc.getTotal(), doc.getMoneda());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"es\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <title>Ticket ").append(doc.getSerie()).append("-").append(doc.getNumero()).append("</title>\n");
        html.append("  <style>\n");
        html.append("    @page {\n");
        html.append(String.format(Locale.US, "      size: %.1fmm auto;\n", anchoTotalMm));
        html.append("      margin: 0;\n");
        html.append("    }\n");
        html.append("    * { box-sizing: border-box; }\n");
        html.append("    body {\n");
        html.append(String.format(Locale.US, "      width: %.1fmm;\n", anchoUtilMm));
        html.append(String.format(Locale.US, "      margin: 0 auto; padding: 2mm %.1fmm;\n", margenLateralMm));
        html.append("      font-family: 'Courier New', Courier, monospace;\n");
        html.append("      font-size: 11px;\n");
        html.append("      color: #000;\n");
        html.append("      line-height: 1.25;\n");
        html.append("    }\n");
        html.append("    .text-center { text-align: center; }\n");
        html.append("    .text-right { text-align: right; }\n");
        html.append("    .text-left { text-align: left; }\n");
        html.append("    .bold { font-weight: bold; }\n");
        html.append("    .linea-separadora { border-top: 1px dashed #000; margin: 4px 0; }\n");
        html.append("    .tabla-items { width: 100%; border-collapse: collapse; margin: 4px 0; }\n");
        html.append("    .tabla-items th { border-bottom: 1px dashed #000; padding: 2px 0; font-size: 10.5px; }\n");
        html.append("    .tabla-items td { padding: 2px 0; vertical-align: top; font-size: 10.5px; }\n");
        html.append("    .tabla-totales { width: 100%; border-collapse: collapse; margin-top: 4px; }\n");
        html.append("    .tabla-totales td { padding: 1px 0; font-size: 11px; }\n");
        html.append("    .caja-fiscal { border: 1px solid #000; padding: 4px; margin: 5px 0; text-align: center; }\n");
        html.append("    .qr-container { text-align: center; margin: 6px 0; }\n");
        html.append("    .qr-container img { width: 30mm; height: 30mm; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Cabecera Empresa
        html.append("  <div class=\"text-center\">\n");
        html.append("    <div class=\"bold\" style=\"font-size: 12.5px;\">").append(escaparHtml(empresa.getRazonSocial())).append("</div>\n");
        html.append("    <div class=\"bold\">RUC: ").append(empresa.getRuc()).append("</div>\n");
        if (sucursal.getDireccion() != null && !sucursal.getDireccion().isBlank()) {
            html.append("    <div>").append(escaparHtml(sucursal.getDireccion())).append("</div>\n");
        }
        if (sucursal.getTelefono() != null && !sucursal.getTelefono().isBlank()) {
            html.append("    <div>Telf: ").append(escaparHtml(sucursal.getTelefono())).append("</div>\n");
        }
        html.append("  </div>\n");

        // Caja Fiscal
        String descTipo = doc.getTipoComprobante() != null ? doc.getTipoComprobante().getDescripcion().toUpperCase() : "COMPROBANTE";
        html.append("  <div class=\"caja-fiscal\">\n");
        html.append("    <div class=\"bold\">").append(descTipo).append("</div>\n");
        html.append("    <div class=\"bold\" style=\"font-size: 13px;\">").append(doc.getSerie()).append("-").append(String.format("%08d", doc.getNumero())).append("</div>\n");
        html.append("  </div>\n");

        // Datos Cliente y Emisión
        html.append("  <div>\n");
        html.append("    <div><span class=\"bold\">Fecha:</span> ").append(doc.getFechaEmision() != null ? doc.getFechaEmision().format(FECHA_HORA_FMT) : "").append("</div>\n");
        html.append("    <div><span class=\"bold\">Doc. Cliente:</span> ").append(doc.getClienteNumeroDoc() != null ? doc.getClienteNumeroDoc() : "-").append("</div>\n");
        html.append("    <div><span class=\"bold\">Cliente:</span> ").append(escaparHtml(doc.getClienteNombre() != null ? doc.getClienteNombre() : "CLIENTES VARIOS")).append("</div>\n");
        if (doc.getClienteDireccion() != null && !doc.getClienteDireccion().isBlank()) {
            html.append("    <div><span class=\"bold\">Dirección:</span> ").append(escaparHtml(doc.getClienteDireccion())).append("</div>\n");
        }
        html.append("    <div><span class=\"bold\">Moneda:</span> ").append(doc.getMoneda()).append(" | <span class=\"bold\">Pago:</span> ").append(doc.getFormaPago()).append("</div>\n");
        html.append("  </div>\n");

        html.append("  <div class=\"linea-separadora\"></div>\n");

        // Tabla Ítems
        html.append("  <table class=\"tabla-items\">\n");
        html.append("    <thead>\n");
        html.append("      <tr>\n");
        html.append("        <th class=\"text-center\" style=\"width: 14%;\">Cant</th>\n");
        html.append("        <th class=\"text-left\" style=\"width: 48%;\">Descrip</th>\n");
        html.append("        <th class=\"text-right\" style=\"width: 18%;\">P.U.</th>\n");
        html.append("        <th class=\"text-right\" style=\"width: 20%;\">Total</th>\n");
        html.append("      </tr>\n");
        html.append("    </thead>\n");
        html.append("    <tbody>\n");
        for (LineaComprobante item : doc.getDetalles()) {
            BigDecimal pu = item.getPrecioUnitario() != null ? item.getPrecioUnitario() : item.getValorUnitario();
            html.append("      <tr>\n");
            html.append(String.format(Locale.US, "        <td class=\"text-center\">%.0f</td>\n", item.getCantidad()));
            html.append("        <td class=\"text-left\">").append(escaparHtml(item.getDescripcion())).append("</td>\n");
            html.append(String.format(Locale.US, "        <td class=\"text-right\">%.2f</td>\n", pu != null ? pu : BigDecimal.ZERO));
            html.append(String.format(Locale.US, "        <td class=\"text-right\">%.2f</td>\n", item.getTotal() != null ? item.getTotal() : BigDecimal.ZERO));
            html.append("      </tr>\n");
        }
        html.append("    </tbody>\n");
        html.append("  </table>\n");

        html.append("  <div class=\"linea-separadora\"></div>\n");

        // Totales
        BigDecimal opGravada = doc.getSubtotal() != null ? doc.getSubtotal() : BigDecimal.ZERO;
        BigDecimal opIgv = doc.getTotalTributos() != null ? doc.getTotalTributos() : BigDecimal.ZERO;
        BigDecimal opTotal = doc.getTotal() != null ? doc.getTotal() : BigDecimal.ZERO;

        html.append("  <table class=\"tabla-totales\">\n");
        html.append(String.format(Locale.US, "    <tr><td class=\"text-right\">Op. Gravada:</td><td class=\"text-right bold\" style=\"width: 35%%;\">%s %.2f</td></tr>\n", doc.getMoneda(), opGravada));
        html.append(String.format(Locale.US, "    <tr><td class=\"text-right\">I.G.V. (18%%):</td><td class=\"text-right bold\">%s %.2f</td></tr>\n", doc.getMoneda(), opIgv));
        html.append(String.format(Locale.US, "    <tr style=\"font-size: 13px;\"><td class=\"text-right bold\">TOTAL:</td><td class=\"text-right bold\">%s %.2f</td></tr>\n", doc.getMoneda(), opTotal));
        html.append("  </table>\n");

        if (!montoLetras.isBlank()) {
            html.append("  <div style=\"margin-top: 4px; font-size: 10px;\"><span class=\"bold\">SON:</span> ").append(escaparHtml(montoLetras)).append("</div>\n");
        }

        // QR y Datos SUNAT
        if (!qrBase64.isBlank()) {
            html.append("  <div class=\"qr-container\">\n");
            html.append("    <img src=\"").append(qrBase64).append("\" alt=\"QR CPE\" />\n");
            html.append("  </div>\n");
        }

        if (doc.getHashCpe() != null && !doc.getHashCpe().isBlank()) {
            html.append("  <div class=\"text-center\" style=\"font-size: 8.5px; word-break: break-all;\">\n");
            html.append("    <div><span class=\"bold\">Hash:</span> ").append(doc.getHashCpe()).append("</div>\n");
            html.append("  </div>\n");
        }

        html.append("  <div class=\"text-center\" style=\"margin-top: 5px; font-size: 9px;\">\n");
        html.append("    Representación Impresa del Comprobante Electrónico.<br>Consulte en www.sunat.gob.pe\n");
        html.append("    <div class=\"bold\" style=\"margin-top: 4px;\">¡Gracias por su compra!</div>\n");
        html.append("  </div>\n");

        // Script de impresión automática
        html.append("  <script>\n");
        html.append("    window.onload = function() {\n");
        html.append("      window.print();\n");
        html.append("    };\n");
        html.append("  </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    @Transactional(readOnly = true)
    public String generarA4Html(UUID documentoId) {
        ComprobanteFiscal doc = documentoRepositoryPort.buscarPorId(documentoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, documentoId));

        Empresa empresa = empresaRepository.findById(doc.getEmpresaId())
                .orElse(Empresa.builder().ruc("00000000000").razonSocial("EMPRESA").build());
        Sucursal sucursal = sucursalRepository.findById(doc.getSucursalId())
                .orElse(Sucursal.builder().codigo("0000").nombreSucursal("Casa Matriz").direccion(empresa.getDireccionFiscal()).build());

        String qrSunatCadena = qrCodeGeneratorService.generarCadenaQrSunat(doc, empresa.getRuc());
        String qrBase64 = qrCodeGeneratorService.generarQrBase64(qrSunatCadena, 180, 180);
        String montoLetras = NumeroALetrasUtil.convertir(doc.getTotal(), doc.getMoneda());
        String descTipo = doc.getTipoComprobante() != null ? doc.getTipoComprobante().getDescripcion().toUpperCase() : "COMPROBANTE";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"es\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <title>").append(descTipo).append(" ").append(doc.getSerie()).append("-").append(doc.getNumero()).append("</title>\n");
        html.append("  <style>\n");
        html.append("    @page { size: A4 portrait; margin: 15mm 12mm 15mm 12mm; }\n");
        html.append("    * { box-sizing: border-box; }\n");
        html.append("    body { font-family: Arial, Helvetica, sans-serif; font-size: 12px; color: #222; margin: 0; padding: 0; line-height: 1.4; }\n");
        html.append("    .text-center { text-align: center; }\n");
        html.append("    .text-right { text-align: right; }\n");
        html.append("    .text-left { text-align: left; }\n");
        html.append("    .bold { font-weight: bold; }\n");
        html.append("    .header-table { width: 100%; border-collapse: collapse; margin-bottom: 15px; }\n");
        html.append("    .header-table td { vertical-align: top; }\n");
        html.append("    .caja-fiscal-a4 { border: 2px solid #0d47a1; border-radius: 6px; padding: 12px; text-align: center; }\n");
        html.append("    .caja-fiscal-a4 .ruc { font-size: 15px; font-weight: bold; color: #0d47a1; }\n");
        html.append("    .caja-fiscal-a4 .tipo { font-size: 13px; font-weight: bold; margin: 6px 0; }\n");
        html.append("    .caja-fiscal-a4 .numero { font-size: 16px; font-weight: bold; color: #0d47a1; }\n");
        html.append("    .box-info { border: 1px solid #ccc; border-radius: 4px; padding: 10px; margin-bottom: 15px; width: 100%; border-collapse: collapse; }\n");
        html.append("    .box-info td { padding: 4px 6px; font-size: 11.5px; }\n");
        html.append("    .tabla-items-a4 { width: 100%; border-collapse: collapse; margin-bottom: 15px; }\n");
        html.append("    .tabla-items-a4 th { background-color: #0d47a1; color: #ffffff; padding: 6px 8px; font-size: 11.5px; text-align: center; }\n");
        html.append("    .tabla-items-a4 td { padding: 6px 8px; border-bottom: 1px solid #e0e0e0; font-size: 11.5px; }\n");
        html.append("    .footer-table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n");
        html.append("    .footer-table td { vertical-align: top; }\n");
        html.append("    .tabla-totales-a4 { width: 100%; border-collapse: collapse; border: 1px solid #ccc; }\n");
        html.append("    .tabla-totales-a4 td { padding: 5px 8px; font-size: 12px; }\n");
        html.append("    .qr-box { text-align: center; }\n");
        html.append("    .qr-box img { width: 32mm; height: 32mm; }\n");
        html.append("    .legal-legend { font-size: 10px; color: #555; text-align: center; margin-top: 15px; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <table class=\"header-table\">\n");
        html.append("    <tr>\n");
        html.append("      <td style=\"width: 60%; padding-right: 15px;\">\n");
        html.append("        <div style=\"font-size: 16px; font-weight: bold; color: #0d47a1;\">").append(escaparHtml(empresa.getRazonSocial())).append("</div>\n");
        html.append("        <div class=\"bold\">RUC: ").append(empresa.getRuc()).append("</div>\n");
        if (sucursal.getDireccion() != null && !sucursal.getDireccion().isBlank()) {
            html.append("        <div>Dirección: ").append(escaparHtml(sucursal.getDireccion())).append("</div>\n");
        }
        if (sucursal.getTelefono() != null && !sucursal.getTelefono().isBlank()) {
            html.append("        <div>Teléfono: ").append(escaparHtml(sucursal.getTelefono())).append("</div>\n");
        }
        html.append("      </td>\n");
        html.append("      <td style=\"width: 40%;\">\n");
        html.append("        <div class=\"caja-fiscal-a4\">\n");
        html.append("          <div class=\"ruc\">R.U.C. ").append(empresa.getRuc()).append("</div>\n");
        html.append("          <div class=\"tipo\">").append(descTipo).append(" ELECTRÓNICA</div>\n");
        html.append("          <div class=\"numero\">").append(doc.getSerie()).append(" - ").append(String.format("%08d", doc.getNumero())).append("</div>\n");
        html.append("        </div>\n");
        html.append("      </td>\n");
        html.append("    </tr>\n");
        html.append("  </table>\n");
        html.append("  <table class=\"box-info\">\n");
        html.append("    <tr>\n");
        html.append("      <td style=\"width: 65%;\">\n");
        html.append("        <div><span class=\"bold\">Señor(es):</span> ").append(escaparHtml(doc.getClienteNombre() != null ? doc.getClienteNombre() : "CLIENTES VARIOS")).append("</div>\n");
        html.append("        <div><span class=\"bold\">RUC/DNI:</span> ").append(doc.getClienteNumeroDoc() != null ? doc.getClienteNumeroDoc() : "-").append("</div>\n");
        if (doc.getClienteDireccion() != null && !doc.getClienteDireccion().isBlank()) {
            html.append("        <div><span class=\"bold\">Dirección:</span> ").append(escaparHtml(doc.getClienteDireccion())).append("</div>\n");
        }
        html.append("      </td>\n");
        html.append("      <td style=\"width: 35%; border-left: 1px solid #eee;\">\n");
        html.append("        <div><span class=\"bold\">Fecha Emisión:</span> ").append(doc.getFechaEmision() != null ? doc.getFechaEmision().format(FECHA_HORA_FMT) : "").append("</div>\n");
        html.append("        <div><span class=\"bold\">Moneda:</span> ").append(doc.getMoneda()).append("</div>\n");
        html.append("        <div><span class=\"bold\">Forma de Pago:</span> ").append(doc.getFormaPago()).append("</div>\n");
        html.append("      </td>\n");
        html.append("    </tr>\n");
        html.append("  </table>\n");
        html.append("  <table class=\"tabla-items-a4\">\n");
        html.append("    <thead>\n");
        html.append("      <tr>\n");
        html.append("        <th style=\"width: 10%;\">Cant.</th>\n");
        html.append("        <th style=\"width: 10%;\">U.M.</th>\n");
        html.append("        <th class=\"text-left\" style=\"width: 50%;\">Descripción</th>\n");
        html.append("        <th class=\"text-right\" style=\"width: 15%;\">V. Unit</th>\n");
        html.append("        <th class=\"text-right\" style=\"width: 15%;\">Importe</th>\n");
        html.append("      </tr>\n");
        html.append("    </thead>\n");
        html.append("    <tbody>\n");
        for (LineaComprobante it : doc.getDetalles()) {
            BigDecimal vu = it.getValorUnitario() != null ? it.getValorUnitario() : it.getPrecioUnitario();
            html.append("      <tr>\n");
            html.append(String.format(Locale.US, "        <td class=\"text-center\">%.0f</td>\n", it.getCantidad()));
            html.append("        <td class=\"text-center\">").append(it.getUnidadMedida() != null ? it.getUnidadMedida() : "NIU").append("</td>\n");
            html.append("        <td class=\"text-left\">").append(escaparHtml(it.getDescripcion())).append("</td>\n");
            html.append(String.format(Locale.US, "        <td class=\"text-right\">%.2f</td>\n", vu != null ? vu : BigDecimal.ZERO));
            html.append(String.format(Locale.US, "        <td class=\"text-right\">%.2f</td>\n", it.getTotal() != null ? it.getTotal() : BigDecimal.ZERO));
            html.append("      </tr>\n");
        }
        html.append("    </tbody>\n");
        html.append("  </table>\n");

        BigDecimal opGravada = doc.getSubtotal() != null ? doc.getSubtotal() : BigDecimal.ZERO;
        BigDecimal opIgv = doc.getTotalTributos() != null ? doc.getTotalTributos() : BigDecimal.ZERO;
        BigDecimal opTotal = doc.getTotal() != null ? doc.getTotal() : BigDecimal.ZERO;

        html.append("  <table class=\"footer-table\">\n");
        html.append("    <tr>\n");
        html.append("      <td style=\"width: 55%; padding-right: 15px;\">\n");
        if (!montoLetras.isBlank()) {
            html.append("        <div style=\"font-size: 11px;\"><span class=\"bold\">SON:</span> ").append(escaparHtml(montoLetras)).append("</div>\n");
        }
        if (!qrBase64.isBlank()) {
            html.append("        <div class=\"qr-box\" style=\"margin-top: 10px;\">\n");
            html.append("          <img src=\"").append(qrBase64).append("\" alt=\"QR SUNAT\" />\n");
            html.append("        </div>\n");
        }
        if (doc.getHashCpe() != null && !doc.getHashCpe().isBlank()) {
            html.append("        <div style=\"font-size: 9.5px; text-align: center; margin-top: 4px; word-break: break-all;\"><span class=\"bold\">Hash CPE:</span> ").append(doc.getHashCpe()).append("</div>\n");
        }
        html.append("      </td>\n");
        html.append("      <td style=\"width: 45%;\">\n");
        html.append("        <table class=\"tabla-totales-a4\">\n");
        html.append(String.format(Locale.US, "          <tr><td class=\"text-right\">Op. Gravada:</td><td class=\"text-right bold\">%s %.2f</td></tr>\n", doc.getMoneda(), opGravada));
        html.append(String.format(Locale.US, "          <tr><td class=\"text-right\">I.G.V. (18%%):</td><td class=\"text-right bold\">%s %.2f</td></tr>\n", doc.getMoneda(), opIgv));
        html.append(String.format(Locale.US, "          <tr style=\"background-color: #f5f5f5; font-size: 13px;\"><td class=\"text-right bold\">IMPORTE TOTAL:</td><td class=\"text-right bold\">%s %.2f</td></tr>\n", doc.getMoneda(), opTotal));
        html.append("        </table>\n");
        html.append("      </td>\n");
        html.append("    </tr>\n");
        html.append("  </table>\n");
        html.append("  <div class=\"legal-legend\">\n");
        html.append("    Representación impresa de la ").append(descTipo).append(" Electrónica.<br>Consulte su validez en www.sunat.gob.pe\n");
        html.append("  </div>\n");
        html.append("  <script>\n");
        html.append("    window.onload = function() { window.print(); };\n");
        html.append("  </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    @Transactional(readOnly = true)
    public byte[] generarPdf(UUID documentoId, String tipoFormato) {
        ComprobanteFiscal doc = documentoRepositoryPort.buscarPorId(documentoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, documentoId));

        Empresa empresa = empresaRepository.findById(doc.getEmpresaId())
                .orElse(Empresa.builder().ruc("00000000000").razonSocial("EMPRESA").build());
        Sucursal sucursal = sucursalRepository.findById(doc.getSucursalId())
                .orElse(Sucursal.builder().codigo("0000").nombreSucursal("Casa Matriz").direccion(empresa.getDireccionFiscal()).build());

        String formato = (tipoFormato != null && !tipoFormato.isBlank()) ? tipoFormato.toUpperCase() : "A4";

        if (formato.startsWith("TICKET")) {
            return generarPdfTicket(doc, empresa, sucursal, formato);
        } else {
            return generarPdfA4(doc, empresa, sucursal);
        }
    }

    private byte[] generarPdfTicket(ComprobanteFiscal doc, Empresa empresa, Sucursal sucursal, String formato) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            float anchoMm = "TICKET_58".equalsIgnoreCase(formato) ? 58.0f : 80.0f;
            float anchoPt = anchoMm * MM_TO_PT;

            // Calcular alto dinámico proporcional a los ítems para no desperdiciar papel
            int cantItems = doc.getDetalles() != null ? doc.getDetalles().size() : 1;
            float altoEstimadoMm = 120.0f + (cantItems * 8.0f);
            float altoPt = Math.max(altoEstimadoMm * MM_TO_PT, 350.0f);

            Rectangle pageSize = new Rectangle(anchoPt, altoPt);
            Document document = new Document(pageSize, 6f, 6f, 6f, 6f);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.BLACK);
            Font fontRegular = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.BLACK);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
            Font fontMini = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, Color.DARK_GRAY);

            // Cabecera Empresa
            Paragraph pEmpresa = new Paragraph(empresa.getRazonSocial(), fontTitulo);
            pEmpresa.setAlignment(Element.ALIGN_CENTER);
            document.add(pEmpresa);

            Paragraph pRuc = new Paragraph("RUC: " + empresa.getRuc(), fontBold);
            pRuc.setAlignment(Element.ALIGN_CENTER);
            document.add(pRuc);

            if (sucursal.getDireccion() != null && !sucursal.getDireccion().isBlank()) {
                Paragraph pDir = new Paragraph(sucursal.getDireccion(), fontRegular);
                pDir.setAlignment(Element.ALIGN_CENTER);
                document.add(pDir);
            }

            // Caja Fiscal
            PdfPTable tablaFiscal = new PdfPTable(1);
            tablaFiscal.setWidthPercentage(100);
            tablaFiscal.setSpacingBefore(4f);
            tablaFiscal.setSpacingAfter(4f);

            String descTipo = doc.getTipoComprobante() != null ? doc.getTipoComprobante().getDescripcion().toUpperCase() : "COMPROBANTE";
            PdfPCell cellFiscal = new PdfPCell();
            cellFiscal.setBorder(Rectangle.BOX);
            cellFiscal.setPadding(3f);
            cellFiscal.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pTipo = new Paragraph(descTipo, fontBold);
            pTipo.setAlignment(Element.ALIGN_CENTER);
            Paragraph pNumero = new Paragraph(doc.getSerie() + "-" + String.format("%08d", doc.getNumero()), fontTitulo);
            pNumero.setAlignment(Element.ALIGN_CENTER);
            cellFiscal.addElement(pTipo);
            cellFiscal.addElement(pNumero);
            tablaFiscal.addCell(cellFiscal);
            document.add(tablaFiscal);

            // Datos Cliente y Fecha
            document.add(new Paragraph("Fecha: " + (doc.getFechaEmision() != null ? doc.getFechaEmision().format(FECHA_HORA_FMT) : ""), fontRegular));
            document.add(new Paragraph("Doc: " + (doc.getClienteNumeroDoc() != null ? doc.getClienteNumeroDoc() : "-"), fontRegular));
            document.add(new Paragraph("Cliente: " + (doc.getClienteNombre() != null ? doc.getClienteNombre() : "CLIENTES VARIOS"), fontBold));
            document.add(new Paragraph("Moneda: " + doc.getMoneda() + " | Pago: " + doc.getFormaPago(), fontRegular));

            // Tabla Ítems
            PdfPTable tablaItems = new PdfPTable(4);
            tablaItems.setWidthPercentage(100);
            tablaItems.setSpacingBefore(5f);
            tablaItems.setWidths(new float[]{14f, 48f, 18f, 20f});

            agregarCeldaHeader(tablaItems, "Cant", fontBold, Element.ALIGN_CENTER);
            agregarCeldaHeader(tablaItems, "Descrip", fontBold, Element.ALIGN_LEFT);
            agregarCeldaHeader(tablaItems, "P.U.", fontBold, Element.ALIGN_RIGHT);
            agregarCeldaHeader(tablaItems, "Total", fontBold, Element.ALIGN_RIGHT);

            for (LineaComprobante it : doc.getDetalles()) {
                BigDecimal pu = it.getPrecioUnitario() != null ? it.getPrecioUnitario() : it.getValorUnitario();
                agregarCeldaFila(tablaItems, String.format(Locale.US, "%.0f", it.getCantidad()), fontRegular, Element.ALIGN_CENTER);
                agregarCeldaFila(tablaItems, it.getDescripcion(), fontRegular, Element.ALIGN_LEFT);
                agregarCeldaFila(tablaItems, String.format(Locale.US, "%.2f", pu != null ? pu : BigDecimal.ZERO), fontRegular, Element.ALIGN_RIGHT);
                agregarCeldaFila(tablaItems, String.format(Locale.US, "%.2f", it.getTotal() != null ? it.getTotal() : BigDecimal.ZERO), fontRegular, Element.ALIGN_RIGHT);
            }
            document.add(tablaItems);

            // Totales
            PdfPTable tablaTotales = new PdfPTable(2);
            tablaTotales.setWidthPercentage(100);
            tablaTotales.setSpacingBefore(4f);
            tablaTotales.setWidths(new float[]{65f, 35f});

            agregarFilaTotal(tablaTotales, "Op. Gravada:", String.format(Locale.US, "%s %.2f", doc.getMoneda(), doc.getSubtotal() != null ? doc.getSubtotal() : BigDecimal.ZERO), fontRegular);
            agregarFilaTotal(tablaTotales, "I.G.V. (18%):", String.format(Locale.US, "%s %.2f", doc.getMoneda(), doc.getTotalTributos() != null ? doc.getTotalTributos() : BigDecimal.ZERO), fontRegular);
            agregarFilaTotal(tablaTotales, "TOTAL:", String.format(Locale.US, "%s %.2f", doc.getMoneda(), doc.getTotal() != null ? doc.getTotal() : BigDecimal.ZERO), fontTitulo);
            document.add(tablaTotales);

            // Código QR
            String qrSunatCadena = qrCodeGeneratorService.generarCadenaQrSunat(doc, empresa.getRuc());
            byte[] qrBytes = qrCodeGeneratorService.generarImagenQrPng(qrSunatCadena, 140, 140);
            if (qrBytes.length > 0) {
                Image imgQr = Image.getInstance(qrBytes);
                imgQr.scaleAbsolute(75f, 75f);
                imgQr.setAlignment(Element.ALIGN_CENTER);
                imgQr.setSpacingBefore(6f);
                document.add(imgQr);
            }

            // Pie legal
            if (doc.getHashCpe() != null && !doc.getHashCpe().isBlank()) {
                Paragraph pHash = new Paragraph("Hash: " + doc.getHashCpe(), fontMini);
                pHash.setAlignment(Element.ALIGN_CENTER);
                document.add(pHash);
            }

            Paragraph pLegal = new Paragraph("Representación Impresa del CPE.\nConsulte en www.sunat.gob.pe\n¡Gracias por su compra!", fontMini);
            pLegal.setAlignment(Element.ALIGN_CENTER);
            pLegal.setSpacingBefore(3f);
            document.add(pLegal);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF de Ticket: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DOCUMENT_PRINT_ERROR, "No se pudo generar el PDF del Ticket: " + e.getMessage());
        }
    }

    private byte[] generarPdfA4(ComprobanteFiscal doc, Empresa empresa, Sucursal sucursal) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 25f, 25f, 25f, 25f);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, new Color(13, 71, 161));
            Font fontEmpresa = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, Color.BLACK);
            Font fontRegular = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
            Font fontHeaderTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
            Font fontMini = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.DARK_GRAY);

            // Cabecera: Empresa (izquierda) + Recuadro RUC/Factura (derecha)
            PdfPTable tablaEncabezado = new PdfPTable(2);
            tablaEncabezado.setWidthPercentage(100);
            tablaEncabezado.setWidths(new float[]{62f, 38f});

            PdfPCell cellIzq = new PdfPCell();
            cellIzq.setBorder(Rectangle.NO_BORDER);
            cellIzq.addElement(new Paragraph(empresa.getRazonSocial(), fontEmpresa));
            cellIzq.addElement(new Paragraph("RUC: " + empresa.getRuc(), fontBold));
            if (sucursal.getDireccion() != null && !sucursal.getDireccion().isBlank()) {
                cellIzq.addElement(new Paragraph("Dirección: " + sucursal.getDireccion(), fontRegular));
            }
            if (sucursal.getTelefono() != null && !sucursal.getTelefono().isBlank()) {
                cellIzq.addElement(new Paragraph("Teléfono: " + sucursal.getTelefono(), fontRegular));
            }
            tablaEncabezado.addCell(cellIzq);

            // Recuadro Fiscal Azul A4
            PdfPCell cellDer = new PdfPCell();
            cellDer.setBorder(Rectangle.BOX);
            cellDer.setBorderColor(new Color(13, 71, 161));
            cellDer.setBorderWidth(1.5f);
            cellDer.setPadding(8f);
            cellDer.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pRucCaja = new Paragraph("R.U.C. " + empresa.getRuc(), fontTitulo);
            pRucCaja.setAlignment(Element.ALIGN_CENTER);
            String descTipo = doc.getTipoComprobante() != null ? doc.getTipoComprobante().getDescripcion().toUpperCase() : "COMPROBANTE";
            Paragraph pTipoCaja = new Paragraph(descTipo + " ELECTRÓNICA", fontBold);
            pTipoCaja.setAlignment(Element.ALIGN_CENTER);
            Paragraph pNumCaja = new Paragraph(doc.getSerie() + " - " + String.format("%08d", doc.getNumero()), fontTitulo);
            pNumCaja.setAlignment(Element.ALIGN_CENTER);

            cellDer.addElement(pRucCaja);
            cellDer.addElement(pTipoCaja);
            cellDer.addElement(pNumCaja);
            tablaEncabezado.addCell(cellDer);
            document.add(tablaEncabezado);

            // Bloque Datos Cliente
            PdfPTable tablaCliente = new PdfPTable(2);
            tablaCliente.setWidthPercentage(100);
            tablaCliente.setSpacingBefore(12f);
            tablaCliente.setWidths(new float[]{65f, 35f});

            PdfPCell cellCliente = new PdfPCell();
            cellCliente.setBorder(Rectangle.BOX);
            cellCliente.setBorderColor(Color.LIGHT_GRAY);
            cellCliente.setPadding(6f);
            cellCliente.addElement(new Paragraph("Señor(es): " + (doc.getClienteNombre() != null ? doc.getClienteNombre() : "-"), fontBold));
            cellCliente.addElement(new Paragraph("RUC/DNI: " + (doc.getClienteNumeroDoc() != null ? doc.getClienteNumeroDoc() : "-"), fontRegular));
            cellCliente.addElement(new Paragraph("Dirección: " + (doc.getClienteDireccion() != null ? doc.getClienteDireccion() : "-"), fontRegular));
            tablaCliente.addCell(cellCliente);

            PdfPCell cellEmision = new PdfPCell();
            cellEmision.setBorder(Rectangle.BOX);
            cellEmision.setBorderColor(Color.LIGHT_GRAY);
            cellEmision.setPadding(6f);
            cellEmision.addElement(new Paragraph("Fecha Emisión: " + (doc.getFechaEmision() != null ? doc.getFechaEmision().format(FECHA_HORA_FMT) : ""), fontRegular));
            cellEmision.addElement(new Paragraph("Moneda: " + doc.getMoneda(), fontRegular));
            cellEmision.addElement(new Paragraph("Forma de Pago: " + doc.getFormaPago(), fontRegular));
            tablaCliente.addCell(cellEmision);
            document.add(tablaCliente);

            // Tabla Ítems
            PdfPTable tablaItems = new PdfPTable(5);
            tablaItems.setWidthPercentage(100);
            tablaItems.setSpacingBefore(10f);
            tablaItems.setWidths(new float[]{10f, 10f, 50f, 15f, 15f});

            Color fondoAzul = new Color(13, 71, 161);
            agregarCeldaHeaderEstilizada(tablaItems, "Cant.", fontHeaderTabla, fondoAzul, Element.ALIGN_CENTER);
            agregarCeldaHeaderEstilizada(tablaItems, "U.M.", fontHeaderTabla, fondoAzul, Element.ALIGN_CENTER);
            agregarCeldaHeaderEstilizada(tablaItems, "Descripción", fontHeaderTabla, fondoAzul, Element.ALIGN_LEFT);
            agregarCeldaHeaderEstilizada(tablaItems, "V. Unit", fontHeaderTabla, fondoAzul, Element.ALIGN_RIGHT);
            agregarCeldaHeaderEstilizada(tablaItems, "Importe", fontHeaderTabla, fondoAzul, Element.ALIGN_RIGHT);

            for (LineaComprobante item : doc.getDetalles()) {
                BigDecimal vu = item.getValorUnitario() != null ? item.getValorUnitario() : item.getPrecioUnitario();
                agregarCeldaFilaA4(tablaItems, String.format(Locale.US, "%.0f", item.getCantidad()), fontRegular, Element.ALIGN_CENTER);
                agregarCeldaFilaA4(tablaItems, item.getUnidadMedida() != null ? item.getUnidadMedida() : "NIU", fontRegular, Element.ALIGN_CENTER);
                agregarCeldaFilaA4(tablaItems, item.getDescripcion(), fontRegular, Element.ALIGN_LEFT);
                agregarCeldaFilaA4(tablaItems, String.format(Locale.US, "%.2f", vu != null ? vu : BigDecimal.ZERO), fontRegular, Element.ALIGN_RIGHT);
                agregarCeldaFilaA4(tablaItems, String.format(Locale.US, "%.2f", item.getTotal() != null ? item.getTotal() : BigDecimal.ZERO), fontRegular, Element.ALIGN_RIGHT);
            }
            document.add(tablaItems);

            // Pie: Monto en Letras + QR (izq) y Totales (der)
            PdfPTable tablaPie = new PdfPTable(2);
            tablaPie.setWidthPercentage(100);
            tablaPie.setSpacingBefore(10f);
            tablaPie.setWidths(new float[]{60f, 40f});

            PdfPCell cellPieIzq = new PdfPCell();
            cellPieIzq.setBorder(Rectangle.NO_BORDER);

            String montoLetras = NumeroALetrasUtil.convertir(doc.getTotal(), doc.getMoneda());
            if (!montoLetras.isBlank()) {
                cellPieIzq.addElement(new Paragraph("SON: " + montoLetras, fontBold));
            }

            // QR
            String qrSunatCadena = qrCodeGeneratorService.generarCadenaQrSunat(doc, empresa.getRuc());
            byte[] qrBytes = qrCodeGeneratorService.generarImagenQrPng(qrSunatCadena, 150, 150);
            if (qrBytes.length > 0) {
                Image imgQr = Image.getInstance(qrBytes);
                imgQr.scaleAbsolute(70f, 70f);
                imgQr.setSpacingBefore(4f);
                cellPieIzq.addElement(imgQr);
            }
            tablaPie.addCell(cellPieIzq);

            // Totales Fiscales
            PdfPCell cellTotales = new PdfPCell();
            cellTotales.setBorder(Rectangle.BOX);
            cellTotales.setBorderColor(Color.LIGHT_GRAY);
            cellTotales.setPadding(6f);

            PdfPTable tablaTotales = new PdfPTable(2);
            tablaTotales.setWidthPercentage(100);
            tablaTotales.setWidths(new float[]{60f, 40f});

            agregarFilaTotal(tablaTotales, "Op. Gravada:", String.format(Locale.US, "%s %.2f", doc.getMoneda(), doc.getSubtotal() != null ? doc.getSubtotal() : BigDecimal.ZERO), fontRegular);
            agregarFilaTotal(tablaTotales, "I.G.V. (18%):", String.format(Locale.US, "%s %.2f", doc.getMoneda(), doc.getTotalTributos() != null ? doc.getTotalTributos() : BigDecimal.ZERO), fontRegular);
            agregarFilaTotal(tablaTotales, "IMPORTE TOTAL:", String.format(Locale.US, "%s %.2f", doc.getMoneda(), doc.getTotal() != null ? doc.getTotal() : BigDecimal.ZERO), fontBold);

            cellTotales.addElement(tablaTotales);
            tablaPie.addCell(cellTotales);
            document.add(tablaPie);

            // Leyenda Legal SUNAT
            Paragraph pLegal = new Paragraph(
                    "Representación impresa de la " + descTipo + " Electrónica. Consulte su comprobante en www.sunat.gob.pe\n" +
                    (doc.getHashCpe() != null ? "Hash CPE: " + doc.getHashCpe() : ""), fontMini);
            pLegal.setAlignment(Element.ALIGN_CENTER);
            pLegal.setSpacingBefore(10f);
            document.add(pLegal);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF A4: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DOCUMENT_PRINT_ERROR, "No se pudo generar el PDF A4: " + e.getMessage());
        }
    }

    private void agregarCeldaHeader(PdfPTable table, String texto, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(align);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setPaddingBottom(3f);
        table.addCell(cell);
    }

    private void agregarCeldaHeaderEstilizada(PdfPTable table, String texto, Font font, Color bgColor, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(4f);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void agregarCeldaFila(PdfPTable table, String texto, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(align);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2f);
        table.addCell(cell);
    }

    private void agregarCeldaFilaA4(PdfPTable table, String texto, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(align);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private void agregarFilaTotal(PdfPTable table, String etiqueta, String valor, Font font) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(etiqueta, font));
        cellLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setPadding(2f);
        table.addCell(cellLabel);

        PdfPCell cellVal = new PdfPCell(new Phrase(valor, font));
        cellVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellVal.setBorder(Rectangle.NO_BORDER);
        cellVal.setPadding(2f);
        table.addCell(cellVal);
    }

    private String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
