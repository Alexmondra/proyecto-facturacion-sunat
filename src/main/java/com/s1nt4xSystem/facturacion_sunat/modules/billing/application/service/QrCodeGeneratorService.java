package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoTributoSunat;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TributoLinea;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class QrCodeGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String generarCadenaQrSunat(ComprobanteFiscal doc, String rucEmisor) {
        String ruc = rucEmisor != null ? rucEmisor.trim() : "";
        String tipoDoc = doc.getTipoComprobante() != null ? doc.getTipoComprobante().getCodigo() : "03";
        String serie = doc.getSerie() != null ? doc.getSerie() : "";
        int numero = doc.getNumero() != null ? doc.getNumero() : 0;

        BigDecimal totalIgv = BigDecimal.ZERO;
        if (doc.getTributosGlobales() != null) {
            for (TributoLinea t : doc.getTributosGlobales()) {
                if (TipoTributoSunat.IGV.coincideCon(t.getCodigoTributo())) {
                    totalIgv = totalIgv.add(t.getMonto());
                }
            }
        }
        if (totalIgv.compareTo(BigDecimal.ZERO) == 0 && doc.getTotalTributos() != null) {
            totalIgv = doc.getTotalTributos();
        }

        BigDecimal total = doc.getTotal() != null ? doc.getTotal() : BigDecimal.ZERO;
        String fecha = doc.getFechaEmision() != null ? doc.getFechaEmision().format(DATE_FORMATTER) : "";
        String clienteTipoDoc = doc.getClienteTipoDoc() != null ? doc.getClienteTipoDoc() : "-";
        String clienteNumDoc = doc.getClienteNumeroDoc() != null ? doc.getClienteNumeroDoc() : "-";
        String hash = doc.getHashCpe() != null ? doc.getHashCpe() : "";

        return String.format("%s|%s|%s|%d|%.2f|%.2f|%s|%s|%s|%s|",
                ruc, tipoDoc, serie, numero, totalIgv, total, fecha, clienteTipoDoc, clienteNumDoc, hash);
    }

    public byte[] generarImagenQrPng(String contenido, int ancho, int alto) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(contenido, BarcodeFormat.QR_CODE, ancho, alto, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generando imagen QR: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    public String generarQrBase64(String contenido, int ancho, int alto) {
        byte[] bytes = generarImagenQrPng(contenido, ancho, alto);
        if (bytes.length == 0) {
            return "";
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
