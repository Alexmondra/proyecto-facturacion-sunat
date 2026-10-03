package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.HasRawPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrearNotaRequest implements HasRawPayload {

    @JsonIgnore
    private String rawPayload;

    @Valid
    @Builder.Default
    private EmisorDto emisor = new EmisorDto();

    @Valid
    @NotNull(message = "El bloque nota es obligatorio")
    private NotaDataRequest nota;

    @Valid
    @JsonProperty("documento_referencia")
    private DocumentoReferenciaRequest documentoReferencia;

    @Valid
    private ClienteDto cliente;

    @Valid
    @Builder.Default
    private List<ItemComprobanteRequest> items = new ArrayList<>();

    public DocumentoReferenciaRequest getDocumentoReferencia() {
        if (this.documentoReferencia != null) {
            return this.documentoReferencia;
        }
        if (this.nota != null) {
            if (this.nota.getDocumentoReferencia() != null) {
                return this.nota.getDocumentoReferencia();
            }
            if (this.nota.getDocumentoReferenciadoId() != null) {
                return DocumentoReferenciaRequest.builder()
                        .documentoReferenciadoId(this.nota.getDocumentoReferenciadoId())
                        .build();
            }
        }
        return null;
    }

    public ClienteDto getCliente() {
        if (this.cliente != null) {
            return this.cliente;
        }
        if (this.nota != null) {
            return this.nota.getCliente();
        }
        return null;
    }

    public List<ItemComprobanteRequest> getItems() {
        if (this.items != null && !this.items.isEmpty()) {
            return this.items;
        }
        if (this.nota != null && this.nota.getItems() != null && !this.nota.getItems().isEmpty()) {
            return this.nota.getItems();
        }
        return this.items != null ? this.items : new ArrayList<>();
    }
}
