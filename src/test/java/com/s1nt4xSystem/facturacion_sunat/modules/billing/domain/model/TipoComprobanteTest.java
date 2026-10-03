package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model;

import com.s1nt4xSystem.facturacion_sunat.shared.errors.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TipoComprobanteTest {

    @ParameterizedTest
    @ValueSource(strings = {"F001", "F002", "FA01", "F999", "FZZZ"})
    @DisplayName("Factura (01) debe aceptar series válidas ^F[A-Z0-9]{3}$")
    void factura_aceptaSeriesValidas(String serie) {
        assertThatCode(() -> TipoComprobante.FACTURA.validarSerie(serie))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"B001", "F01", "F0001", "F-01", "1001", "T001", "NV01"})
    @DisplayName("Factura (01) debe rechazar series inválidas")
    void factura_rechazaSeriesInvalidas(String serie) {
        assertThatThrownBy(() -> TipoComprobante.FACTURA.validarSerie(serie))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no tiene un formato válido para Factura Electrónica");
    }

    @ParameterizedTest
    @ValueSource(strings = {"B001", "B002", "BA01", "B999", "BZZZ"})
    @DisplayName("Boleta (03) debe aceptar series válidas ^B[A-Z0-9]{3}$")
    void boleta_aceptaSeriesValidas(String serie) {
        assertThatCode(() -> TipoComprobante.BOLETA.validarSerie(serie))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"F001", "B01", "B0001", "B-01", "1001", "NV01"})
    @DisplayName("Boleta (03) debe rechazar series inválidas")
    void boleta_rechazaSeriesInvalidas(String serie) {
        assertThatThrownBy(() -> TipoComprobante.BOLETA.validarSerie(serie))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no tiene un formato válido para Boleta de Venta Electrónica");
    }

    @ParameterizedTest
    @ValueSource(strings = {"FC01", "BC01", "F001", "B001", "FC99", "BC99"})
    @DisplayName("Nota de Crédito (07) debe aceptar series oficiales")
    void notaCredito_aceptaSeriesValidas(String serie) {
        assertThatCode(() -> TipoComprobante.NOTA_CREDITO.validarSerie(serie))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"FD01", "BD01", "F001", "B001", "FD99", "BD99"})
    @DisplayName("Nota de Débito (08) debe aceptar series oficiales")
    void notaDebito_aceptaSeriesValidas(String serie) {
        assertThatCode(() -> TipoComprobante.NOTA_DEBITO.validarSerie(serie))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NV01", "NV02", "TK01", "NP01", "T001", "T002", "A001", "N001"})
    @DisplayName("Ticket Interno (00) debe aceptar series de venta interna (NV01, TK01, T001)")
    void ticketInterno_aceptaSeriesValidas(String serie) {
        assertThatCode(() -> TipoComprobante.TICKET_INTERNO.validarSerie(serie))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"F001", "B001", "R001", "P001", "V001", "E001", "NV1", "NV0001"})
    @DisplayName("Ticket Interno (00) debe rechazar series reservadas de SUNAT (F, B, R, P, V, E) o longitudes inválidas")
    void ticketInterno_rechazaSeriesReservadas(String serie) {
        assertThatThrownBy(() -> TipoComprobante.TICKET_INTERNO.validarSerie(serie))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no tiene un formato válido para Ticket / Nota de Venta Interna");
    }

    @Test
    @DisplayName("esElectronico debe ser false para TICKET_INTERNO y true para CPE")
    void verificarFlagsEsElectronico() {
        assertThat(TipoComprobante.TICKET_INTERNO.isEsElectronico()).isFalse();
        assertThat(TipoComprobante.FACTURA.isEsElectronico()).isTrue();
        assertThat(TipoComprobante.BOLETA.isEsElectronico()).isTrue();
        assertThat(TipoComprobante.NOTA_CREDITO.isEsElectronico()).isTrue();
        assertThat(TipoComprobante.NOTA_DEBITO.isEsElectronico()).isTrue();
    }
}
