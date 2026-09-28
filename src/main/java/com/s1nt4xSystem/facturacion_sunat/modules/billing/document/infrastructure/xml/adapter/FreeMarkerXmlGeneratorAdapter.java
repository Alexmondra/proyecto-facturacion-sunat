package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.xml.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.GeneradorXmlPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.xml.util.NumeroALetrasUtil;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class FreeMarkerXmlGeneratorAdapter implements GeneradorXmlPort {

    private final Configuration freemarkerConfig;

    public FreeMarkerXmlGeneratorAdapter() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates/ubl");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        this.freemarkerConfig = cfg;
    }

    @Override
    public byte[] generarXml(ComprobanteFiscal comprobante, Empresa empresa, Sucursal sucursal) {
        try {
            String tipoCodigo = comprobante.getTipoComprobante() != null 
                    ? comprobante.getTipoComprobante().getCodigo() 
                    : "01";

            String templateName;
            if ("07".equals(tipoCodigo)) {
                templateName = "credit-note-2.1.ftl";
            } else if ("08".equals(tipoCodigo)) {
                templateName = "debit-note-2.1.ftl";
            } else {
                templateName = "invoice-2.1.ftl";
            }

            Template template = freemarkerConfig.getTemplate(templateName);
            Map<String, Object> model = buildDataModel(comprobante, empresa, sucursal);

            StringWriter writer = new StringWriter();
            template.process(model, writer);

            return writer.toString().getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error al generar el XML UBL 2.1 con FreeMarker: {}", e.getMessage(), e);
            throw new DomainException("Error al generar XML UBL 2.1 del comprobante: " + e.getMessage());
        }
    }

    private Map<String, Object> buildDataModel(ComprobanteFiscal comprobante, Empresa empresa, Sucursal sucursal) {
        Map<String, Object> model = new HashMap<>();

        // Identificador del documento (ej: F001-00000001)
        String numeroPadded = String.format("%08d", comprobante.getNumero());
        String idComprobante = comprobante.getSerie() + "-" + numeroPadded;
        model.put("idComprobante", idComprobante);

        // Fechas y horas
        if (comprobante.getFechaEmision() != null) {
            model.put("fechaEmision", comprobante.getFechaEmision().toLocalDate().toString());
            model.put("horaEmision", comprobante.getFechaEmision().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } else {
            model.put("fechaEmision", java.time.LocalDate.now().toString());
            model.put("horaEmision", java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }

        // Tipo comprobante y operación
        String tipoDoc = comprobante.getTipoComprobante() != null ? comprobante.getTipoComprobante().getCodigo() : "01";
        model.put("tipoComprobante", tipoDoc);
        model.put("tipoOperacion", comprobante.getTipoOperacion() != null ? comprobante.getTipoOperacion() : "0101");
        model.put("moneda", comprobante.getMoneda() != null ? comprobante.getMoneda() : "PEN");

        // Leyenda de monto en letras obligatoria en SUNAT
        String montoEnLetras = NumeroALetrasUtil.convertir(comprobante.getTotal(), comprobante.getMoneda());
        model.put("leyendaMontoLetras", montoEnLetras);

        // Emisor
        model.put("emisorRuc", empresa.getRuc());
        model.put("emisorRazonSocial", empresa.getRazonSocial());
        model.put("direccionFiscal", empresa.getDireccionFiscal() != null ? empresa.getDireccionFiscal() : "");
        model.put("codigoSucursal", sucursal != null && sucursal.getCodigo() != null ? sucursal.getCodigo() : "0000");
        model.put("ubigeoSucursal", sucursal != null && sucursal.getUbigeo() != null ? sucursal.getUbigeo() : "");

        // Cliente
        model.put("clienteTipoDoc", comprobante.getClienteTipoDoc() != null ? comprobante.getClienteTipoDoc() : "6");
        model.put("clienteNumeroDoc", comprobante.getClienteNumeroDoc() != null ? comprobante.getClienteNumeroDoc() : "-");
        model.put("clienteNombre", comprobante.getClienteNombre() != null ? comprobante.getClienteNombre() : "-");
        model.put("clienteDireccion", comprobante.getClienteDireccion() != null ? comprobante.getClienteDireccion() : "");

        // Totales monetarios
        model.put("subtotal", comprobante.getSubtotal() != null ? comprobante.getSubtotal() : BigDecimal.ZERO);
        model.put("total", comprobante.getTotal() != null ? comprobante.getTotal() : BigDecimal.ZERO);
        model.put("totalTributos", comprobante.getTotalTributos() != null ? comprobante.getTotalTributos() : BigDecimal.ZERO);
        model.put("totalDescuentos", comprobante.getTotalDescuentos() != null ? comprobante.getTotalDescuentos() : BigDecimal.ZERO);
        model.put("totalOtrosCargos", comprobante.getTotalOtrosCargos() != null ? comprobante.getTotalOtrosCargos() : BigDecimal.ZERO);

        // Forma de pago y Cuotas
        String formaPago = comprobante.getFormaPago() != null ? comprobante.getFormaPago().toUpperCase() : "CONTADO";
        model.put("formaPago", formaPago);
        model.put("cuotas", comprobante.getCuotas() != null ? comprobante.getCuotas() : Collections.emptyList());

        BigDecimal montoPendiente = BigDecimal.ZERO;
        if ("CREDITO".equals(formaPago) && comprobante.getCuotas() != null) {
            for (CuotaPago c : comprobante.getCuotas()) {
                if (c.getMonto() != null) {
                    montoPendiente = montoPendiente.add(c.getMonto());
                }
            }
        }
        model.put("montoPendienteCredito", montoPendiente);

        // Detracción
        model.put("detraccion", comprobante.getDetraccion());

        // Referencias
        model.put("referencias", comprobante.getReferencias() != null ? comprobante.getReferencias() : Collections.emptyList());

        // Tributos globales
        List<Map<String, Object>> tributosGlobalesDto = new ArrayList<>();
        if (comprobante.getTributosGlobales() != null) {
            for (TributoLinea tg : comprobante.getTributosGlobales()) {
                Map<String, Object> map = new HashMap<>();
                boolean esIcbper = "7152".equals(tg.getCodigoTributo()) || "ICBPER".equalsIgnoreCase(tg.getNombreTributo());
                map.put("codigoTributo", tg.getCodigoTributo() != null ? tg.getCodigoTributo() : (esIcbper ? "7152" : "1000"));
                map.put("nombreTributo", tg.getNombreTributo() != null ? tg.getNombreTributo() : (esIcbper ? "ICBPER" : "IGV"));
                map.put("codigoInternacional", tg.getTipoTributo() != null ? tg.getTipoTributo() : (esIcbper ? "OTH" : "VAT"));
                map.put("monto", tg.getMonto() != null ? tg.getMonto() : BigDecimal.ZERO);
                map.put("baseImponible", tg.getBaseImponible() != null ? tg.getBaseImponible() : BigDecimal.ZERO);
                map.put("cantidadBase", tg.getCantidadBase());
                map.put("esIcbper", esIcbper);
                tributosGlobalesDto.add(map);
            }
        }
        model.put("tributosGlobales", tributosGlobalesDto);

        // Líneas de ítems
        List<Map<String, Object>> lineasDto = new ArrayList<>();
        if (comprobante.getLineas() != null) {
            for (LineaComprobante l : comprobante.getLineas()) {
                Map<String, Object> lMap = new HashMap<>();
                lMap.put("item", l.getItem());
                lMap.put("descripcion", l.getDescripcion());
                lMap.put("cantidad", l.getCantidad());
                lMap.put("unidadMedida", l.getUnidadMedida() != null ? l.getUnidadMedida() : "NIU");
                BigDecimal valorVenta = l.getSubtotal() != null ? l.getSubtotal()
                        : (l.getTotal() != null && l.getTotalTributos() != null ? l.getTotal().subtract(l.getTotalTributos())
                        : (l.getValorUnitario() != null && l.getCantidad() != null ? l.getValorUnitario().multiply(l.getCantidad()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO));
                lMap.put("valorVenta", valorVenta);
                lMap.put("totalTributos", l.getTotalTributos() != null ? l.getTotalTributos() : BigDecimal.ZERO);
                lMap.put("valorUnitario", l.getValorUnitario() != null ? l.getValorUnitario() : BigDecimal.ZERO);
                
                boolean esGratuito = l.getTipoAfectacion() != null && l.getTipoAfectacion().isGratuito();
                BigDecimal precioReferencial = l.getPrecioUnitario() != null ? l.getPrecioUnitario()
                        : (l.getCantidad() != null && l.getCantidad().compareTo(BigDecimal.ZERO) > 0 && l.getTotal() != null
                        ? l.getTotal().divide(l.getCantidad(), 4, RoundingMode.HALF_UP) : l.getValorUnitario());
                lMap.put("precioReferencial", precioReferencial);
                lMap.put("tipoPrecioReferencial", esGratuito ? "02" : "01");
                lMap.put("tipoAfectacionCodigo", l.getTipoAfectacion() != null ? l.getTipoAfectacion().getCodigo() : "10");
                lMap.put("codigoProducto", l.getCodigoProducto());

                List<Map<String, Object>> lTributos = new ArrayList<>();
                if (l.getTributos() != null) {
                    for (TributoLinea lt : l.getTributos()) {
                        Map<String, Object> ltMap = new HashMap<>();
                        boolean esIcbper = "7152".equals(lt.getCodigoTributo()) || "ICBPER".equalsIgnoreCase(lt.getNombreTributo());
                        ltMap.put("codigoTributo", lt.getCodigoTributo() != null ? lt.getCodigoTributo() : (esIcbper ? "7152" : "1000"));
                        ltMap.put("nombreTributo", lt.getNombreTributo() != null ? lt.getNombreTributo() : (esIcbper ? "ICBPER" : "IGV"));
                        ltMap.put("codigoInternacional", lt.getTipoTributo() != null ? lt.getTipoTributo() : (esIcbper ? "OTH" : "VAT"));
                        ltMap.put("monto", lt.getMonto() != null ? lt.getMonto() : BigDecimal.ZERO);
                        ltMap.put("baseImponible", lt.getBaseImponible() != null ? lt.getBaseImponible() : BigDecimal.ZERO);
                        ltMap.put("porcentaje", lt.getPorcentaje() != null ? lt.getPorcentaje() : BigDecimal.ZERO);

                        BigDecimal cantidadBolsas = lt.getCantidadBase() != null ? lt.getCantidadBase() : l.getCantidad();
                        ltMap.put("cantidadBase", cantidadBolsas != null ? cantidadBolsas : BigDecimal.ONE);
                        ltMap.put("esIcbper", esIcbper);

                        if (esIcbper) {
                            BigDecimal cBolsas = (BigDecimal) ltMap.get("cantidadBase");
                            BigDecimal montoUnitario = (cBolsas != null && cBolsas.compareTo(BigDecimal.ZERO) > 0 && lt.getMonto() != null)
                                    ? lt.getMonto().divide(cBolsas, 2, RoundingMode.HALF_UP)
                                    : (lt.getMonto() != null ? lt.getMonto() : BigDecimal.ZERO);
                            ltMap.put("montoUnitario", montoUnitario);
                        }

                        lTributos.add(ltMap);
                    }
                }
                lMap.put("tributos", lTributos);
                lineasDto.add(lMap);
            }
        }
        model.put("lineas", lineasDto);
        model.put("lineaCount", lineasDto.size());

        return model;
    }
}
