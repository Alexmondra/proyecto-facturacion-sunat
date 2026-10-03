package com.s1nt4xSystem.facturacion_sunat.shared.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Excepción estándar para infracciones a reglas de negocio de la plataforma.
 * Contiene el código de error tipado (ErrorCode), mensaje formateado y detalles adicionales (extraInfo).
 */
@Getter
public class BusinessException extends DomainException {

    private final ErrorCode errorCode;
    private final Map<String, Object> extraInfo;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessageTemplate());
        this.errorCode = errorCode;
        this.extraInfo = Collections.emptyMap();
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(formatMessage(errorCode.getMessageTemplate(), args));
        this.errorCode = errorCode;
        this.extraInfo = Collections.emptyMap();
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> extraInfo, Object... args) {
        super(formatMessage(errorCode.getMessageTemplate(), args));
        this.errorCode = errorCode;
        this.extraInfo = extraInfo != null ? Collections.unmodifiableMap(extraInfo) : Collections.emptyMap();
    }

    public BusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(formatMessage(errorCode.getMessageTemplate(), args), cause);
        this.errorCode = errorCode;
        this.extraInfo = Collections.emptyMap();
    }

    public HttpStatus getHttpStatus() {
        return errorCode != null ? errorCode.getHttpStatus() : HttpStatus.BAD_REQUEST;
    }

    public int getCode() {
        return errorCode != null ? errorCode.getCode() : 0;
    }

    private static String formatMessage(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }
}
