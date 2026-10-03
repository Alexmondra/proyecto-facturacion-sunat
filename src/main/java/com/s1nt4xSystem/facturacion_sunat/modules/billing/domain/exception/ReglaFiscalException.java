package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.exception;

import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;

public class ReglaFiscalException extends BusinessException {
    private final String codigoError;

    public ReglaFiscalException(String codigoError, String message) {
        super(resolveErrorCode(codigoError), message);
        this.codigoError = codigoError;
    }

    public ReglaFiscalException(String message) {
        super(ErrorCode.DOCUMENT_CLIENT_INVALID, message);
        this.codigoError = "REGLA_FISCAL_INVALIDA";
    }

    public String getCodigoError() {
        return codigoError;
    }

    private static ErrorCode resolveErrorCode(String codigoError) {
        if (codigoError == null) return ErrorCode.DOCUMENT_CLIENT_INVALID;
        return switch (codigoError) {
            case "DOCUMENT_ITEMS_EMPTY", "LINEAS_VACIAS" -> ErrorCode.DOCUMENT_ITEMS_EMPTY;
            case "DOCUMENT_SERIE_REQUIRED", "SERIE_REQUERIDA" -> ErrorCode.DOCUMENT_SERIE_REQUIRED;
            case "FACTURA_CLIENTE_TIPO_DOC_INVALIDO" -> ErrorCode.DOCUMENT_FACTURA_RUC_REQUIRED;
            case "FACTURA_RUC_INVALIDO" -> ErrorCode.DOCUMENT_FACTURA_RUC_INVALID;
            case "FACTURA_SERIE_INVALIDA" -> ErrorCode.DOCUMENT_FACTURA_SERIE_INVALID;
            case "BOLETA_SERIE_INVALIDA" -> ErrorCode.DOCUMENT_BOLETA_SERIE_INVALID;
            case "BOLETA_IDENTIFICACION_REQUERIDA", "BOLETA_NUMERO_DOC_REQUERIDO" -> ErrorCode.DOCUMENT_BOLETA_CLIENT_ID_REQUIRED;
            case "NOTA_REFERENCIA_REQUERIDA" -> ErrorCode.DOCUMENT_REF_REQUIRED;
            case "REFERENCIA_TIPO_DOC_REQUERIDO" -> ErrorCode.DOCUMENT_REF_TYPE_REQUIRED;
            case "REFERENCIA_SERIE_REQUERIDA" -> ErrorCode.DOCUMENT_REF_SERIE_REQUIRED;
            case "REFERENCIA_NUMERO_REQUERIDO" -> ErrorCode.DOCUMENT_REF_NUMERO_REQUIRED;
            case "REFERENCIA_MOTIVO_REQUERIDO" -> ErrorCode.DOCUMENT_REF_MOTIVO_REQUIRED;
            case "CREDITO_CUOTAS_REQUERIDAS" -> ErrorCode.DOCUMENT_CREDITO_CUOTAS_REQUIRED;
            case "CREDITO_SUMA_CUOTAS_INVALIDA" -> ErrorCode.DOCUMENT_CREDITO_CUOTAS_MISMATCH;
            case "DETRACCION_CODIGO_BIEN_REQUERIDO" -> ErrorCode.DOCUMENT_DETRACCION_CODE_REQUIRED;
            case "DETRACCION_PORCENTAJE_INVALIDO" -> ErrorCode.DOCUMENT_DETRACCION_PERCENT_INVALID;
            case "DETRACCION_CUENTA_REQUERIDA" -> ErrorCode.DOCUMENT_DETRACCION_ACCOUNT_REQUIRED;
            default -> ErrorCode.DOCUMENT_CLIENT_INVALID;
        };
    }
}
