package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.servicios.identidad")
public class IdentidadProperties {

    /**
     * URL para consulta de DNI (8 dígitos).
     */
    private String dniUrl = "https://api-central.tuservicio.com/api/v1/dni";

    /**
     * URL para consulta de RUC (11 dígitos).
     */
    private String rucUrl = "https://api-central.tuservicio.com/api/v1/ruc";

    /**
     * API Key o Token para autenticación en el servicio central.
     */
    private String apiKey;

    /**
     * Tiempo de espera máximo en milisegundos para la consulta externa.
     */
    private int timeoutMs = 3000;
}
