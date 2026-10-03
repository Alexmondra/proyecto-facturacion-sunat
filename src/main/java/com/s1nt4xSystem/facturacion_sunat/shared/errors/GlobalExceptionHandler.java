package com.s1nt4xSystem.facturacion_sunat.shared.errors;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.exception.ReglaFiscalException;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 0. Excepciones de Negocio Tipadas con ErrorCode
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusinessException(BusinessException ex) {
        log.warn("Error de negocio [{}]: {}", ex.getCode(), ex.getMessage());
        Map<String, Object> extra = ex.getExtraInfo() != null && !ex.getExtraInfo().isEmpty() ? ex.getExtraInfo() : null;
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage(), extra));
    }

    // 1. Recurso no encontrado (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // 2. Ruta o Endpoint inexistente (404)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        String msg = String.format("La ruta solicitada no existe en el servidor: /%s", ex.getResourcePath());
        log.warn("Ruta inexistente: {}", msg);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(msg));
    }

    // 3. Infracción a Reglas Fiscales SUNAT (422)
    @ExceptionHandler(ReglaFiscalException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleReglaFiscalException(ReglaFiscalException ex) {
        log.warn("Regla fiscal no cumplida: [{}] {}", ex.getCodigoError(), ex.getMessage());
        Map<String, String> data = Map.of("codigoError", ex.getCodigoError(), "detalle", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("Infracción a regla fiscal SUNAT: " + ex.getMessage(), data));
    }

    // 4. Validación de campos con @Valid (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Error de validación en request: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error de validación en los campos enviados", errors));
    }

    // 5. Tipo de parámetro inválido en URL, ej. UUID mal formado o ID no numérico (400)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String tipoEsperado = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido";
        String msg = String.format("El valor '%s' es inválido para el parámetro '%s'. Se esperaba un valor de tipo %s.",
                ex.getValue(), ex.getName(), tipoEsperado);
        log.warn("Tipo de argumento incompatible: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg));
    }

    // 6. JSON mal formado o cuerpo de solicitud ilegible (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de petición JSON ilegible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("El cuerpo de la solicitud JSON es inválido o contiene errores de sintaxis"));
    }

    // 7. Parámetro de URL query faltante (400)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String msg = String.format("Falta el parámetro de consulta obligatorio en la URL: '%s' (tipo: %s)",
                ex.getParameterName(), ex.getParameterType());
        log.warn("Parámetro faltante: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg));
    }

    // 8. Método HTTP no soportado, ej. POST a ruta que solo admite GET (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String soportados = ex.getSupportedMethods() != null ? Arrays.toString(ex.getSupportedMethods()) : "N/A";
        String msg = String.format("El método HTTP '%s' no está soportado para esta ruta. Métodos permitidos: %s",
                ex.getMethod(), soportados);
        log.warn("Método HTTP no permitido: {}", msg);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(msg));
    }

    // 9. Conflicto de Integridad de Datos / Claves Duplicadas (409)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        log.warn("Violación de integridad de datos en BD: {}", rootMsg);

        String amigable = "Conflicto con registros existentes en la base de datos (registro duplicado o relación inválida)";
        if (rootMsg != null) {
            if (rootMsg.contains("duplicate key") || rootMsg.contains("unique") || rootMsg.contains("uq_")) {
                amigable = "Ya existe un registro con los mismos datos únicos en el sistema";
            } else if (rootMsg.contains("foreign key") || rootMsg.contains("fk_")) {
                amigable = "No se puede completar la operación debido a una restricción de clave foránea o relación inexistente";
            }
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(amigable));
    }

    // 10. Argumentos ilegales o estados inválidos de negocio (400)
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleIllegalArguments(RuntimeException ex) {
        log.warn("Error de operación o argumento: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // 11. Errores de dominio personalizados (400)
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        log.warn("Error de dominio: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // 12. Error no controlado / Excepción genérica (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Error no controlado en el servidor: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ha ocurrido un error interno en el servidor: " + ex.getMessage()));
    }
}
