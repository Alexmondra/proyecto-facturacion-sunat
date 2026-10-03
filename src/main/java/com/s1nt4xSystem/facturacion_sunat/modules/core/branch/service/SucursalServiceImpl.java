package com.s1nt4xSystem.facturacion_sunat.modules.core.branch.service;

import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.dto.SucursalRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.dto.SucursalResponse;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.dto.SucursalUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.service.EmpresaTenantService;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.model.CatalogoUbigeo;
import com.s1nt4xSystem.facturacion_sunat.platform.catalog.repository.CatalogoUbigeoRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.DomainException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ResourceNotFoundException;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.validation.BranchValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class SucursalServiceImpl implements SucursalService {

    private final SucursalRepository sucursalRepository;
    private final EmpresaTenantService empresaTenantService;
    private final CatalogoUbigeoRepository catalogoUbigeoRepository;
    private final SerieRepository serieRepository;
    private final BranchValidator branchValidator;

    @org.springframework.beans.factory.annotation.Autowired
    public SucursalServiceImpl(
            SucursalRepository sucursalRepository,
            EmpresaTenantService empresaTenantService,
            CatalogoUbigeoRepository catalogoUbigeoRepository,
            SerieRepository serieRepository,
            BranchValidator branchValidator) {
        this.sucursalRepository = sucursalRepository;
        this.empresaTenantService = empresaTenantService;
        this.catalogoUbigeoRepository = catalogoUbigeoRepository;
        this.serieRepository = serieRepository;
        this.branchValidator = branchValidator;
    }

    public SucursalServiceImpl(
            SucursalRepository sucursalRepository,
            EmpresaTenantService empresaTenantService,
            CatalogoUbigeoRepository catalogoUbigeoRepository,
            SerieRepository serieRepository) {
        this(sucursalRepository, empresaTenantService, catalogoUbigeoRepository, serieRepository,
                new BranchValidator(sucursalRepository, catalogoUbigeoRepository));
    }

    @Override
    public SucursalResponse createSucursal(SucursalRequest request) {
        Empresa empresa = empresaTenantService.getEmpresaEntity();
        branchValidator.validateNewBranch(request.getCodigo(), empresa.getId());

        CatalogoUbigeo ubigeoValido = branchValidator.validateUbigeo(request.getUbigeo());
        BigDecimal impuestoFinal = resolverImpuesto(request.getImpuestoPorcentaje(), ubigeoValido);

        Sucursal sucursal = Sucursal.builder()
                .empresa(empresa)
                .codigo(request.getCodigo())
                .nombreSucursal(request.getNombreSucursal())
                .ubigeo(request.getUbigeo() != null ? request.getUbigeo().trim() : null)
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .imagenSucursal(request.getImagenSucursal())
                .impuestoPorcentaje(impuestoFinal)
                .configuracionExtra(request.getConfiguracionExtra())
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        Sucursal saved = sucursalRepository.save(sucursal);

        // Auto-crear series correlativas para la nueva sucursal (siguiente a las existentes)
        autogenerarSeriesParaNuevaSucursal(saved);

        return SucursalResponse.fromEntity(saved, ubigeoValido);
    }

    private void autogenerarSeriesParaNuevaSucursal(Sucursal sucursal) {
        List<Serie> seriesExistentes = new ArrayList<>(serieRepository.findAll());
        List<String> tiposBase = List.of(
                TipoComprobante.FACTURA.getCodigo(),
                TipoComprobante.BOLETA.getCodigo(),
                TipoComprobante.NOTA_CREDITO.getCodigo(),
                TipoComprobante.NOTA_DEBITO.getCodigo(),
                TipoComprobante.TICKET_INTERNO.getCodigo()
        );

        for (String tipoDoc : tiposBase) {
            String siguienteSerie = calcularSiguienteSerie(tipoDoc, seriesExistentes);
            Serie nuevaSerie = Serie.builder()
                    .sucursal(sucursal)
                    .tipoComprobante(tipoDoc)
                    .serie(siguienteSerie)
                    .correlativo(0)
                    .build();
            Serie guardada = serieRepository.save(nuevaSerie);
            seriesExistentes.add(guardada != null ? guardada : nuevaSerie);
        }
    }

    private String calcularSiguienteSerie(String tipoDoc, List<Serie> existentes) {
        List<Serie> delTipo = existentes.stream()
                .filter(s -> s != null && tipoDoc.equals(s.getTipoComprobante()))
                .toList();

        TipoComprobante tipoEnum = TipoComprobante.fromCodigo(tipoDoc);
        String prefijoDefault = tipoEnum.getPrefijoSerieDefault();

        if (delTipo.isEmpty()) {
            int digitos = 4 - prefijoDefault.length();
            return prefijoDefault + String.format("%0" + digitos + "d", 1);
        }

        int maxNumero = 0;
        String mejorPrefijo = prefijoDefault;
        int digitos = 4 - prefijoDefault.length();

        for (Serie s : delTipo) {
            String codigoSerie = s.getSerie().trim().toUpperCase();
            int splitIdx = 0;
            while (splitIdx < codigoSerie.length() && Character.isLetter(codigoSerie.charAt(splitIdx))) {
                splitIdx++;
            }
            if (splitIdx > 0 && splitIdx < codigoSerie.length()) {
                String pref = codigoSerie.substring(0, splitIdx);
                String numStr = codigoSerie.substring(splitIdx);
                try {
                    int num = Integer.parseInt(numStr);
                    if (num > maxNumero) {
                        maxNumero = num;
                        mejorPrefijo = pref;
                        digitos = numStr.length();
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        int siguienteNumero = maxNumero + 1;
        String candidata = mejorPrefijo + String.format("%0" + digitos + "d", siguienteNumero);
        tipoEnum.validarSerie(candidata);
        return candidata;
    }

    @Override
    @Transactional(readOnly = true)
    public SucursalResponse getSucursalById(UUID id) {
        Sucursal sucursal = getSucursalEntity(id);
        CatalogoUbigeo ubigeo = resolverUbigeoEntity(sucursal.getUbigeo());
        return SucursalResponse.fromEntity(sucursal, ubigeo);
    }

    @Override
    @Transactional(readOnly = true)
    public Sucursal getSucursalEntity(UUID id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SucursalResponse> getAllSucursales() {
        Empresa empresa = empresaTenantService.getEmpresaEntity();
        List<Sucursal> sucursales = sucursalRepository.findByEmpresaId(empresa.getId());
        if (sucursales.isEmpty()) {
            sucursales = sucursalRepository.findAll();
        }
        List<String> codigosUbigeo = sucursales.stream()
                .map(Sucursal::getUbigeo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, CatalogoUbigeo> ubigeoMap = catalogoUbigeoRepository.findAll().stream()
                .filter(u -> codigosUbigeo.contains(u.getCodigo()))
                .collect(Collectors.toMap(CatalogoUbigeo::getCodigo, Function.identity(), (a, b) -> a));

        return sucursales.stream()
                .map(s -> SucursalResponse.fromEntity(s, ubigeoMap.get(s.getUbigeo())))
                .toList();
    }

    @Override
    public SucursalResponse updateSucursal(UUID id, SucursalUpdateRequest request) {
        Sucursal sucursal = getSucursalEntity(id);
        Empresa empresa = empresaTenantService.getEmpresaEntity();
        branchValidator.validateBranchOwnership(sucursal, empresa.getId());
        branchValidator.validateUpdateCode(sucursal, request.getCodigo(), empresa.getId());

        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            sucursal.setCodigo(request.getCodigo().trim());
        }

        if (request.getNombreSucursal() != null && !request.getNombreSucursal().isBlank()) {
            sucursal.setNombreSucursal(request.getNombreSucursal().trim());
        }

        CatalogoUbigeo ubigeoValido = null;
        if (request.getUbigeo() != null) {
            String ubigeoClean = request.getUbigeo().trim();
            if (!ubigeoClean.isEmpty()) {
                ubigeoValido = branchValidator.validateUbigeo(ubigeoClean);
                sucursal.setUbigeo(ubigeoClean);
            } else {
                sucursal.setUbigeo(null);
            }
            BigDecimal impuestoFinal = resolverImpuesto(request.getImpuestoPorcentaje(), ubigeoValido);
            sucursal.setImpuestoPorcentaje(impuestoFinal);
        } else if (request.getImpuestoPorcentaje() != null) {
            sucursal.setImpuestoPorcentaje(request.getImpuestoPorcentaje());
        }

        if (request.getDireccion() != null) {
            sucursal.setDireccion(request.getDireccion());
        }
        if (request.getTelefono() != null) {
            sucursal.setTelefono(request.getTelefono());
        }
        if (request.getEmail() != null) {
            sucursal.setEmail(request.getEmail());
        }
        if (request.getImagenSucursal() != null) {
            sucursal.setImagenSucursal(request.getImagenSucursal());
        }
        if (request.getConfiguracionExtra() != null) {
            sucursal.setConfiguracionExtra(request.getConfiguracionExtra());
        }
        if (request.getActivo() != null) {
            sucursal.setActivo(request.getActivo());
        }

        Sucursal saved = sucursalRepository.save(sucursal);
        if (ubigeoValido == null) {
            ubigeoValido = resolverUbigeoEntity(saved.getUbigeo());
        }
        return SucursalResponse.fromEntity(saved, ubigeoValido);
    }

    @Override
    public SucursalResponse updateSucursal(UUID id, SucursalRequest request) {
        SucursalUpdateRequest updateReq = SucursalUpdateRequest.builder()
                .codigo(request.getCodigo())
                .nombreSucursal(request.getNombreSucursal())
                .ubigeo(request.getUbigeo())
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .imagenSucursal(request.getImagenSucursal())
                .impuestoPorcentaje(request.getImpuestoPorcentaje())
                .configuracionExtra(request.getConfiguracionExtra())
                .activo(request.getActivo())
                .build();
        return updateSucursal(id, updateReq);
    }

    @Override
    public SucursalResponse updateStatus(UUID id, Boolean activo) {
        Sucursal sucursal = getSucursalEntity(id);
        branchValidator.validateStatusChange(sucursal, activo);
        sucursal.setActivo(activo);
        CatalogoUbigeo ubigeo = resolverUbigeoEntity(sucursal.getUbigeo());
        return SucursalResponse.fromEntity(sucursalRepository.save(sucursal), ubigeo);
    }

    @Override
    public void deleteSucursal(UUID id) {
        Sucursal sucursal = getSucursalEntity(id);
        branchValidator.validateCanDelete(sucursal);
        sucursal.setActivo(false); // Soft delete
        sucursalRepository.save(sucursal);
    }

    /**
     * Resuelve el impuesto_porcentaje:
     * - Si se especifica un porcentaje explícito en la solicitud, se respeta.
     * - Si viene nulo:
     *     - Si el ubigeo es de Amazonía (Ley 27037) -> 0.00%
     *     - Si no es Amazonía -> 18.00%
     */
    private BigDecimal resolverImpuesto(BigDecimal impuestoRequest, CatalogoUbigeo ubigeo) {
        if (impuestoRequest != null) {
            return impuestoRequest;
        }
        if (ubigeo != null && Boolean.TRUE.equals(ubigeo.getEsAmazonia())) {
            return BigDecimal.ZERO.setScale(2);
        }
        return new BigDecimal("18.00");
    }

    private CatalogoUbigeo resolverUbigeoEntity(String ubigeo) {
        if (ubigeo == null || ubigeo.trim().isEmpty()) {
            return null;
        }
        return catalogoUbigeoRepository.findByCodigo(ubigeo.trim()).orElse(null);
    }
}
