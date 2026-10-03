package com.s1nt4xSystem.facturacion_sunat.modules.billing.application.validation;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.dto.ComprobanteItemCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.application.dto.EmitirComprobanteCommand;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoAfectacionIgv;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.TipoComprobante;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.ClienteDto;
import com.s1nt4xSystem.facturacion_sunat.modules.billing.infrastructure.rest.dto.ItemComprobanteRequest;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.model.Sucursal;
import com.s1nt4xSystem.facturacion_sunat.modules.core.branch.repository.SucursalRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.Empresa;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.model.EmpresaConfig;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaConfigRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.repository.EmpresaRepository;
import com.s1nt4xSystem.facturacion_sunat.modules.core.company.validation.CompanyValidator;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.model.Serie;
import com.s1nt4xSystem.facturacion_sunat.modules.core.series.repository.SerieRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.BusinessException;
import com.s1nt4xSystem.facturacion_sunat.shared.errors.ErrorCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Component
public class DocumentValidator {

    private final EmpresaRepository empresaRepository;
    private final EmpresaConfigRepository empresaConfigRepository;
    private final SucursalRepository sucursalRepository;
    private final SerieRepository serieRepository;
    private final CompanyValidator companyValidator;

    public DocumentValidator(
            EmpresaRepository empresaRepository,
            EmpresaConfigRepository empresaConfigRepository,
            SucursalRepository sucursalRepository,
            SerieRepository serieRepository,
            CompanyValidator companyValidator) {
        this.empresaRepository = empresaRepository;
        this.empresaConfigRepository = empresaConfigRepository;
        this.sucursalRepository = sucursalRepository;
        this.serieRepository = serieRepository;
        this.companyValidator = companyValidator;
    }

    /**
     * Valida y obtiene la empresa configurada en el tenant actual.
     */
    public Empresa validateAndGetEmpresaTenant() {
        return empresaRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND, "tenant"));
    }

    /**
     * Valida requisitos fiscales de la empresa (credenciales SOL, certificado activo y vigente).
     */
    public void validateRequisitosFiscales(Empresa empresa, TipoComprobante tipoComprobante) {
        if (!tipoComprobante.isEsElectronico()) {
            return;
        }

        if (empresa.getRuc() == null || empresa.getRuc().isBlank()
                || empresa.getRazonSocial() == null || empresa.getRazonSocial().isBlank()) {
            throw new BusinessException(ErrorCode.COMPANY_SOL_CREDENTIALS_MISSING);
        }

        Optional<EmpresaConfig> configOpt = empresaConfigRepository.findByEmpresaId(empresa.getId());
        companyValidator.validateForEmission(empresa, configOpt.orElse(null), tipoComprobante.isEsElectronico());
    }

    /**
     * Valida que el RUC emisor enviado coincida con el RUC de la empresa del tenant.
     */
    public void validateEmisorRucCoincide(String emisorRuc, Empresa empresa) {
        if (emisorRuc != null && !emisorRuc.isBlank()) {
            if (!empresa.getRuc().equalsIgnoreCase(emisorRuc.trim())) {
                throw new BusinessException(ErrorCode.DOCUMENT_EMISOR_RUC_MISMATCH, emisorRuc, empresa.getRuc());
            }
        }
    }

    /**
     * Valida el formato y sintaxis de la serie según el tipo de comprobante.
     */
    public void validateSerieFormato(TipoComprobante tipo, String serie) {
        if (serie == null || serie.isBlank()) {
            throw new BusinessException(ErrorCode.DOCUMENT_SERIE_REQUIRED);
        }
        String serieLimpia = serie.trim().toUpperCase();
        if (!tipo.getPattern().matcher(serieLimpia).matches()) {
            throw new BusinessException(ErrorCode.DOCUMENT_SERIE_INVALID_FORMAT,
                    serieLimpia, tipo.getDescripcion(), tipo.getPatronSerieRegex());
        }
    }

    /**
     * Valida y resuelve la serie configurada para emisión de comprobante general.
     */
    public Serie validateAndResolveSerie(EmitirComprobanteCommand command) {
        if (command.getSucursalId() != null) {
            sucursalRepository.findById(command.getSucursalId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_SUCURSAL_NOT_FOUND));

            return serieRepository.findBySucursalIdAndTipoComprobanteAndSerie(
                    command.getSucursalId(), command.getTipoComprobante(), command.getSerie()
            ).orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_SERIE_NOT_CONFIGURED_SUCURSAL,
                    command.getSerie(), command.getTipoComprobante()));
        } else {
            return serieRepository.findByTipoComprobanteAndSerie(
                    command.getTipoComprobante(), command.getSerie()
            ).orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_SERIE_NOT_CONFIGURED_EMPRESA,
                    command.getSerie(), command.getTipoComprobante()));
        }
    }

    /**
     * Valida y resuelve la serie para comprobantes OpenAPI (con o sin serie explícita).
     */
    public Serie validateAndResolveSerieRest(String tipoComprobante, String serie, String codSucursalEmisor) {
        if (serie != null && !serie.isBlank()) {
            return serieRepository.findByTipoComprobanteAndSerie(tipoComprobante, serie.trim())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_SERIE_NOT_CONFIGURED_EMPRESA,
                            serie, tipoComprobante));
        }

        String codSucursal = (codSucursalEmisor != null && !codSucursalEmisor.isBlank())
                ? codSucursalEmisor.trim() : "0000";

        Sucursal sucursal = sucursalRepository.findByCodigo(codSucursal)
                .orElseGet(() -> sucursalRepository.findByActivoTrue().stream().findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_SUCURSAL_ACTIVE_NOT_FOUND, codSucursal)));

        return serieRepository.findFirstBySucursalIdAndTipoComprobante(sucursal.getId(), tipoComprobante)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_SERIE_NOT_FOUND_FOR_TYPE, tipoComprobante));
    }

    /**
     * Valida y resuelve la serie para notas de crédito / débito.
     */
    public Serie validateAndResolveSerieNota(String tipoNota, String serie, String codSucursalEmisor) {
        return validateAndResolveSerieRest(tipoNota, serie, codSucursalEmisor);
    }

    /**
     * Valida que el bloque cliente esté presente (opcional para boletas sin identificación).
     */
    public void validateClientePresent(ClienteDto cliente, String tipoComprobante) {
        if (cliente == null && !"03".equals(tipoComprobante)) {
            throw new BusinessException(ErrorCode.DOCUMENT_CLIENT_INVALID);
        }
    }

    /**
     * Valida que el bloque cliente esté presente.
     */
    public void validateClientePresent(ClienteDto cliente) {
        validateClientePresent(cliente, null);
    }

    /**
     * Valida que la lista de ítems no sea nula ni vacía.
     */
    public void validateItemsNotEmpty(List<?> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_ITEMS_EMPTY);
        }
    }

    /**
     * Valida que el valor de un ítem no sea nulo.
     */
    public void validateItemValor(BigDecimal valor, String descripcion) {
        if (valor == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_ITEM_PRICE_REQUIRED, descripcion);
        }
    }

    /**
     * Valida que el ítem tenga un precio o valor unitario válido.
     */
    public void validateItemPrecios(ComprobanteItemCommand itemCmd, BigDecimal valorUnitario, BigDecimal precioUnitario) {
        if (valorUnitario == null && precioUnitario == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_ITEM_PRICE_REQUIRED, itemCmd.getDescripcion());
        }
    }

    /**
     * Valida coherencia del precio de bolsas plásticas con respecto al ICBPER cuando la empresa tiene tributos incluidos.
     */
    public void validatePrecioBolsaIcbper(TipoAfectacionIgv afectacion, BigDecimal precioTotal, BigDecimal tasaIcbper) {
        if (afectacion.isGravaIgv() && precioTotal.compareTo(tasaIcbper) <= 0) {
            BigDecimal precioMostrar = precioTotal.setScale(2, RoundingMode.HALF_UP);
            BigDecimal tasaMostrar = tasaIcbper.setScale(2, RoundingMode.HALF_UP);
            throw new BusinessException(ErrorCode.DOCUMENT_BOLSA_PRICE_BELOW_ICBPER,
                    afectacion.getCodigo(), precioMostrar, tasaMostrar);
        }
    }

    /**
     * Valida tipo de nota permitido (07 o 08).
     */
    public void validateTipoNota(String tipoNota) {
        if (!"07".equals(tipoNota) && !"08".equals(tipoNota)) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOTA_TYPE_INVALID);
        }
    }
}
