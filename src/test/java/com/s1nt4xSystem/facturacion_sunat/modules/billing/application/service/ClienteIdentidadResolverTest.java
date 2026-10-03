package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ClienteIdentidad;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.DatosClienteIdentidad;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.ConsultaIdentidadPort;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteIdentidadResolverTest {

    @Mock
    private ConsultaIdentidadPort consultaIdentidadPort;

    private ClienteIdentidadResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClienteIdentidadResolver(consultaIdentidadPort);
    }

    @Test
    @DisplayName("Boleta < 700 sin DNI asigna CLIENTES VARIOS")
    void boletaMenor700SinDni_asignaClientesVarios() {
        ClienteIdentidad cliente = resolver.resolverYValidar(
                "03", null, null, null, null, new BigDecimal("150.00"));

        assertThat(cliente.tipoDocumento()).isEqualTo("0");
        assertThat(cliente.numeroDocumento()).isEqualTo("00000000");
        assertThat(cliente.denominacion()).isEqualTo("CLIENTES VARIOS");
        assertThat(cliente.verificadoExternamente()).isFalse();

        verifyNoInteractions(consultaIdentidadPort);
    }

    @Test
    @DisplayName("Boleta < 700 con 00000000 asigna CLIENTES VARIOS")
    void boletaMenor700ConCeros_asignaClientesVarios() {
        ClienteIdentidad cliente = resolver.resolverYValidar(
                "03", "0", "00000000", "", null, new BigDecimal("50.00"));

        assertThat(cliente.denominacion()).isEqualTo("CLIENTES VARIOS");
        verifyNoInteractions(consultaIdentidadPort);
    }

    @Test
    @DisplayName("Boleta >= 700 sin documento lanza error DOCUMENT_BOLETA_CLIENT_ID_REQUIRED")
    void boletaMayorOIgual700SinDocumento_lanzaError() {
        assertThatThrownBy(() -> resolver.resolverYValidar(
                "03", null, "", null, null, new BigDecimal("700.00")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DOCUMENT_BOLETA_CLIENT_ID_REQUIRED);
    }

    @Test
    @DisplayName("DNI encontrado en Servicio Central rectifica y sobreescribe nombre oficial")
    void dniEncontrado_rectificaNombreOficial() {
        DatosClienteIdentidad datosApi = DatosClienteIdentidad.builder()
                .tipoDocumento("1")
                .numeroDocumento("44556677")
                .denominacion("PEREZ PRADO JUAN CARLOS")
                .direccion("Av. Primavera 123")
                .build();

        when(consultaIdentidadPort.consultarDocumento("1", "44556677"))
                .thenReturn(Optional.of(datosApi));

        ClienteIdentidad resultado = resolver.resolverYValidar(
                "03", "1", "44556677", "juancho perez", null, new BigDecimal("850.00"));

        assertThat(resultado.denominacion()).isEqualTo("PEREZ PRADO JUAN CARLOS");
        assertThat(resultado.numeroDocumento()).isEqualTo("44556677");
        assertThat(resultado.direccion()).isEqualTo("Av. Primavera 123");
        assertThat(resultado.verificadoExternamente()).isTrue();
    }

    @Test
    @DisplayName("DNI encontrado en Servicio Central autocompleta nombre cuando viene vacío")
    void dniEncontrado_autocompletaNombreVacio() {
        DatosClienteIdentidad datosApi = DatosClienteIdentidad.builder()
                .tipoDocumento("1")
                .numeroDocumento("77889900")
                .denominacion("MENDOZA RUIZ MARIA")
                .build();

        when(consultaIdentidadPort.consultarDocumento("1", "77889900"))
                .thenReturn(Optional.of(datosApi));

        ClienteIdentidad resultado = resolver.resolverYValidar(
                "03", "1", "77889900", null, null, new BigDecimal("120.00"));

        assertThat(resultado.denominacion()).isEqualTo("MENDOZA RUIZ MARIA");
        assertThat(resultado.verificadoExternamente()).isTrue();
    }

    @Test
    @DisplayName("DNI NO encontrado en Servicio Central pero con nombre enviado: usa nombre provisto (Fallback)")
    void dniNoEncontrado_conNombre_usaFallback() {
        when(consultaIdentidadPort.consultarDocumento("1", "12345678"))
                .thenReturn(Optional.empty());

        ClienteIdentidad resultado = resolver.resolverYValidar(
                "03", "1", "12345678", "CARLOS ALCANTARA", "Calle Las Flores 456", new BigDecimal("300.00"));

        assertThat(resultado.denominacion()).isEqualTo("CARLOS ALCANTARA");
        assertThat(resultado.numeroDocumento()).isEqualTo("12345678");
        assertThat(resultado.direccion()).isEqualTo("Calle Las Flores 456");
        assertThat(resultado.verificadoExternamente()).isFalse();
    }

    @Test
    @DisplayName("DNI NO encontrado en Servicio Central y sin nombre enviado: lanza error DOCUMENT_CLIENT_NAME_REQUIRED")
    void dniNoEncontrado_sinNombre_lanzaError() {
        when(consultaIdentidadPort.consultarDocumento("1", "12345678"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolverYValidar(
                "03", "1", "12345678", "   ", null, new BigDecimal("300.00")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DOCUMENT_CLIENT_NAME_REQUIRED);
    }

    @Test
    @DisplayName("Factura con tipo distinto a 6 lanza DOCUMENT_FACTURA_RUC_REQUIRED")
    void facturaTipoNoRuc_lanzaError() {
        assertThatThrownBy(() -> resolver.resolverYValidar(
                "01", "1", "44556677", "JUAN PEREZ", null, new BigDecimal("1000.00")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DOCUMENT_FACTURA_RUC_REQUIRED);
    }

    @Test
    @DisplayName("Factura con RUC de longitud inválida lanza DOCUMENT_FACTURA_RUC_INVALID")
    void facturaRucInvalido_lanzaError() {
        assertThatThrownBy(() -> resolver.resolverYValidar(
                "01", "6", "201234567", "EMPRESA SAC", null, new BigDecimal("1000.00")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DOCUMENT_FACTURA_RUC_INVALID);
    }

    @Test
    @DisplayName("Factura con RUC encontrado en Servicio Central rectifica razón social")
    void facturaRucEncontrado_rectificaRazonSocial() {
        DatosClienteIdentidad datosApi = DatosClienteIdentidad.builder()
                .tipoDocumento("6")
                .numeroDocumento("20601234567")
                .denominacion("INVERSIONES TECNOLOGICAS DEL PERU S.A.C.")
                .direccion("Av. Central 789")
                .estado("ACTIVO")
                .condicion("HABIDO")
                .build();

        when(consultaIdentidadPort.consultarDocumento("6", "20601234567"))
                .thenReturn(Optional.of(datosApi));

        ClienteIdentidad resultado = resolver.resolverYValidar(
                "01", "6", "20601234567", "inversiones sac", null, new BigDecimal("2500.00"));

        assertThat(resultado.denominacion()).isEqualTo("INVERSIONES TECNOLOGICAS DEL PERU S.A.C.");
        assertThat(resultado.numeroDocumento()).isEqualTo("20601234567");
        assertThat(resultado.direccion()).isEqualTo("Av. Central 789");
        assertThat(resultado.verificadoExternamente()).isTrue();
    }
}
