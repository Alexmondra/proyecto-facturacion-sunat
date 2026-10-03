package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in.ConsultarComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in.EmitirNotaUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.CrearNotaRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.EstadoNotaResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.NotaResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.mapper.ComprobanteRestMapper;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant/notas")
@RequiredArgsConstructor
public class NotaTenantController {

    private final EmitirNotaUseCase emitirNotaUseCase;
    private final ConsultarComprobanteUseCase consultarComprobanteUseCase;
    private final EmpresaRepository empresaRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<NotaResponse>> crearNota(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody CrearNotaRequest request) {

        if (request.getNota().getClaveIdempotencia() == null && idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            request.getNota().setClaveIdempotencia(idempotencyKeyHeader.trim());
        }

        ComprobanteFiscal emitido = emitirNotaUseCase.emitirNota(request);
        String rucEmisor = resolverRucEmpresa();

        NotaResponse response = ComprobanteRestMapper.toNotaResponse(emitido, rucEmisor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Nota creada correctamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotaResponse>> obtenerPorId(@PathVariable UUID id) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorId(id)
                .orElseThrow(() -> new com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException(
                        com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, id));

        String rucEmisor = resolverRucEmpresa();
        NotaResponse response = ComprobanteRestMapper.toNotaResponse(doc, rucEmisor);
        return ResponseEntity.ok(ApiResponse.ok(response, "Nota encontrada"));
    }

    @GetMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<EstadoNotaResponse>> obtenerEstado(@PathVariable UUID id) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorId(id)
                .orElseThrow(() -> new com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException(
                        com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, id));

        EstadoNotaResponse estado = EstadoNotaResponse.builder()
                .id(doc.getId())
                .estado(doc.getEstadoInterno())
                .estadoSunat(null)
                .mensaje("Nota " + doc.getEstadoInterno().toLowerCase())
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
