package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ComprobanteEmitidoResponse {

    private UUID id;
    private String claveIdempotencia;
    private UUID empresaId;
    private UUID sucursalId;
    private String tipoComprobante;
    private String serie;
    private Integer numero;
    private String numeroCompleto; // F001-00000001
    private LocalDateTime fechaEmision;
    private String moneda;
    private String clienteTipoDoc;
    private String clienteNumeroDoc;
    private String clienteNombre;
    private String formaPago;
    private BigDecimal subtotal;
    private BigDecimal totalTributos;
    private BigDecimal total;
    private String estadoInterno;
    private int totalItems;

    public static ComprobanteEmitidoResponse fromDomain(ComprobanteFiscal doc) {
        String numFormateado = doc.getNumero() != null
                ? String.format("%s-%08d", doc.getSerie(), doc.getNumero())
                : doc.getSerie();

        return ComprobanteEmitidoResponse.builder()
                .id(doc.getId())
                .claveIdempotencia(doc.getClaveIdempotencia())
                .empresaId(doc.getEmpresaId())
                .sucursalId(doc.getSucursalId())
                .tipoComprobante(doc.getTipoComprobante() != null ? doc.getTipoComprobante().getCodigo() : null)
                .serie(doc.getSerie())
                .numero(doc.getNumero())
                .numeroCompleto(numFormateado)
                .fechaEmision(doc.getFechaEmision())
                .moneda(doc.getMoneda())
                .clienteTipoDoc(doc.getClienteTipoDoc())
                .clienteNumeroDoc(doc.getClienteNumeroDoc())
                .clienteNombre(doc.getClienteNombre())
                .formaPago(doc.getFormaPago())
                .subtotal(doc.getSubtotal())
                .totalTributos(doc.getTotalTributos())
                .total(doc.getTotal())
                .estadoInterno(doc.getEstadoInterno())
                .totalItems(doc.getDetalles() != null ? doc.getDetalles().size() : 0)
                .build();
    }
}
