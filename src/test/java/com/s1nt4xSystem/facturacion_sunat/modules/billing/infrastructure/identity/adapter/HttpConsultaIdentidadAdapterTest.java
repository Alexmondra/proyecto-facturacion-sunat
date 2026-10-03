package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.identity.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.DatosClienteIdentidad;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.identity.config.IdentidadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpConsultaIdentidadAdapterTest {

    private IdentidadProperties properties;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private HttpConsultaIdentidadAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new IdentidadProperties();
        properties.setDniUrl("https://api.central.test/v1/dni");
        properties.setRucUrl("https://api.central.test/v1/ruc");
        properties.setApiKey("test-secret-key");
        properties.setTimeoutMs(2000);

        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        adapter = new HttpConsultaIdentidadAdapter(properties, restClientBuilder.build());
    }

    @Test
    @DisplayName("Consulta DNI exitosa con JSON que contiene nombres y apellidos separados")
    void consultarDni_conNombresYApellidos_parseaCorrectamente() {
        String json = """
                {
                    "data": {
                        "nombres": "JUAN CARLOS",
                        "apellido_paterno": "PEREZ",
                        "apellido_materno": "GARCIA",
                        "direccion": "Av. Los Alamos 123"
                    }
                }
                """;

        mockServer.expect(requestTo("https://api.central.test/v1/dni/72345678"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "test-secret-key"))
                .andExpect(header("Authorization", "Bearer test-secret-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<DatosClienteIdentidad> resultado = adapter.consultarDocumento("1", "72345678");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().denominacion()).isEqualTo("PEREZ GARCIA JUAN CARLOS");
        assertThat(resultado.get().numeroDocumento()).isEqualTo("72345678");
        assertThat(resultado.get().direccion()).isEqualTo("Av. Los Alamos 123");
        mockServer.verify();
    }

    @Test
    @DisplayName("Consulta RUC exitosa con JSON que contiene razon_social y estado ACTIVO")
    void consultarRuc_conRazonSocial_parseaCorrectamente() {
        String json = """
                {
                    "razon_social": "SERVICIOS TECNOLOGICOS SAC",
                    "estado": "ACTIVO",
                    "condicion": "HABIDO",
                    "direccion_fiscal": "Calle Principal 456"
                }
                """;

        mockServer.expect(requestTo("https://api.central.test/v1/ruc/20601234567"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<DatosClienteIdentidad> resultado = adapter.consultarDocumento("6", "20601234567");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().denominacion()).isEqualTo("SERVICIOS TECNOLOGICOS SAC");
        assertThat(resultado.get().estado()).isEqualTo("ACTIVO");
        assertThat(resultado.get().condicion()).isEqualTo("HABIDO");
        assertThat(resultado.get().isActivo()).isTrue();
        assertThat(resultado.get().isHabido()).isTrue();
        assertThat(resultado.get().direccion()).isEqualTo("Calle Principal 456");
        mockServer.verify();
    }

    @Test
    @DisplayName("Respuesta 404 de servicio central devuelve Optional.empty() sin lanzar excepción")
    void servicioRetorna404_retornaOptionalEmpty() {
        mockServer.expect(requestTo("https://api.central.test/v1/dni/99999999"))
                .andRespond(withResourceNotFound());

        Optional<DatosClienteIdentidad> resultado = adapter.consultarDocumento("1", "99999999");

        assertThat(resultado).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("Respuesta 500 de servicio central devuelve Optional.empty() tolerando el fallo")
    void servicioRetorna500_retornaOptionalEmpty() {
        mockServer.expect(requestTo("https://api.central.test/v1/dni/88888888"))
                .andRespond(withServerError());

        Optional<DatosClienteIdentidad> resultado = adapter.consultarDocumento("1", "88888888");

        assertThat(resultado).isEmpty();
        mockServer.verify();
    }
}
