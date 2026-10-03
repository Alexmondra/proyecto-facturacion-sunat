package com.s1nt4xSystem.facturacion_sunat.shared.web;

import com.s1nt4xSystem.facturacion_sunat.shared.dto.HasRawPayload;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@ControllerAdvice
public class RawPayloadCaptureAdvice extends RequestBodyAdviceAdapter {

    private final ThreadLocal<String> capturedRawPayload = new ThreadLocal<>();

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return HasRawPayload.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                          Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        byte[] bytes = inputMessage.getBody().readAllBytes();
        String rawJson = new String(bytes, StandardCharsets.UTF_8);
        capturedRawPayload.set(rawJson);

        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        try {
            if (body instanceof HasRawPayload hasRawPayload) {
                hasRawPayload.setRawPayload(capturedRawPayload.get());
            }
        } finally {
            capturedRawPayload.remove();
        }
        return body;
    }
}
