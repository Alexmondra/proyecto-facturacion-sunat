package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.model.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.DocumentoRepositoryPort;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.entity.*;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.repository.DocumentoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentoRepositoryAdapter implements DocumentoRepositoryPort {

    private final DocumentoJpaRepository jpaRepository;
    private final SucursalRepository sucursalRepository;

    @Override
    public ComprobanteFiscal guardar(ComprobanteFiscal domain) {
        DocumentoEntity entity = toEntity(domain);
        DocumentoEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ComprobanteFiscal> buscarPorClaveIdempotencia(String claveIdempotencia) {
        return jpaRepository.findByClaveIdempotencia(claveIdempotencia)
                .map(this::toDomain);
    }

    @Override
    public Optional<ComprobanteFiscal> buscarPorId(UUID id) {
        return jpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<ComprobanteFiscal> buscarPorEmision(UUID empresaId, String tipoComprobante, String serie, Integer numero) {
        return jpaRepository.findByEmision(empresaId, tipoComprobante, serie, numero)
                .map(this::toDomain);
    }

    private DocumentoEntity toEntity(ComprobanteFiscal d) {
        DocumentoEntity entity = DocumentoEntity.builder()
                .id(d.getId())
                .claveIdempotencia(d.getClaveIdempotencia())
                .empresaId(d.getEmpresaId())
                .sucursalId(d.getSucursalId())
                .serieId(d.getSerieId())
                .tipoComprobante(d.getTipoComprobante() != null ? d.getTipoComprobante().getCodigo() : null)
                .serie(d.getSerie())
                .numero(d.getNumero())
                .fechaEmision(d.getFechaEmision())
                .moneda(d.getMoneda())
                .tipoOperacion(d.getTipoOperacion())
                .clienteTipoDoc(d.getClienteTipoDoc())
                .clienteNumeroDoc(d.getClienteNumeroDoc())
                .clienteNombre(d.getClienteNombre())
                .formaPago(d.getFormaPago())
                .totalOtrosCargos(d.getTotalOtrosCargos())
                .totalTributos(d.getTotalTributos())
                .total(d.getTotal())
                .estadoInterno(d.getEstadoInterno())
                .hashCpe(d.getHashCpe())
                .detalles(new ArrayList<>())
                .tributos(new ArrayList<>())
                .totalesAfectacion(new ArrayList<>())
                .cuotas(new ArrayList<>())
                .referencias(new ArrayList<>())
                .build();

        // Mapear detalles
        if (d.getDetalles() != null) {
            for (LineaComprobante l : d.getDetalles()) {
                DocumentoDetalleEntity det = DocumentoDetalleEntity.builder()
                        .documento(entity)
                        .item(l.getItem())
                        .descripcion(l.getDescripcion())
                        .cantidad(l.getCantidad())
                        .unidadMedida(l.getUnidadMedida())
                        .valorUnitario(l.getValorUnitario())
                        .tipoAfectacion(l.getTipoAfectacion() != null ? l.getTipoAfectacion().getCodigo() : "10")
                        .totalTributos(l.getTotalTributos())
                        .total(l.getTotal())
                        .tributos(new ArrayList<>())
                        .build();

                if (l.getTributos() != null) {
                    for (TributoLinea tl : l.getTributos()) {
                        DocumentoDetalleTributoEntity dte = DocumentoDetalleTributoEntity.builder()
                                .detalle(det)
                                .tributoId(tl.getTributoId() != null ? tl.getTributoId() : 1L)
                                .baseImponible(tl.getBaseImponible())
                                .porcentaje(tl.getPorcentaje())
                                .cantidadBase(tl.getCantidadBase())
                                .monto(tl.getMonto())
                                .build();
                        det.getTributos().add(dte);
                    }
                }
                entity.getDetalles().add(det);
            }
        }

        // Mapear tributos globales
        if (d.getTributosGlobales() != null) {
            for (TributoLinea tg : d.getTributosGlobales()) {
                DocumentoTributoEntity dte = DocumentoTributoEntity.builder()
                        .documento(entity)
                        .tributoId(tg.getTributoId() != null ? tg.getTributoId() : 1L)
                        .baseImponible(tg.getBaseImponible())
                        .monto(tg.getMonto())
                        .build();
                entity.getTributos().add(dte);
            }
        }

        // Mapear totales afectación
        if (d.getTotalesAfectacion() != null) {
            for (TotalesAfectacion ta : d.getTotalesAfectacion()) {
                DocumentoTotalesAfectacionEntity tae = DocumentoTotalesAfectacionEntity.builder()
                        .documento(entity)
                        .tipoAfectacionCodigo(ta.getTipoAfectacionCodigo())
                        .tipoTotal(ta.getTipoTotal())
                        .baseImponible(ta.getBaseImponible())
                        .montoTributo(ta.getMontoTributo())
                        .montoTotal(ta.getMontoTotal())
                        .build();
                entity.getTotalesAfectacion().add(tae);
            }
        }

        // Mapear cuotas
        if (d.getCuotas() != null) {
            for (CuotaPago c : d.getCuotas()) {
                DocumentoCuotaEntity ce = DocumentoCuotaEntity.builder()
                        .documento(entity)
                        .numeroCuota(c.getNumeroCuota())
                        .fechaVencimiento(c.getFechaVencimiento())
                        .monto(c.getMonto())
                        .moneda(c.getMoneda() != null ? c.getMoneda() : "PEN")
                        .build();
                entity.getCuotas().add(ce);
            }
        }

        // Mapear detracción
        if (d.getDetraccion() != null) {
            DetraccionFiscal df = d.getDetraccion();
            DocumentoDetraccionEntity de = DocumentoDetraccionEntity.builder()
                    .documento(entity)
                    .codigoBienServicio(df.getCodigoBienServicio())
                    .porcentaje(df.getPorcentaje())
                    .montoDetraccion(df.getMontoDetraccion())
                    .moneda(df.getMoneda() != null ? df.getMoneda() : "PEN")
                    .medioPago(df.getMedioPago())
                    .numeroCuentaDetraccion(df.getNumeroCuentaDetraccion())
                    .build();
            entity.setDetraccion(de);
        }

        // Mapear referencias
        if (d.getReferencias() != null) {
            for (ReferenciaComprobante r : d.getReferencias()) {
                DocumentoReferenciaEntity re = DocumentoReferenciaEntity.builder()
                        .documento(entity)
                        .tipoRelacion(r.getTipoRelacion())
                        .documentoReferenciadoId(r.getDocumentoReferenciadoId())
                        .tipoDocumentoRef(r.getTipoDocumentoRef())
                        .serieRef(r.getSerieRef())
                        .numeroRef(r.getNumeroRef())
                        .motivoCodigo(r.getMotivoCodigo())
                        .motivoDescripcion(r.getMotivoDescripcion())
                        .fechaEmisionRef(r.getFechaEmisionRef())
                        .monedaRef(r.getMonedaRef())
                        .build();
                entity.getReferencias().add(re);
            }
        }

        return entity;
    }

    private ComprobanteFiscal toDomain(DocumentoEntity e) {
        List<LineaComprobante> lineas = new ArrayList<>();
        if (e.getDetalles() != null) {
            for (DocumentoDetalleEntity d : e.getDetalles()) {
                List<TributoLinea> tribs = new ArrayList<>();
                if (d.getTributos() != null) {
                    for (DocumentoDetalleTributoEntity t : d.getTributos()) {
                        String codigoTributo = "1000";
                        String nombreTributo = "IGV";
                        String tipoTributo = "VAT";
                        if (t.getTributoId() != null) {
                            if (t.getTributoId() == 3L || (t.getTributoId() == 2L && t.getCantidadBase() != null)) {
                                codigoTributo = "7152";
                                nombreTributo = "ICBPER";
                                tipoTributo = "OTH";
                            } else if (t.getTributoId() == 2L) {
                                codigoTributo = "2000";
                                nombreTributo = "ISC";
                                tipoTributo = "EXC";
                            }
                        }
                        tribs.add(TributoLinea.builder()
                                .tributoId(t.getTributoId())
                                .codigoTributo(codigoTributo)
                                .nombreTributo(nombreTributo)
                                .tipoTributo(tipoTributo)
                                .baseImponible(t.getBaseImponible())
                                .porcentaje(t.getPorcentaje())
                                .cantidadBase(t.getCantidadBase())
                                .monto(t.getMonto())
                                .build());
                    }
                }

                lineas.add(LineaComprobante.builder()
                        .item(d.getItem())
                        .descripcion(d.getDescripcion())
                        .cantidad(d.getCantidad())
                        .unidadMedida(d.getUnidadMedida())
                        .valorUnitario(d.getValorUnitario())
                        .tipoAfectacion(TipoAfectacionIgv.fromCodigo(d.getTipoAfectacion()))
                        .totalTributos(d.getTotalTributos())
                        .total(d.getTotal())
                        .tributos(tribs)
                        .build());
            }
        }

        List<TotalesAfectacion> totalesAf = new ArrayList<>();
        if (e.getTotalesAfectacion() != null) {
            for (DocumentoTotalesAfectacionEntity t : e.getTotalesAfectacion()) {
                totalesAf.add(TotalesAfectacion.builder()
                        .tipoAfectacionCodigo(t.getTipoAfectacionCodigo())
                        .tipoTotal(t.getTipoTotal())
                        .baseImponible(t.getBaseImponible())
                        .montoTributo(t.getMontoTributo())
                        .montoTotal(t.getMontoTotal())
                        .build());
            }
        }

        List<CuotaPago> cuotas = new ArrayList<>();
        if (e.getCuotas() != null) {
            for (DocumentoCuotaEntity c : e.getCuotas()) {
                cuotas.add(CuotaPago.builder()
                        .numeroCuota(c.getNumeroCuota())
                        .fechaVencimiento(c.getFechaVencimiento())
                        .monto(c.getMonto())
                        .moneda(c.getMoneda())
                        .build());
            }
        }

        DetraccionFiscal det = null;
        if (e.getDetraccion() != null) {
            det = DetraccionFiscal.builder()
                    .codigoBienServicio(e.getDetraccion().getCodigoBienServicio())
                    .porcentaje(e.getDetraccion().getPorcentaje())
                    .montoDetraccion(e.getDetraccion().getMontoDetraccion())
                    .moneda(e.getDetraccion().getMoneda())
                    .medioPago(e.getDetraccion().getMedioPago())
                    .numeroCuentaDetraccion(e.getDetraccion().getNumeroCuentaDetraccion())
                    .build();
        }

        List<ReferenciaComprobante> refs = new ArrayList<>();
        if (e.getReferencias() != null) {
            for (DocumentoReferenciaEntity r : e.getReferencias()) {
                refs.add(ReferenciaComprobante.builder()
                        .id(r.getId())
                        .tipoRelacion(r.getTipoRelacion())
                        .documentoReferenciadoId(r.getDocumentoReferenciadoId())
                        .tipoDocumentoRef(r.getTipoDocumentoRef())
                        .serieRef(r.getSerieRef())
                        .numeroRef(r.getNumeroRef())
                        .motivoCodigo(r.getMotivoCodigo())
                        .motivoDescripcion(r.getMotivoDescripcion())
                        .fechaEmisionRef(r.getFechaEmisionRef())
                        .monedaRef(r.getMonedaRef())
                        .build());
            }
        }

        String codigoSucursal = null;
        if (e.getSucursalId() != null) {
            codigoSucursal = sucursalRepository.findById(e.getSucursalId())
                    .map(Sucursal::getCodigo)
                    .orElse("0000");
        }

        return ComprobanteFiscal.builder()
                .id(e.getId())
                .claveIdempotencia(e.getClaveIdempotencia())
                .empresaId(e.getEmpresaId())
                .sucursalId(e.getSucursalId())
                .codigoSucursal(codigoSucursal)
                .serieId(e.getSerieId())
                .tipoComprobante(TipoComprobante.fromCodigo(e.getTipoComprobante()))
                .serie(e.getSerie())
                .numero(e.getNumero())
                .fechaEmision(e.getFechaEmision())
                .moneda(e.getMoneda())
                .tipoOperacion(e.getTipoOperacion())
                .clienteTipoDoc(e.getClienteTipoDoc())
                .clienteNumeroDoc(e.getClienteNumeroDoc())
                .clienteNombre(e.getClienteNombre())
                .formaPago(e.getFormaPago())
                .totalOtrosCargos(e.getTotalOtrosCargos())
                .totalTributos(e.getTotalTributos())
                .total(e.getTotal())
                .estadoInterno(e.getEstadoInterno())
                .hashCpe(e.getHashCpe())
                .detalles(lineas)
                .totalesAfectacion(totalesAf)
                .cuotas(cuotas)
                .detraccion(det)
                .referencias(refs)
                .build();
    }
}
