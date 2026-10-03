package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.identity.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.DatosClienteIdentidad;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.ConsultaIdentidadPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.identity.config.IdentidadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HttpConsultaIdentidadAdapter implements ConsultaIdentidadPort {

    private final IdentidadProperties properties;
    private final RestClient restClient;

    @Autowired
    public HttpConsultaIdentidadAdapter(IdentidadProperties properties) {
        this(properties, RestClient.builder());
    }

    public HttpConsultaIdentidadAdapter(IdentidadProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public HttpConsultaIdentidadAdapter(IdentidadProperties properties,
                                        RestClient.Builder restClientBuilder) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeout = properties.getTimeoutMs() > 0 ? properties.getTimeoutMs() : 3000;
        requestFactory.setConnectTimeout(Duration.ofMillis(timeout));
        requestFactory.setReadTimeout(Duration.ofMillis(timeout));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<DatosClienteIdentidad> consultarDocumento(String tipoDocumento, String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            return Optional.empty();
        }

        String numDocLimpio = numeroDocumento.trim();
        String urlTemplate = resolverUrl(tipoDocumento, numDocLimpio);
        if (urlTemplate == null || urlTemplate.isBlank()) {
            log.warn("No hay URL configurada para consultar documento tipo: {}", tipoDocumento);
            return Optional.empty();
        }

        String finalUrl = construirUrl(urlTemplate, numDocLimpio);

        try {
            var requestSpec = restClient.get().uri(finalUrl);

            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                requestSpec.header("X-API-KEY", properties.getApiKey());
                requestSpec.header("Authorization", "Bearer " + properties.getApiKey());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = requestSpec
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, resp) -> {
                        log.warn("Servicio central de identidad respondió HTTP {} para documento {}",
                                resp.getStatusCode(), numDocLimpio);
                    })
                    .body(Map.class);

            if (body == null || body.isEmpty()) {
                return Optional.empty();
            }

            return parsearRespuesta(tipoDocumento, numDocLimpio, body);

        } catch (Exception e) {
            log.warn("No se pudo consultar el documento {} en el servicio central: {}", numDocLimpio, e.getMessage());
            return Optional.empty();
        }
    }

    private String resolverUrl(String tipoDocumento, String numeroDocumento) {
        if ("1".equals(tipoDocumento) || numeroDocumento.length() == 8) {
            return properties.getDniUrl();
        }
        if ("6".equals(tipoDocumento) || numeroDocumento.length() == 11) {
            return properties.getRucUrl();
        }
        return null;
    }

    private String construirUrl(String urlTemplate, String numeroDocumento) {
        if (urlTemplate.contains("{numero}")) {
            return urlTemplate.replace("{numero}", numeroDocumento);
        }
        if (urlTemplate.contains("{dni}")) {
            return urlTemplate.replace("{dni}", numeroDocumento);
        }
        if (urlTemplate.contains("{ruc}")) {
            return urlTemplate.replace("{ruc}", numeroDocumento);
        }
        if (urlTemplate.endsWith("/")) {
            return urlTemplate + numeroDocumento;
        }
        return urlTemplate + "/" + numeroDocumento;
    }

    @SuppressWarnings("unchecked")
    private Optional<DatosClienteIdentidad> parsearRespuesta(String tipoDocumento, String numeroDocumento, Map<String, Object> body) {
        try {
            Map<String, Object> data = (body.get("data") instanceof Map<?, ?> dataMap)
                    ? (Map<String, Object>) dataMap
                    : body;

            String denominacion = extraerDenominacion(data);
            if (denominacion == null || denominacion.isBlank()) {
                return Optional.empty();
            }

            String direccion = extraerCampo(data, "direccion", "direccion_fiscal", "direccionFiscal");
            String estado = extraerCampo(data, "estado", "estado_contribuyente", "estadoContribuyente");
            String condicion = extraerCampo(data, "condicion", "condicion_domicilio", "condicionDomicilio");

            String tipoDocResuelto = (tipoDocumento != null && !tipoDocumento.isBlank())
                    ? tipoDocumento
                    : (numeroDocumento.length() == 11 ? "6" : "1");

            return Optional.of(DatosClienteIdentidad.builder()
                    .tipoDocumento(tipoDocResuelto)
                    .numeroDocumento(numeroDocumento)
                    .denominacion(denominacion.trim())
                    .direccion(direccion != null ? direccion.trim() : null)
                    .estado(estado != null ? estado.trim().toUpperCase() : null)
                    .condicion(condicion != null ? condicion.trim().toUpperCase() : null)
                    .build());

        } catch (Exception e) {
            log.error("Error al deserializar respuesta de identidad para {}: {}", numeroDocumento, e.getMessage());
            return Optional.empty();
        }
    }

    private String extraerDenominacion(Map<String, Object> data) {
        // 1. Razón Social o Nombre Completo directo
        String valorDirecto = extraerCampo(data, "razon_social", "razonSocial", "nombre_completo", "nombreCompleto", "full_name", "nombre");
        if (valorDirecto != null && !valorDirecto.isBlank()) {
            return valorDirecto;
        }

        // 2. Partes de persona natural (DNI): nombres + apellidos
        String nombres = extraerCampo(data, "nombres", "first_name", "firstName");
        String apePaterno = extraerCampo(data, "apellido_paterno", "apellidoPaterno", "first_last_name", "firstLastName");
        String apeMaterno = extraerCampo(data, "apellido_materno", "apellidoMaterno", "second_last_name", "secondLastName");

        if (nombres != null || apePaterno != null || apeMaterno != null) {
            StringBuilder sb = new StringBuilder();
            if (apePaterno != null) sb.append(apePaterno).append(" ");
            if (apeMaterno != null) sb.append(apeMaterno).append(" ");
            if (nombres != null) sb.append(nombres);
            String resultado = sb.toString().trim();
            if (!resultado.isBlank()) {
                return resultado;
            }
        }

        return null;
    }

    private String extraerCampo(Map<String, Object> data, String... posiblesNombres) {
        for (String nombre : posiblesNombres) {
            Object val = data.get(nombre);
            if (val != null) {
                String str = String.valueOf(val).trim();
                if (!str.isBlank() && !"null".equalsIgnoreCase(str)) {
                    return str;
                }
            }
        }
        return null;
    }
}
