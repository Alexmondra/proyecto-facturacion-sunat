package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in.ConsultarComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.in.EmitirComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.ComprobanteResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.CrearComprobanteRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.dto.EstadoComprobanteResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.rest.mapper.ComprobanteRestMapper;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant/comprobantes")
@RequiredArgsConstructor
public class ComprobanteTenantController {

    private final EmitirComprobanteUseCase emitirComprobanteUseCase;
    private final ConsultarComprobanteUseCase consultarComprobanteUseCase;
    private final EmpresaRepository empresaRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ComprobanteResponse>> crearComprobante(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody CrearComprobanteRequest request) {

        if (request.getComprobante().getClaveIdempotencia() == null && idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            request.getComprobante().setClaveIdempotencia(idempotencyKeyHeader.trim());
        }

        ComprobanteFiscal emitido = emitirComprobanteUseCase.emitir(request);
        String rucEmisor = resolverRucEmpresa();

        ComprobanteResponse response = ComprobanteRestMapper.toComprobanteResponse(emitido, rucEmisor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Comprobante creado correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComprobanteResponse>> obtenerPorId(@PathVariable UUID id) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún comprobante con ID: " + id));

        String rucEmisor = resolverRucEmpresa();
        ComprobanteResponse response = ComprobanteRestMapper.toComprobanteResponse(doc, rucEmisor);
        return ResponseEntity.ok(ApiResponse.ok(response, "Comprobante encontrado"));
    }

    @GetMapping("/idempotency/{clave}")
    public ResponseEntity<ApiResponse<ComprobanteResponse>> obtenerPorClaveIdempotencia(@PathVariable String clave) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorClaveIdempotencia(clave)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún comprobante con clave de idempotencia: " + clave));

        String rucEmisor = resolverRucEmpresa();
        ComprobanteResponse response = ComprobanteRestMapper.toComprobanteResponse(doc, rucEmisor);
        return ResponseEntity.ok(ApiResponse.ok(response, "Comprobante encontrado"));
    }

    @GetMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<EstadoComprobanteResponse>> obtenerEstado(@PathVariable UUID id) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún comprobante con ID: " + id));

        EstadoComprobanteResponse estado = EstadoComprobanteResponse.builder()
                .id(doc.getId())
                .estado(doc.getEstadoInterno())
                .estadoSunat(doc.getEstadoSunat() != null ? doc.getEstadoSunat() : doc.getEstadoInterno())
                .mensaje(doc.getMensajeSunat() != null ? doc.getMensajeSunat() : ("Comprobante " + doc.getEstadoInterno().toLowerCase()))
                .createdAt(doc.getFechaEmision())
                .updatedAt(doc.getFechaEmision())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(estado));
    }

    private String resolverRucEmpresa() {
        return empresaRepository.findAll().stream().findFirst()
                .map(Empresa::getRuc)
                .orElse("00000000000");
    }
}
