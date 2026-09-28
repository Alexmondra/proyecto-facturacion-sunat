package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ComprobanteFiscal {

    private final UUID id;
    private final String claveIdempotencia;
    private final UUID empresaId;
    private final UUID sucursalId;
    private final String codigoSucursal;
    private final UUID serieId;

    private final TipoComprobante tipoComprobante;
    private final String serie;
    private final Integer numero;
    private final LocalDateTime fechaEmision;

    @Builder.Default
    private final String moneda = "PEN";
    @Builder.Default
    private final String tipoOperacion = "0101"; // Venta interna

    // Datos del Cliente Receptor
    private final String clienteTipoDoc; // "6": RUC, "1": DNI, "-": Venta menor
    private final String clienteNumeroDoc;
    private final String clienteNombre;
    private final String clienteDireccion;

    // Forma de Pago
    @Builder.Default
    private final String formaPago = "CONTADO"; // "CONTADO" o "CREDITO"

    // Totales Calculados
    private final BigDecimal totalOtrosCargos;
    private final BigDecimal totalTributos; // Suma de todos los tributos
    private final BigDecimal subtotal; // Total valor venta
    private final BigDecimal total; // Total importe a pagar
    private final BigDecimal totalDescuentos;

    @Builder.Default
    private final String estadoInterno = "REGISTRADO";
    private final String hashCpe;
    private final String payloadEntrada;

    // Auditoría y Respuesta SUNAT
    private final String estadoSunat;
    private final String codigoSunat;
    private final String mensajeSunat;
    private final String xmlUrl;
    private final String cdrUrl;

    // Relaciones hijas
    @Builder.Default
    private final List<LineaComprobante> detalles = new ArrayList<>();

    @Builder.Default
    private final List<TotalesAfectacion> totalesAfectacion = new ArrayList<>();

    @Builder.Default
    private final List<TributoLinea> tributosGlobales = new ArrayList<>();

    @Builder.Default
    private final List<CuotaPago> cuotas = new ArrayList<>();

    private final DetraccionFiscal detraccion;

    @Builder.Default
    private final List<ReferenciaComprobante> referencias = new ArrayList<>();

    public List<LineaComprobante> getLineas() {
        return detalles;
    }

    public BigDecimal getSubtotal() {
        if (subtotal != null) {
            return subtotal;
        }
        if (totalesAfectacion == null || totalesAfectacion.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return totalesAfectacion.stream()
                .map(TotalesAfectacion::getBaseImponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
