package com.s1nt4xSystem.facturacion_sunat.modules.billing.document.infrastructure.persistence.adapter;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.document.domain.port.out.CatalogoFiscalPort;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoDetraccion;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoTributoTasa;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository.CatalogoDetraccionRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository.CatalogoTributoTasaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogoFiscalAdapter implements CatalogoFiscalPort {

    private static final BigDecimal DEFAULT_TASA_IGV = new BigDecimal("18.00");
    private static final BigDecimal DEFAULT_TASA_ICBPER = new BigDecimal("0.50");

    // Departamentos que gozan de la Ley de Promoción de la Inversión en la Amazonía (Ley 27037)
    // 01: Amazonas, 16: Loreto, 17: Madre de Dios, 22: San Martín, 25: Ucayali
    private static final Set<String> PREFIJOS_UBIGEO_AMAZONIA = Set.of("01", "16", "17", "22", "25");

    private final CatalogoTributoTasaRepository tasaRepository;
    private final CatalogoDetraccionRepository detraccionRepository;

    private final ConcurrentHashMap<String, BigDecimal> cacheTasas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DetraccionInfo> cacheDetracciones = new ConcurrentHashMap<>();

    @Override
    public BigDecimal obtenerTasaIgvVigente() {
        return cacheTasas.computeIfAbsent("1000", k -> {
            try {
                Optional<CatalogoTributoTasa> tasaOpt = tasaRepository.findTasaVigente("1000", LocalDate.now());
                if (tasaOpt.isPresent() && tasaOpt.get().getValor() != null) {
                    return tasaOpt.get().getValor();
                }
            } catch (Exception e) {
                log.warn("No se pudo consultar tasa de IGV desde la BD, usando valor por defecto {}: {}", DEFAULT_TASA_IGV, e.getMessage());
            }
            return DEFAULT_TASA_IGV;
        });
    }

    @Override
    public BigDecimal obtenerTasaIcbperVigente() {
        return cacheTasas.computeIfAbsent("7152", k -> {
            try {
                Optional<CatalogoTributoTasa> tasaOpt = tasaRepository.findTasaVigente("7152", LocalDate.now());
                if (tasaOpt.isPresent() && tasaOpt.get().getValor() != null) {
                    return tasaOpt.get().getValor();
                }
            } catch (Exception e) {
                log.warn("No se pudo consultar tasa de ICBPER desde la BD, usando valor por defecto {}: {}", DEFAULT_TASA_ICBPER, e.getMessage());
            }
            return DEFAULT_TASA_ICBPER;
        });
    }

    @Override
    public BigDecimal determinarTasaIgvPorSucursal(Sucursal sucursal) {
        if (sucursal == null) {
            return obtenerTasaIgvVigente();
        }

        // 1. Si la sucursal tiene un impuesto_porcentaje explícito configurado
        if (sucursal.getImpuestoPorcentaje() != null) {
            BigDecimal tasaSucursal = sucursal.getImpuestoPorcentaje();
            // Si la tasa configurada es 0.00 (Amazonía o régimen especial), respetarla
            if (tasaSucursal.compareTo(BigDecimal.ZERO) == 0) {
                log.info("Sucursal '{}' opera con tasa especial de impuesto 0.00%", sucursal.getNombreSucursal());
                return BigDecimal.ZERO;
            }
            // Si tiene una tasa diferente al 18% (ej. 10%), respetarla
            if (tasaSucursal.compareTo(DEFAULT_TASA_IGV) != 0) {
                return tasaSucursal;
            }
        }

        // 2. Si el ubigeo corresponde a la región de Amazonía peruana (Ley 27037)
        if (esUbigeoAmazonia(sucursal.getUbigeo())) {
            log.info("Sucursal '{}' detectada en región Amazonía (Ubigeo {}), aplicando tasa IGV 0.00%",
                    sucursal.getNombreSucursal(), sucursal.getUbigeo());
            return BigDecimal.ZERO;
        }

        // 3. Tasa general vigente de base de datos
        return obtenerTasaIgvVigente();
    }

    @Override
    public Optional<DetraccionInfo> buscarDetraccion(String codigoBienServicio) {
        if (codigoBienServicio == null || codigoBienServicio.trim().isEmpty()) {
            return Optional.empty();
        }

        String codigo = codigoBienServicio.trim();
        if (cacheDetracciones.containsKey(codigo)) {
            return Optional.of(cacheDetracciones.get(codigo));
        }

        try {
            Optional<CatalogoDetraccion> detOpt = detraccionRepository.findByCodigoBienServicioAndEstadoTrue(codigo);
            if (detOpt.isPresent()) {
                CatalogoDetraccion det = detOpt.get();
                DetraccionInfo info = new DetraccionInfo(
                        det.getCodigoBienServicio(),
                        det.getDescripcion(),
                        det.getPorcentaje(),
                        det.getMontoMinimo()
                );
                cacheDetracciones.put(codigo, info);
                return Optional.of(info);
            }
        } catch (Exception e) {
            log.warn("Error al consultar detracción para código {}: {}", codigo, e.getMessage());
        }

        return Optional.empty();
    }

    private boolean esUbigeoAmazonia(String ubigeo) {
        if (ubigeo == null || ubigeo.trim().length() < 2) {
            return false;
        }
        String prefijo = ubigeo.trim().substring(0, 2);
        return PREFIJOS_UBIGEO_AMAZONIA.contains(prefijo);
    }
}
