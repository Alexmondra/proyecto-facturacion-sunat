package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.exception;

public class ReglaFiscalException extends RuntimeException {
    private final String codigoError;

    public ReglaFiscalException(String codigoError, String message) {
        super(message);
        this.codigoError = codigoError;
    }

    public ReglaFiscalException(String message) {
        super(message);
        this.codigoError = "REGLA_FISCAL_INVALIDA";
    }

    public String getCodigoError() {
        return codigoError;
    }
}
