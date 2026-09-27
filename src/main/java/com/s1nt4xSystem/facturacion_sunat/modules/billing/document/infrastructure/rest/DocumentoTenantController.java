package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto.ComprobanteEmitidoResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.dto.EmitirComprobanteCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in.ConsultarComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in.EmitirComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant/documents")
@RequiredArgsConstructor
public class DocumentoTenantController {

    private final EmitirComprobanteUseCase emitirComprobanteUseCase;
    private final ConsultarComprobanteUseCase consultarComprobanteUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<ComprobanteEmitidoResponse>> emitir(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody EmitirComprobanteCommand command) {

        String clave = command.getClaveIdempotencia();
        if ((clave == null || clave.trim().isEmpty()) && idempotencyKeyHeader != null && !idempotencyKeyHeader.trim().isEmpty()) {
            clave = idempotencyKeyHeader.trim();
        }

        EmitirComprobanteCommand commandToExecute = command;
        if (clave != null && !clave.equals(command.getClaveIdempotencia())) {
            commandToExecute = EmitirComprobanteCommand.builder()
                    .claveIdempotencia(clave)
                    .sucursalId(command.getSucursalId())
                    .tipoComprobante(command.getTipoComprobante())
                    .serie(command.getSerie())
                    .moneda(command.getMoneda())
                    .tipoOperacion(command.getTipoOperacion())
                    .clienteTipoDoc(command.getClienteTipoDoc())
                    .clienteNumeroDoc(command.getClienteNumeroDoc())
                    .clienteNombre(command.getClienteNombre())
                    .clienteDireccion(command.getClienteDireccion())
                    .formaPago(command.getFormaPago())
                    .descuentoGlobal(command.getDescuentoGlobal())
                    .items(command.getItems())
                    .cuotas(command.getCuotas())
                    .detraccion(command.getDetraccion())
                    .referencias(command.getReferencias())
                    .build();
        }

        ComprobanteFiscal emitido = emitirComprobanteUseCase.emitir(commandToExecute);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ComprobanteEmitidoResponse.fromDomain(emitido), "Comprobante emitido exitosamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComprobanteFiscal>> consultarPorId(@PathVariable UUID id) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún comprobante con el ID: " + id));
        return ResponseEntity.ok(ApiResponse.ok(doc, "Comprobante encontrado"));
    }

    @GetMapping("/idempotency/{clave}")
    public ResponseEntity<ApiResponse<ComprobanteFiscal>> consultarPorClaveIdempotencia(@PathVariable String clave) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorClaveIdempotencia(clave)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún comprobante con la clave de idempotencia: " + clave));
        return ResponseEntity.ok(ApiResponse.ok(doc, "Comprobante encontrado"));
    }
}
