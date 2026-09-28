package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.application.service;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.company.repository.EmpresaConfigRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ComprobanteFiscal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.ResultadoFirma;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.SunatSoapResult;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcesadorComprobanteElectronicoService {

    private final GeneradorXmlPort generadorXmlPort;
    private final FirmaDigitalPort firmaDigitalPort;
    private final SunatSoapPort sunatSoapPort;
    private final DocumentoArchivoStoragePort documentoArchivoStoragePort;
    private final DocumentoSunatPersistencePort documentoSunatPersistencePort;
    private final DocumentoRepositoryPort documentoRepositoryPort;
    private final EmpresaConfigRepository empresaConfigRepository;

    @Transactional
    public ComprobanteFiscal procesarFirmaYEnvio(ComprobanteFiscal comprobante, Empresa empresa, Sucursal sucursal) {
        if (comprobante == null || comprobante.getId() == null || !comprobante.getTipoComprobante().isEsElectronico()) {
            return comprobante;
        }

        try {
            Optional<EmpresaConfig> configOpt = empresaConfigRepository.findByEmpresaId(empresa.getId());
            if (configOpt.isEmpty()) {
                log.info("No se encontró configuración fiscal para empresa {}. Se omite firma/envío a SUNAT.", empresa.getRuc());
                return comprobante;
            }
            EmpresaConfig config = configOpt.get();

            String ruc = empresa.getRuc().trim();
            String tipoCodigo = comprobante.getTipoComprobante().getCodigo();
            String serie = comprobante.getSerie();
            int numero = comprobante.getNumero();
            String nombreBase = String.format("%s-%s-%s-%08d", ruc, tipoCodigo, serie, numero);

            // 1. Generación de XML UBL 2.1
            byte[] xmlBytes = generadorXmlPort.generarXml(comprobante, empresa, sucursal);

            // 2. Firma Digital XMLDSig (usa certificado configurado o demo de respaldo en Beta/pruebas)
            ResultadoFirma firma = firmaDigitalPort.firmarXml(xmlBytes, config.getCertificado(), config.getCertificadoPass());
            String hashCpe = firma.getHashCpe();
            byte[] xmlFirmado = firma.getXmlFirmado();

            // 3. Guardar XML firmado en storage y registrar en documento_archivos
            String rutaXmlFirmado = documentoArchivoStoragePort.guardarArchivo(ruc, "xml", nombreBase + ".xml", xmlFirmado);
            documentoSunatPersistencePort.registrarArchivo(comprobante.getId(), "XML", rutaXmlFirmado, nombreBase + ".xml");

            String nuevoEstado = "FIRMADO";
            String estadoSunat = null;
            String codigoSunat = null;
            String mensajeSunat = null;
            String rutaCdrZip = null;

            // 4. Envío a SUNAT si se tienen credenciales SOL configuradas (o MODDATOS de prueba)
            if (config.getUserSol() == null || config.getUserSol().isBlank()) {
                config.setUserSol("MODDATOS");
            }
            if (config.getPassSol() == null || config.getPassSol().isBlank()) {
                config.setPassSol("moddatos");
            }

            SunatSoapResult soapResult = sunatSoapPort.enviarComprobante(xmlFirmado, nombreBase, empresa, config);

            // Guardar ZIP oficial del CDR (único archivo necesario para el CDR)
            if (soapResult.getCdrZip() != null && soapResult.getCdrZip().length > 0) {
                String cdrNombreZip = (soapResult.getCdrNombreArchivo() != null && !soapResult.getCdrNombreArchivo().isBlank())
                        ? soapResult.getCdrNombreArchivo() : "R-" + nombreBase + ".zip";
                rutaCdrZip = documentoArchivoStoragePort.guardarArchivo(ruc, "cdr", cdrNombreZip, soapResult.getCdrZip());
                documentoSunatPersistencePort.registrarArchivo(comprobante.getId(), "CDR", rutaCdrZip, cdrNombreZip);
            }

            if (soapResult.getResponse() != null) {
                documentoSunatPersistencePort.guardarRespuestaSunat(comprobante.getId(), soapResult.getResponse());
                estadoSunat = soapResult.getResponse().getEstadoSunat();
                codigoSunat = soapResult.getResponse().getCodigoRespuestaSunat();
                mensajeSunat = soapResult.getResponse().getMensajeSunat();
            }

            // 5. Actualizar hash_cpe en documentos manteniendo estado_interno='REGISTRADO' (lo de SUNAT va en documento_sunat)
            documentoRepositoryPort.actualizarHashYEstado(comprobante.getId(), hashCpe, "REGISTRADO");

            return ComprobanteFiscal.builder()
                    .id(comprobante.getId())
                    .claveIdempotencia(comprobante.getClaveIdempotencia())
                    .empresaId(comprobante.getEmpresaId())
                    .sucursalId(comprobante.getSucursalId())
                    .codigoSucursal(comprobante.getCodigoSucursal())
                    .serieId(comprobante.getSerieId())
                    .tipoComprobante(comprobante.getTipoComprobante())
                    .serie(comprobante.getSerie())
                    .numero(comprobante.getNumero())
                    .fechaEmision(comprobante.getFechaEmision())
                    .moneda(comprobante.getMoneda())
                    .tipoOperacion(comprobante.getTipoOperacion())
                    .clienteTipoDoc(comprobante.getClienteTipoDoc())
                    .clienteNumeroDoc(comprobante.getClienteNumeroDoc())
                    .clienteNombre(comprobante.getClienteNombre())
                    .clienteDireccion(comprobante.getClienteDireccion())
                    .formaPago(comprobante.getFormaPago())
                    .totalOtrosCargos(comprobante.getTotalOtrosCargos())
                    .totalTributos(comprobante.getTotalTributos())
                    .subtotal(comprobante.getSubtotal())
                    .total(comprobante.getTotal())
                    .totalDescuentos(comprobante.getTotalDescuentos())
                    .estadoInterno("REGISTRADO")
                    .hashCpe(hashCpe)
                    .estadoSunat(estadoSunat)
                    .codigoSunat(codigoSunat)
                    .mensajeSunat(mensajeSunat)
                    .xmlUrl(rutaXmlFirmado)
                    .cdrUrl(rutaCdrZip)
                    .detalles(comprobante.getDetalles())
                    .totalesAfectacion(comprobante.getTotalesAfectacion())
                    .tributosGlobales(comprobante.getTributosGlobales())
                    .cuotas(comprobante.getCuotas())
                    .detraccion(comprobante.getDetraccion())
                    .referencias(comprobante.getReferencias())
                    .build();

        } catch (Exception e) {
            log.error("Excepción en fase de firma y envío para comprobante {}: {}", comprobante.getId(), e.getMessage(), e);
            documentoRepositoryPort.actualizarHashYEstado(comprobante.getId(), null, "ERROR_PROCESO");
            return comprobante;
        }
    }
}
