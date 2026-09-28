package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.mapper;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.CuotaPago;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.LineaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ReferenciaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoAfectacionIgv;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TipoTributoSunat;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TotalesAfectacion;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.TributoLinea;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.ClienteDto;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.ComprobanteDataResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.ComprobanteResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.CuotaDto;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.DetraccionDto;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.DocumentoReferenciaResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.EmisorDto;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.ItemComprobanteResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.NotaResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.TotalesNotaResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.TotalesResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ComprobanteRestMapper {

    private ComprobanteRestMapper() {
    }

    public static ComprobanteResponse toComprobanteResponse(ComprobanteFiscal d, String rucEmisor) {
        return toComprobanteResponse(d, rucEmisor, null);
    }

    public static ComprobanteResponse toComprobanteResponse(ComprobanteFiscal d, String rucEmisor, String codSucursal) {
        List<ItemComprobanteResponse> items = new ArrayList<>();
        BigDecimal totalIgv = BigDecimal.ZERO;
        BigDecimal totalIcbper = BigDecimal.ZERO;
        BigDecimal totalIsc = BigDecimal.ZERO;

        for (LineaComprobante l : d.getDetalles()) {
            BigDecimal itemIgv = BigDecimal.ZERO;
            BigDecimal itemIcbper = BigDecimal.ZERO;
            BigDecimal itemIsc = BigDecimal.ZERO;

            Integer bolsasCount = null;
            if (l.getTributos() != null && !l.getTributos().isEmpty()) {
                for (TributoLinea t : l.getTributos()) {
                    boolean isIgv = TipoTributoSunat.IGV.coincideCon(t.getCodigoTributo());
                    boolean isIcbper = TipoTributoSunat.ICBPER.coincideCon(t.getCodigoTributo());
                    boolean isIsc = TipoTributoSunat.ISC.coincideCon(t.getCodigoTributo());

                    if (isIgv) {
                        itemIgv = itemIgv.add(t.getMonto());
                        totalIgv = totalIgv.add(t.getMonto());
                    } else if (isIcbper) {
                        itemIcbper = itemIcbper.add(t.getMonto());
                        totalIcbper = totalIcbper.add(t.getMonto());
                        if (t.getCantidadBase() != null) {
                            bolsasCount = t.getCantidadBase().intValue();
                        }
                    } else if (isIsc) {
                        itemIsc = itemIsc.add(t.getMonto());
                        totalIsc = totalIsc.add(t.getMonto());
                    }
                }
            }

            // Fallback para IGV en líneas gravadas si no vino en tributos individuales
            if (itemIgv.compareTo(BigDecimal.ZERO) == 0 && l.getTipoAfectacion() != null && l.getTipoAfectacion().isGravaIgv()) {
                if (l.getTotalTributos() != null && l.getTotalTributos().compareTo(BigDecimal.ZERO) > 0) {
                    itemIgv = l.getTotalTributos().subtract(itemIcbper).subtract(itemIsc);
                    totalIgv = totalIgv.add(itemIgv);
                }
            }

            boolean hasIcbper = itemIcbper.compareTo(BigDecimal.ZERO) > 0;
            boolean hasIsc = itemIsc.compareTo(BigDecimal.ZERO) > 0;

            items.add(ItemComprobanteResponse.builder()
                    .codigo(l.getCodigoProducto() != null && !l.getCodigoProducto().isBlank() ? l.getCodigoProducto() : null)
                    .descripcion(l.getDescripcion())
                    .unidadMedida(l.getUnidadMedida())
                    .codigoAfectacionSunat(l.getTipoAfectacion() != null ? l.getTipoAfectacion().getCodigo() : "10")
                    .cantidad(l.getCantidad())
                    .valor(l.getValorUnitario())
                    .subtotal(l.getSubtotal())
                    .igv(itemIgv)
                    .icbper(hasIcbper ? Boolean.TRUE : null)
                    .cantidadBolsasIcbper(hasIcbper && bolsasCount != null ? bolsasCount : (hasIcbper ? l.getCantidad().intValue() : null))
                    .icbperMonto(hasIcbper ? itemIcbper : null)
                    .iscMonto(hasIsc ? itemIsc : null)
                    .total(l.getTotal())
                    .build());
        }

        BigDecimal totalGravada = BigDecimal.ZERO;
        BigDecimal totalExonerada = BigDecimal.ZERO;
        BigDecimal totalInafecta = BigDecimal.ZERO;
        BigDecimal totalGratuita = BigDecimal.ZERO;
        BigDecimal totalExportacion = BigDecimal.ZERO;

        if (d.getTotalesAfectacion() != null && !d.getTotalesAfectacion().isEmpty()) {
            for (TotalesAfectacion ta : d.getTotalesAfectacion()) {
                if ("GRAVADO".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalGravada = totalGravada.add(ta.getBaseImponible());
                } else if ("EXONERADO".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalExonerada = totalExonerada.add(ta.getBaseImponible());
                } else if ("INAFECTO".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalInafecta = totalInafecta.add(ta.getBaseImponible());
                } else if ("GRATUITO".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalGratuita = totalGratuita.add(ta.getBaseImponible());
                } else if ("EXPORTACION".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalExportacion = totalExportacion.add(ta.getBaseImponible());
                }
            }
        } else if (d.getDetalles() != null) {
            for (LineaComprobante l : d.getDetalles()) {
                if (l.getTipoAfectacion() != null) {
                    BigDecimal base = l.getSubtotal() != null ? l.getSubtotal() : BigDecimal.ZERO;
                    if (l.getTipoAfectacion().isGratuito()) {
                        totalGratuita = totalGratuita.add(base);
                    } else if (l.getTipoAfectacion().isGravaIgv()) {
                        totalGravada = totalGravada.add(base);
                    } else if (l.getTipoAfectacion() == TipoAfectacionIgv.EXONERADO_OPERACION_ONEROSA) {
                        totalExonerada = totalExonerada.add(base);
                    } else if (l.getTipoAfectacion() == TipoAfectacionIgv.INAFECTO_OPERACION_ONEROSA) {
                        totalInafecta = totalInafecta.add(base);
                    } else if (l.getTipoAfectacion() == TipoAfectacionIgv.EXPORTACION_BIENES_SERVICIOS) {
                        totalExportacion = totalExportacion.add(base);
                    }
                }
            }
        }

        List<CuotaDto> cuotas = null;
        if (d.getCuotas() != null && !d.getCuotas().isEmpty()) {
            cuotas = new ArrayList<>();
            for (CuotaPago c : d.getCuotas()) {
                cuotas.add(CuotaDto.builder()
                        .numero(c.getNumeroCuota())
                        .fechaVencimiento(c.getFechaVencimiento())
                        .monto(c.getMonto())
                        .build());
            }
        }

        DetraccionDto detDto = null;
        if (d.getDetraccion() != null) {
            detDto = DetraccionDto.builder()
                    .codigoBienServicio(d.getDetraccion().getCodigoBienServicio())
                    .porcentaje(d.getDetraccion().getPorcentaje())
                    .montoDetraccion(d.getDetraccion().getMontoDetraccion())
                    .medioPago(d.getDetraccion().getMedioPago())
                    .cuentaBancoNacion(d.getDetraccion().getNumeroCuentaDetraccion())
                    .build();
        }

        BigDecimal subtotal = d.getSubtotal() != null ? d.getSubtotal() : totalGravada.add(totalExonerada).add(totalInafecta).add(totalExportacion);
        BigDecimal totalTributos = d.getTotalTributos() != null ? d.getTotalTributos() : totalIgv.add(totalIcbper).add(totalIsc);
        BigDecimal totalFinal = d.getTotal();
        BigDecimal totalGravadaFinal = (totalGravada.compareTo(BigDecimal.ZERO) > 0 || (totalExonerada.compareTo(BigDecimal.ZERO) == 0 && totalInafecta.compareTo(BigDecimal.ZERO) == 0 && totalExportacion.compareTo(BigDecimal.ZERO) == 0))
                ? totalGravada : null;

        TotalesResponse totales = TotalesResponse.builder()
                .totalGravada(totalGravadaFinal)
                .totalExonerada(totalExonerada.compareTo(BigDecimal.ZERO) > 0 ? totalExonerada : null)
                .totalInafecta(totalInafecta.compareTo(BigDecimal.ZERO) > 0 ? totalInafecta : null)
                .totalGratuita(totalGratuita.compareTo(BigDecimal.ZERO) > 0 ? totalGratuita : null)
                .totalExportacion(totalExportacion.compareTo(BigDecimal.ZERO) > 0 ? totalExportacion : null)
                .subtotal(subtotal)
                .totalIgv(totalIgv)
                .totalIsc(totalIsc.compareTo(BigDecimal.ZERO) > 0 ? totalIsc : null)
                .totalIcbper(totalIcbper.compareTo(BigDecimal.ZERO) > 0 ? totalIcbper : null)
                .totalTributos(totalTributos)
                .totalOtrosCargos(d.getTotalOtrosCargos() != null && d.getTotalOtrosCargos().compareTo(BigDecimal.ZERO) > 0 ? d.getTotalOtrosCargos() : null)
                .totalDescuentos(d.getTotalDescuentos() != null && d.getTotalDescuentos().compareTo(BigDecimal.ZERO) > 0 ? d.getTotalDescuentos() : null)
                .totalDetraccion(detDto != null ? detDto.getMontoDetraccion() : null)
                .totalPagado(totalFinal)
                .total(totalFinal)
                .build();

        return ComprobanteResponse.builder()
                .id(d.getId())
                .emisor(EmisorDto.builder()
                        .ruc(rucEmisor)
                        .codigoSucursal(d.getCodigoSucursal() != null ? d.getCodigoSucursal() : (codSucursal != null ? codSucursal : "0000"))
                        .build())
                .comprobante(ComprobanteDataResponse.builder()
                        .claveIdempotencia(d.getClaveIdempotencia())
                        .tipo(d.getTipoComprobante().getCodigo())
                        .serie(d.getSerie())
                        .numero(d.getNumero())
                        .fechaEmision(d.getFechaEmision())
                        .moneda(d.getMoneda())
                        .formaPago(d.getFormaPago().toLowerCase())
                        .build())
                .cliente(ClienteDto.builder()
                        .tipoDocumento(d.getClienteTipoDoc())
                        .numeroDocumento(d.getClienteNumeroDoc())
                        .nombreRazonSocial(d.getClienteNombre())
                        .direccion(d.getClienteDireccion())
                        .build())
                .items(items)
                .cuotas(cuotas)
                .detraccion(detDto)
                .totales(totales)
                .estado(d.getEstadoInterno())
                .estadoSunat(d.getEstadoSunat())
                .codigoSunat(d.getCodigoSunat())
                .mensajeSunat(d.getMensajeSunat())
                .xmlUrl(d.getXmlUrl())
                .cdrUrl(d.getCdrUrl())
                .hashCpe(d.getHashCpe())
                .createdAt(d.getFechaEmision())
                .updatedAt(d.getFechaEmision())
                .build();
    }

    public static NotaResponse toNotaResponse(ComprobanteFiscal d) {
        return toNotaResponse(d, null);
    }

    public static NotaResponse toNotaResponse(ComprobanteFiscal d, @SuppressWarnings("unused") String rucEmisor) {
        List<ItemComprobanteResponse> items = new ArrayList<>();
        BigDecimal totalIgv = BigDecimal.ZERO;
        BigDecimal totalIcbper = BigDecimal.ZERO;
        BigDecimal totalIsc = BigDecimal.ZERO;

        for (LineaComprobante l : d.getDetalles()) {
            BigDecimal itemIgv = BigDecimal.ZERO;
            BigDecimal itemIcbper = BigDecimal.ZERO;
            BigDecimal itemIsc = BigDecimal.ZERO;

            Integer bolsasCount = null;
            if (l.getTributos() != null && !l.getTributos().isEmpty()) {
                for (TributoLinea t : l.getTributos()) {
                    boolean isIgv = TipoTributoSunat.IGV.coincideCon(t.getCodigoTributo());
                    boolean isIcbper = TipoTributoSunat.ICBPER.coincideCon(t.getCodigoTributo());
                    boolean isIsc = TipoTributoSunat.ISC.coincideCon(t.getCodigoTributo());

                    if (isIgv) {
                        itemIgv = itemIgv.add(t.getMonto());
                        totalIgv = totalIgv.add(t.getMonto());
                    } else if (isIcbper) {
                        itemIcbper = itemIcbper.add(t.getMonto());
                        totalIcbper = totalIcbper.add(t.getMonto());
                        if (t.getCantidadBase() != null) {
                            bolsasCount = t.getCantidadBase().intValue();
                        }
                    } else if (isIsc) {
                        itemIsc = itemIsc.add(t.getMonto());
                        totalIsc = totalIsc.add(t.getMonto());
                    }
                }
            }

            // Fallback para IGV en líneas gravadas si no vino en tributos individuales
            if (itemIgv.compareTo(BigDecimal.ZERO) == 0 && l.getTipoAfectacion() != null && l.getTipoAfectacion().isGravaIgv()) {
                if (l.getTotalTributos() != null && l.getTotalTributos().compareTo(BigDecimal.ZERO) > 0) {
                    itemIgv = l.getTotalTributos().subtract(itemIcbper).subtract(itemIsc);
                    totalIgv = totalIgv.add(itemIgv);
                }
            }

            boolean hasIcbper = itemIcbper.compareTo(BigDecimal.ZERO) > 0;
            boolean hasIsc = itemIsc.compareTo(BigDecimal.ZERO) > 0;

            items.add(ItemComprobanteResponse.builder()
                    .codigo(l.getCodigoProducto() != null && !l.getCodigoProducto().isBlank() ? l.getCodigoProducto() : null)
                    .descripcion(l.getDescripcion())
                    .unidadMedida(l.getUnidadMedida())
                    .codigoAfectacionSunat(l.getTipoAfectacion() != null ? l.getTipoAfectacion().getCodigo() : "10")
                    .cantidad(l.getCantidad())
                    .valor(l.getValorUnitario())
                    .subtotal(l.getSubtotal())
                    .igv(itemIgv)
                    .icbper(hasIcbper ? Boolean.TRUE : null)
                    .cantidadBolsasIcbper(hasIcbper && bolsasCount != null ? bolsasCount : (hasIcbper ? l.getCantidad().intValue() : null))
                    .icbperMonto(hasIcbper ? itemIcbper : null)
                    .iscMonto(hasIsc ? itemIsc : null)
                    .total(l.getTotal())
                    .build());
        }

        BigDecimal totalGravada = BigDecimal.ZERO;
        BigDecimal totalExonerada = BigDecimal.ZERO;
        BigDecimal totalInafecta = BigDecimal.ZERO;
        BigDecimal totalGratuita = BigDecimal.ZERO;
        BigDecimal totalExportacion = BigDecimal.ZERO;

        if (d.getTotalesAfectacion() != null && !d.getTotalesAfectacion().isEmpty()) {
            for (TotalesAfectacion ta : d.getTotalesAfectacion()) {
                if ("GRAVADO".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalGravada = totalGravada.add(ta.getBaseImponible());
                } else if ("EXONERADO".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalExonerada = totalExonerada.add(ta.getBaseImponible());
                } else if ("INAFECTO".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalInafecta = totalInafecta.add(ta.getBaseImponible());
                } else if ("GRATUITO".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalGratuita = totalGratuita.add(ta.getBaseImponible());
                } else if ("EXPORTACION".equalsIgnoreCase(ta.getTipoTotal())) {
                    totalExportacion = totalExportacion.add(ta.getBaseImponible());
                }
            }
        } else if (d.getDetalles() != null) {
            for (LineaComprobante l : d.getDetalles()) {
                if (l.getTipoAfectacion() != null) {
                    BigDecimal base = l.getSubtotal() != null ? l.getSubtotal() : BigDecimal.ZERO;
                    if (l.getTipoAfectacion().isGratuito()) {
                        totalGratuita = totalGratuita.add(base);
                    } else if (l.getTipoAfectacion().isGravaIgv()) {
                        totalGravada = totalGravada.add(base);
                    } else if (l.getTipoAfectacion() == TipoAfectacionIgv.EXONERADO_OPERACION_ONEROSA) {
                        totalExonerada = totalExonerada.add(base);
                    } else if (l.getTipoAfectacion() == TipoAfectacionIgv.INAFECTO_OPERACION_ONEROSA) {
                        totalInafecta = totalInafecta.add(base);
                    } else if (l.getTipoAfectacion() == TipoAfectacionIgv.EXPORTACION_BIENES_SERVICIOS) {
                        totalExportacion = totalExportacion.add(base);
                    }
                }
            }
        }

        BigDecimal subtotal = d.getSubtotal() != null ? d.getSubtotal() : totalGravada.add(totalExonerada).add(totalInafecta).add(totalExportacion);
        BigDecimal totalTributos = d.getTotalTributos() != null ? d.getTotalTributos() : totalIgv.add(totalIcbper).add(totalIsc);
        BigDecimal totalFinal = d.getTotal();
        BigDecimal totalGravadaFinal = (totalGravada.compareTo(BigDecimal.ZERO) > 0 || (totalExonerada.compareTo(BigDecimal.ZERO) == 0 && totalInafecta.compareTo(BigDecimal.ZERO) == 0 && totalExportacion.compareTo(BigDecimal.ZERO) == 0))
                ? totalGravada : null;

        TotalesNotaResponse totales = TotalesNotaResponse.builder()
                .totalGravada(totalGravadaFinal)
                .totalExonerada(totalExonerada.compareTo(BigDecimal.ZERO) > 0 ? totalExonerada : null)
                .totalInafecta(totalInafecta.compareTo(BigDecimal.ZERO) > 0 ? totalInafecta : null)
                .totalGratuita(totalGratuita.compareTo(BigDecimal.ZERO) > 0 ? totalGratuita : null)
                .totalExportacion(totalExportacion.compareTo(BigDecimal.ZERO) > 0 ? totalExportacion : null)
                .subtotal(subtotal)
                .totalOtrosCargos(d.getTotalOtrosCargos() != null && d.getTotalOtrosCargos().compareTo(BigDecimal.ZERO) > 0 ? d.getTotalOtrosCargos() : null)
                .totalDescuentos(d.getTotalDescuentos() != null && d.getTotalDescuentos().compareTo(BigDecimal.ZERO) > 0 ? d.getTotalDescuentos() : null)
                .totalTributos(totalTributos)
                .totalIgv(totalIgv)
                .totalIsc(totalIsc.compareTo(BigDecimal.ZERO) > 0 ? totalIsc : null)
                .totalIcbper(totalIcbper.compareTo(BigDecimal.ZERO) > 0 ? totalIcbper : null)
                .totalPagado(totalFinal)
                .total(totalFinal)
                .build();

        DocumentoReferenciaResponse refResponse = null;
        if (d.getReferencias() != null && !d.getReferencias().isEmpty()) {
            ReferenciaComprobante r = d.getReferencias().get(0);
            refResponse = DocumentoReferenciaResponse.builder()
                    .id(r.getId())
                    .tipoRelacion(r.getTipoRelacion())
                    .documentoReferenciadoId(r.getDocumentoReferenciadoId())
                    .tipoDocumentoRef(r.getTipoDocumentoRef())
                    .serieRef(r.getSerieRef())
                    .numeroRef(r.getNumeroRef())
                    .motivoCodigo(r.getMotivoCodigo())
                    .motivoDescripcion(r.getMotivoDescripcion())
                    .fechaEmisionRef(r.getFechaEmisionRef())
                    .monedaRef(r.getMonedaRef())
                    .createdAt(d.getFechaEmision())
                    .build();
        }

        return NotaResponse.builder()
                .id(d.getId())
                .claveIdempotencia(d.getClaveIdempotencia())
                .tipoComprobante(d.getTipoComprobante().getCodigo())
                .serie(d.getSerie())
                .numero(d.getNumero())
                .fechaEmision(d.getFechaEmision())
                .moneda(d.getMoneda())
                .estadoInterno(d.getEstadoInterno())
                .estadoSunat(d.getEstadoSunat())
                .codigoSunat(d.getCodigoSunat())
                .mensajeSunat(d.getMensajeSunat())
                .xmlUrl(d.getXmlUrl())
                .cdrUrl(d.getCdrUrl())
                .documentoReferencia(refResponse)
                .cliente(ClienteDto.builder()
                        .tipoDocumento(d.getClienteTipoDoc())
                        .numeroDocumento(d.getClienteNumeroDoc())
                        .nombreRazonSocial(d.getClienteNombre())
                        .direccion(d.getClienteDireccion())
                        .build())
                .items(items)
                .totales(totales)
                .hashCpe(d.getHashCpe())
                .createdAt(d.getFechaEmision())
                .updatedAt(d.getFechaEmision())
                .build();
    }
}
