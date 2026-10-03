package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest;

import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in.ConsultarComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in.EmitirComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.ComprobanteResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.CrearComprobanteRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.EstadoComprobanteResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.mapper.ComprobanteRestMapper;
import com.s1nt4xSystem.facturacion_sunat.shared.dto.ApiResponse;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.DocumentoArchivoStoragePort;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service.ImpresionComprobanteService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant/comprobantes")
@RequiredArgsConstructor
public class ComprobanteTenantController {

    private final EmitirComprobanteUseCase emitirComprobanteUseCase;
    private final ConsultarComprobanteUseCase consultarComprobanteUseCase;
    private final EmpresaRepository empresaRepository;
    private final ImpresionComprobanteService impresionComprobanteService;
    private final DocumentoArchivoStoragePort documentoArchivoStoragePort;

    @PostMapping
    public ResponseEntity<ApiResponse<ComprobanteResponse>> crearComprobante(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody CrearComprobanteRequest request) {

        if (request.getComprobante().getClaveIdempotencia() == null && idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            request.getComprobante().setClaveIdempotencia(idempotencyKeyHeader.trim());
        }

        ComprobanteFiscal emitido = emitirComprobanteUseCase.emitir(request);
        Map<String, String> enlaces = impresionComprobanteService.resolverEnlacesImpresion(
                emitido.getId(), emitido.getEmpresaId(), emitido.getSucursalId(), emitido.getXmlUrl(), emitido.getCdrUrl());

        ComprobanteResponse response = ComprobanteRestMapper.toComprobanteEmitidoResponse(emitido, enlaces);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Comprobante creado correctamente"));
    }

    @GetMapping(value = "/{id}/print-ticket", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> imprimirTicketHtml(
            @PathVariable UUID id,
            @RequestParam(value = "formato", required = false, defaultValue = "TICKET_80") String formato) {
        String html = impresionComprobanteService.generarHtml(id, formato);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/html;charset=UTF-8"))
                .body(html);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable UUID id,
            @RequestParam(value = "formato", required = false, defaultValue = "A4") String formato) {
        byte[] pdfBytes = impresionComprobanteService.generarPdf(id, formato);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = String.format("comprobante-%s-%s.pdf", id, formato.toLowerCase());
        headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> descargarXml(@PathVariable UUID id) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, id));

        if (doc.getXmlUrl() == null || doc.getXmlUrl().isBlank()) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, "El comprobante no tiene archivo XML generado");
        }

        byte[] xmlBytes = documentoArchivoStoragePort.leerArchivo(doc.getXmlUrl());
        String ruc = resolverRucEmpresa();
        String tipo = doc.getTipoComprobante() != null ? doc.getTipoComprobante().getCodigo() : "03";
        String filename = String.format("%s-%s-%s-%08d.xml", ruc, tipo, doc.getSerie(), doc.getNumero());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(xmlBytes.length);

        return new ResponseEntity<>(xmlBytes, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}/cdr", produces = "application/zip")
    public ResponseEntity<byte[]> descargarCdr(@PathVariable UUID id) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, id));

        if (doc.getCdrUrl() == null || doc.getCdrUrl().isBlank()) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, "El comprobante aún no cuenta con constancia de recepción (CDR) de SUNAT");
        }

        byte[] cdrBytes = documentoArchivoStoragePort.leerArchivo(doc.getCdrUrl());
        String ruc = resolverRucEmpresa();
        String tipo = doc.getTipoComprobante() != null ? doc.getTipoComprobante().getCodigo() : "03";
        String filename = String.format("R-%s-%s-%s-%08d.zip", ruc, tipo, doc.getSerie(), doc.getNumero());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(cdrBytes.length);

        return new ResponseEntity<>(cdrBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComprobanteResponse>> obtenerPorId(@PathVariable UUID id) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorId(id)
                .orElseThrow(() -> new com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException(
                        com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, id));

        String rucEmisor = resolverRucEmpresa();
        Map<String, String> enlaces = impresionComprobanteService.resolverEnlacesImpresion(
                doc.getId(), doc.getEmpresaId(), doc.getSucursalId(), doc.getXmlUrl(), doc.getCdrUrl());
        ComprobanteResponse response = ComprobanteRestMapper.toComprobanteResponse(doc, rucEmisor, doc.getCodigoSucursal(), enlaces);
        return ResponseEntity.ok(ApiResponse.ok(response, "Comprobante encontrado"));
    }

    @GetMapping("/idempotency/{clave}")
    public ResponseEntity<ApiResponse<ComprobanteResponse>> obtenerPorClaveIdempotencia(@PathVariable String clave) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorClaveIdempotencia(clave)
                .orElseThrow(() -> new com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException(
                        com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode.DOCUMENT_NOT_FOUND_BY_IDEMPOTENCY, clave));

        String rucEmisor = resolverRucEmpresa();
        Map<String, String> enlaces = impresionComprobanteService.resolverEnlacesImpresion(
                doc.getId(), doc.getEmpresaId(), doc.getSucursalId(), doc.getXmlUrl(), doc.getCdrUrl());
        ComprobanteResponse response = ComprobanteRestMapper.toComprobanteResponse(doc, rucEmisor, doc.getCodigoSucursal(), enlaces);
        return ResponseEntity.ok(ApiResponse.ok(response, "Comprobante encontrado"));
    }

    @GetMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<EstadoComprobanteResponse>> obtenerEstado(@PathVariable UUID id) {
        ComprobanteFiscal doc = consultarComprobanteUseCase.consultarPorId(id)
                .orElseThrow(() -> new com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException(
                        com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode.DOCUMENT_NOT_FOUND_BY_ID, id));

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
