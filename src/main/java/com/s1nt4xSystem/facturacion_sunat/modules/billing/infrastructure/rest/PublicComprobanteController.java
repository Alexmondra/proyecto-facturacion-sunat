package com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.multitenancy.TenantContext;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service.ImpresionComprobanteService;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.in.ConsultarComprobanteUseCase;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/comprobantes")
@RequiredArgsConstructor
@Slf4j
public class PublicComprobanteController {

    private final ImpresionComprobanteService impresionComprobanteService;
    private final ConsultarComprobanteUseCase consultarComprobanteUseCase;
    private final EmpresaRouterRepository empresaRouterRepository;

    @GetMapping("/{ruc}/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdfPublico(
            @PathVariable String ruc,
            @PathVariable UUID id,
            @RequestParam(value = "formato", required = false, defaultValue = "A4") String formato) {

        String rucLimpio = ruc != null ? ruc.trim() : "";
        EmpresaRouter router = empresaRouterRepository.findByRuc(rucLimpio)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND, rucLimpio));

        try {
            TenantContext.setCurrentTenant(router.getDbSchema());

            byte[] pdfBytes = impresionComprobanteService.generarPdf(id, formato);

            String nombreArchivo = resolverNombreArchivo(id, rucLimpio);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename(nombreArchivo).build());
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolverNombreArchivo(UUID id, String ruc) {
        try {
            return consultarComprobanteUseCase.consultarPorId(id)
                    .map(doc -> String.format("%s-%s-%s-%08d.pdf",
                            ruc,
                            doc.getTipoComprobante() != null ? doc.getTipoComprobante().getCodigo() : "03",
                            doc.getSerie(),
                            doc.getNumero()))
                    .orElse(String.format("%s-%s.pdf", ruc, id));
        } catch (Exception e) {
            return String.format("%s-%s.pdf", ruc, id);
        }
    }
}
