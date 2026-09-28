package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model;

import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public enum TipoComprobante {
    FACTURA("01", "Factura Electrónica", "F", "^F[A-Z0-9]{3}$", true),
    BOLETA("03", "Boleta de Venta Electrónica", "B", "^B[A-Z0-9]{3}$", true),
    NOTA_CREDITO("07", "Nota de Crédito Electrónica", "FC", "^(F[A-Z0-9]{3}|B[A-Z0-9]{3}|FC[A-Z0-9]{2}|BC[A-Z0-9]{2})$", true),
    NOTA_DEBITO("08", "Nota de Débito Electrónica", "FD", "^(F[A-Z0-9]{3}|B[A-Z0-9]{3}|FD[A-Z0-9]{2}|BD[A-Z0-9]{2})$", true),
    GUIA_REMISION_REMITENTE("09", "Guía de Remisión Remitente", "T", "^(T[A-Z0-9]{3}|EG[A-Z0-9]{2})$", true),
    GUIA_REMISION_TRANSPORTISTA("31", "Guía de Remisión Transportista", "V", "^V[A-Z0-9]{3}$", true),
    RETENCION("20", "Comprobante de Retención", "R", "^R[A-Z0-9]{3}$", true),
    PERCEPCION("40", "Comprobante de Percepción", "P", "^P[A-Z0-9]{3}$", true),
    TICKET_INTERNO("00", "Ticket / Nota de Venta Interna", "NV", "^(?:T[0-9]{3}|(?!^[FBRVPE])[A-Z0-9]{4})$", false);

    private final String codigo;
    private final String descripcion;
    private final String prefijoSerieDefault;
    private final String patronSerieRegex;
    private final boolean esElectronico;
    private final Pattern pattern;

    TipoComprobante(String codigo, String descripcion, String prefijoSerieDefault, String patronSerieRegex, boolean esElectronico) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.prefijoSerieDefault = prefijoSerieDefault;
        this.patronSerieRegex = patronSerieRegex;
        this.esElectronico = esElectronico;
        this.pattern = Pattern.compile(patronSerieRegex);
    }

    public static TipoComprobante fromCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new DomainException("El tipo de comprobante es obligatorio");
        }
        String cod = codigo.trim();
        for (TipoComprobante tipo : values()) {
            if (tipo.getCodigo().equalsIgnoreCase(cod)) {
                return tipo;
            }
        }
        throw new DomainException("Tipo de comprobante no soportado o inválido: " + codigo);
    }

    public void validarSerie(String serie) {
        if (serie == null || serie.trim().isEmpty()) {
            throw new DomainException("La serie es obligatoria para " + this.descripcion);
        }
        String serieLimpia = serie.trim().toUpperCase();
        if (!this.pattern.matcher(serieLimpia).matches()) {
            throw new DomainException(String.format(
                    "La serie '%s' no tiene un formato válido para %s (código %s). Debe cumplir con el patrón: %s",
                    serieLimpia, this.descripcion, this.codigo, this.patronSerieRegex));
        }
    }
}
