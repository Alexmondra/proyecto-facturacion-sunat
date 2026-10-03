package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.ClienteIdentidad;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.DatosClienteIdentidad;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out.ConsultaIdentidadPort;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteIdentidadResolver {

    private static final BigDecimal LIMITE_BOLETA_IDENTIFICACION = new BigDecimal("700.00");
    private final ConsultaIdentidadPort consultaIdentidadPort;

    /**
     * Resuelve, valida y rectifica los datos del cliente según el tipo de comprobante,
     * monto total y reglas de negocio con tolerancia a fallos.
     */
    public ClienteIdentidad resolverYValidar(String tipoComprobante,
                                            String tipoDocEnviado,
                                            String numeroDocEnviado,
                                            String nombreEnviado,
                                            String direccionEnviada,
                                            BigDecimal totalComprobante) {

        String numDoc = numeroDocEnviado != null ? numeroDocEnviado.trim() : "";
        String tipoDoc = tipoDocEnviado != null ? tipoDocEnviado.trim() : "";
        String nombre = nombreEnviado != null ? nombreEnviado.trim() : "";
        String direccion = direccionEnviada != null ? direccionEnviada.trim() : null;

        // Auto-detección de tipo de documento por longitud si no fue enviado
        if (tipoDoc.isBlank() && !numDoc.isBlank()) {
            if (numDoc.length() == 8 && numDoc.matches("\\d{8}")) {
                tipoDoc = "1"; // DNI
            } else if (numDoc.length() == 11 && numDoc.matches("\\d{11}")) {
                tipoDoc = "6"; // RUC
            }
        }

        // =====================================================================
        // 1. BOLETA DE VENTA (03)
        // =====================================================================
        if ("03".equals(tipoComprobante)) {
            boolean sinDocumento = numDoc.isBlank() || "00000000".equals(numDoc) || "-".equals(numDoc) || "0".equals(numDoc);

            if (sinDocumento) {
                if (totalComprobante != null && totalComprobante.compareTo(LIMITE_BOLETA_IDENTIFICACION) >= 0) {
                    throw new BusinessException(ErrorCode.DOCUMENT_BOLETA_CLIENT_ID_REQUIRED);
                }
                return ClienteIdentidad.clientesVarios();
            }

            // Con documento proporcionado
            return procesarConDocumento(tipoDoc, numDoc, nombre, direccion);
        }

        // =====================================================================
        // 2. FACTURA (01)
        // =====================================================================
        if ("01".equals(tipoComprobante)) {
            if (!"6".equals(tipoDoc)) {
                throw new BusinessException(ErrorCode.DOCUMENT_FACTURA_RUC_REQUIRED);
            }
            if (!numDoc.matches("\\d{11}")) {
                throw new BusinessException(ErrorCode.DOCUMENT_FACTURA_RUC_INVALID);
            }

            return procesarConDocumento(tipoDoc, numDoc, nombre, direccion);
        }

        // =====================================================================
        // 3. OTROS COMPROBANTES (Notas 07, 08, etc.)
        // =====================================================================
        if (numDoc.isBlank() || "00000000".equals(numDoc) || "-".equals(numDoc)) {
            return ClienteIdentidad.clientesVarios();
        }

        return procesarConDocumento(tipoDoc, numDoc, nombre, direccion);
    }

    private ClienteIdentidad procesarConDocumento(String tipoDoc,
                                                 String numDoc,
                                                 String nombreEnviado,
                                                 String direccionEnviada) {
        // Consultar al servicio central
        Optional<DatosClienteIdentidad> consultaOpt = consultaIdentidadPort.consultarDocumento(tipoDoc, numDoc);

        if (consultaOpt.isPresent()) {
            DatosClienteIdentidad datos = consultaOpt.get();
            log.info("Identidad de cliente verificada con éxito para {}: '{}'", numDoc, datos.denominacion());

            return ClienteIdentidad.builder()
                    .tipoDocumento(datos.tipoDocumento() != null ? datos.tipoDocumento() : tipoDoc)
                    .numeroDocumento(datos.numeroDocumento() != null ? datos.numeroDocumento() : numDoc)
                    .denominacion(datos.denominacion()) // Sobreescribe con el nombre oficial
                    .direccion((direccionEnviada != null && !direccionEnviada.isBlank()) ? direccionEnviada : datos.direccion())
                    .verificadoExternamente(true)
                    .build();
        }

        // Si NO fue encontrado en el servicio central o hubo timeout:
        if (!nombreEnviado.isBlank()) {
            log.warn("Documento {} no encontrado en el servicio central. Emitiendo con nombre provisto: '{}'",
                    numDoc, nombreEnviado);

            return ClienteIdentidad.builder()
                    .tipoDocumento(tipoDoc)
                    .numeroDocumento(numDoc)
                    .denominacion(nombreEnviado)
                    .direccion(direccionEnviada)
                    .verificadoExternamente(false)
                    .build();
        }

        // Si no se encontró y tampoco enviaron nombre, bloquear emisión solicitando nombre
        throw new BusinessException(ErrorCode.DOCUMENT_CLIENT_NAME_REQUIRED, numDoc);
    }
}
