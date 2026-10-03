package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.exception.ReglaFiscalException;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.CuotaPago;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.DetraccionFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.LineaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ReferenciaComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoComprobante;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validador de Reglas Fiscales SUNAT en Dominio Puro.
 * No depende de ningún framework ni base de datos.
 */
public class ValidadorReglasFiscales {

    private static final BigDecimal MONTO_LIMITE_BOLETA_ANONIMA = new BigDecimal("700.00");

    public void validar(ComprobanteFiscal doc) {
        if (doc == null) {
            throw new ReglaFiscalException("DOCUMENTO_NULO", "El comprobante fiscal no puede ser nulo");
        }

        validarCabeceraBasica(doc);
        validarLineas(doc.getLineas());
        validarSegunTipoComprobante(doc);
        validarFormaPago(doc);
        validarDetraccion(doc);
    }

    private void validarCabeceraBasica(ComprobanteFiscal doc) {
        if (doc.getTipoComprobante() == null) {
            throw new ReglaFiscalException("TIPO_COMPROBANTE_REQUERIDO", "El tipo de comprobante es obligatorio");
        }
        if (doc.getSerie() == null || doc.getSerie().trim().isEmpty()) {
            throw new ReglaFiscalException("SERIE_REQUERIDA", "La serie del comprobante es obligatoria");
        }
        if (doc.getMoneda() == null || doc.getMoneda().trim().isEmpty()) {
            throw new ReglaFiscalException("MONEDA_REQUERIDA", "La moneda es obligatoria (ej. PEN, USD)");
        }
        if (doc.getClienteNombre() == null || doc.getClienteNombre().trim().isEmpty()) {
            throw new ReglaFiscalException("CLIENTE_NOMBRE_REQUERIDO", "La razón social o nombre del cliente es obligatorio");
        }
    }

    private void validarLineas(List<LineaComprobante> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            throw new ReglaFiscalException("LINEAS_VACIAS", "El comprobante debe tener al menos una línea de detalle");
        }

        for (int i = 0; i < lineas.size(); i++) {
            LineaComprobante item = lineas.get(i);
            int numItem = i + 1;

            if (item.getDescripcion() == null || item.getDescripcion().trim().isEmpty()) {
                throw new ReglaFiscalException("ITEM_DESCRIPCION_REQUERIDA", "El ítem " + numItem + " no tiene descripción");
            }
            if (item.getCantidad() == null || item.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ReglaFiscalException("ITEM_CANTIDAD_INVALIDA", "El ítem " + numItem + " debe tener una cantidad mayor a 0");
            }
            if (item.getValorUnitario() == null || item.getValorUnitario().compareTo(BigDecimal.ZERO) < 0) {
                throw new ReglaFiscalException("ITEM_VALOR_UNITARIO_INVALIDO", "El ítem " + numItem + " tiene un valor unitario inválido");
            }
        }
    }

    private void validarSegunTipoComprobante(ComprobanteFiscal doc) {
        TipoComprobante tipo = doc.getTipoComprobante();

        if (tipo == TipoComprobante.FACTURA) {
            validarFactura(doc);
        } else if (tipo == TipoComprobante.BOLETA) {
            validarBoleta(doc);
        } else if (tipo == TipoComprobante.NOTA_CREDITO || tipo == TipoComprobante.NOTA_DEBITO) {
            validarNota(doc);
        }
    }

    private void validarFactura(ComprobanteFiscal doc) {
        if (!"6".equals(doc.getClienteTipoDoc())) {
            throw new ReglaFiscalException("FACTURA_CLIENTE_TIPO_DOC_INVALIDO",
                    "Para emitir una Factura (01), el tipo de documento del cliente debe ser RUC (tipo '6')");
        }

        String ruc = doc.getClienteNumeroDoc();
        if (ruc == null || !ruc.matches("^\\d{11}$")) {
            throw new ReglaFiscalException("FACTURA_RUC_INVALIDO",
                    "El RUC del cliente debe tener exactamente 11 dígitos numéricos");
        }

        if (!doc.getSerie().toUpperCase().startsWith("F")) {
            throw new ReglaFiscalException("FACTURA_SERIE_INVALIDA",
                    "La serie de una Factura electrónica debe comenzar con 'F' (ej. F001)");
        }
    }

    private void validarBoleta(ComprobanteFiscal doc) {
        if (!doc.getSerie().toUpperCase().startsWith("B")) {
            throw new ReglaFiscalException("BOLETA_SERIE_INVALIDA",
                    "La serie de una Boleta de venta electrónica debe comenzar con 'B' (ej. B001)");
        }

        BigDecimal total = doc.getTotal() != null ? doc.getTotal() : BigDecimal.ZERO;
        if (total.compareTo(MONTO_LIMITE_BOLETA_ANONIMA) >= 0) {
            if ("0".equals(doc.getClienteTipoDoc()) || doc.getClienteTipoDoc() == null || doc.getClienteTipoDoc().trim().isEmpty()) {
                throw new ReglaFiscalException("BOLETA_IDENTIFICACION_REQUERIDA",
                        "Para boletas de venta iguales o mayores a S/ 700.00 es obligatoria la identificación del cliente (DNI, Carnet de extranjería o Pasaporte)");
            }
            if (doc.getClienteNumeroDoc() == null || doc.getClienteNumeroDoc().trim().isEmpty() || "00000000".equals(doc.getClienteNumeroDoc().trim())) {
                throw new ReglaFiscalException("BOLETA_NUMERO_DOC_REQUERIDO",
                        "Para boletas de venta iguales o mayores a S/ 700.00 se requiere un número de documento de identidad válido");
            }
        }
    }

    private void validarNota(ComprobanteFiscal doc) {
        List<ReferenciaComprobante> referencias = doc.getReferencias();
        if (referencias == null || referencias.isEmpty()) {
            throw new ReglaFiscalException("NOTA_REFERENCIA_REQUERIDA",
                    "Las Notas de Crédito y Débito deben incluir la referencia al comprobante que modifican");
        }

        for (ReferenciaComprobante ref : referencias) {
            if (ref.getTipoDocumentoRef() == null || ref.getTipoDocumentoRef().trim().isEmpty()) {
                throw new ReglaFiscalException("REFERENCIA_TIPO_DOC_REQUERIDO", "El tipo de documento referenciado es obligatorio");
            }
            if (ref.getSerieRef() == null || ref.getSerieRef().trim().isEmpty()) {
                throw new ReglaFiscalException("REFERENCIA_SERIE_REQUERIDA", "La serie del documento referenciado es obligatoria");
            }
            if (ref.getNumeroRef() == null || ref.getNumeroRef() <= 0) {
                throw new ReglaFiscalException("REFERENCIA_NUMERO_REQUERIDO", "El número del documento referenciado debe ser mayor a 0");
            }
            if (ref.getMotivoCodigo() == null || ref.getMotivoCodigo().trim().isEmpty()) {
                throw new ReglaFiscalException("REFERENCIA_MOTIVO_REQUERIDO", "El código del motivo de emisión según catálogo SUNAT es obligatorio");
            }
        }
    }

    private void validarFormaPago(ComprobanteFiscal doc) {
        if ("CREDITO".equalsIgnoreCase(doc.getFormaPago())) {
            List<CuotaPago> cuotas = doc.getCuotas();
            if (cuotas == null || cuotas.isEmpty()) {
                throw new ReglaFiscalException("CREDITO_CUOTAS_REQUERIDAS",
                        "Para operaciones con forma de pago al CRÉDITO debe especificarse al menos una cuota de pago");
            }

            BigDecimal totalCuotas = cuotas.stream()
                    .map(c -> c.getMonto() != null ? c.getMonto() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal montoEsperado = doc.getTotal();
            if (doc.getDetraccion() != null && doc.getDetraccion().getMontoDetraccion() != null) {
                montoEsperado = montoEsperado.subtract(doc.getDetraccion().getMontoDetraccion());
            }

            if (totalCuotas.compareTo(montoEsperado) != 0) {
                throw new ReglaFiscalException("CREDITO_SUMA_CUOTAS_INVALIDA",
                        String.format("La suma de las cuotas (%s) no coincide con el monto neto pendiente a crédito (%s)",
                                totalCuotas, montoEsperado));
            }
        }
    }

    private void validarDetraccion(ComprobanteFiscal doc) {
        DetraccionFiscal det = doc.getDetraccion();
        if (det != null) {
            if (det.getCodigoBienServicio() == null || det.getCodigoBienServicio().trim().isEmpty()) {
                throw new ReglaFiscalException("DETRACCION_CODIGO_BIEN_REQUERIDO",
                        "El código de bien/servicio sujeto a detracción es obligatorio (Catálogo 54)");
            }
            if (det.getPorcentaje() == null || det.getPorcentaje().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ReglaFiscalException("DETRACCION_PORCENTAJE_INVALIDO",
                        "El porcentaje de detracción debe ser mayor a 0");
            }
            if (det.getNumeroCuentaDetraccion() == null || det.getNumeroCuentaDetraccion().trim().isEmpty()) {
                throw new ReglaFiscalException("DETRACCION_CUENTA_REQUERIDA",
                        "El número de cuenta de detracción del Banco de la Nación es obligatorio");
            }
        }
    }
}
