package com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.service;

import com.s1nt4xSystem.facturacion_sunat.infrastructure.liquibase.TenantLiquibaseMigrator;
import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import com.s1nt4xSystem.facturacion_sunat.platform.account.service.CuentaSaasService;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaOnboardingRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaRouterResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaUpdateRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.model.EmpresaRouter;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.DomainException;
import com.s1nt4xSystem.facturacion_sunat.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class EmpresaOnboardingServiceImpl implements EmpresaOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(EmpresaOnboardingServiceImpl.class);

    private final EmpresaRouterRepository empresaRouterRepository;
    private final CuentaSaasService cuentaSaasService;
    private final TenantLiquibaseMigrator tenantLiquibaseMigrator;
    private final JdbcTemplate jdbcTemplate;
    private final String storageBasePath;

    public EmpresaOnboardingServiceImpl(
            EmpresaRouterRepository empresaRouterRepository,
            CuentaSaasService cuentaSaasService,
            TenantLiquibaseMigrator tenantLiquibaseMigrator,
            JdbcTemplate jdbcTemplate,
            @Value("${app.storage.base-path:storage}") String storageBasePath) {
        this.empresaRouterRepository = empresaRouterRepository;
        this.cuentaSaasService = cuentaSaasService;
        this.tenantLiquibaseMigrator = tenantLiquibaseMigrator;
        this.jdbcTemplate = jdbcTemplate;
        this.storageBasePath = storageBasePath;
    }

    @Override
    public EmpresaRouterResponse onboardEmpresa(EmpresaOnboardingRequest request) {
        log.info("Iniciando onboarding para empresa RUC: {}", request.getRuc());

        // 1. Validar que la cuenta SaaS exista
        CuentaSaas cuentaSaas = cuentaSaasService.getAccountEntity(request.getSaasId());

        // 2. Validar que el RUC no esté ya registrado en empresas_router
        if (empresaRouterRepository.existsByRuc(request.getRuc())) {
            throw new DomainException("El RUC " + request.getRuc() + " ya se encuentra registrado en el sistema");
        }

        // 3. Generar el nombre del esquema PostgreSQL aislado para este tenant
        String schemaName = "tenant_" + request.getRuc().trim();
        if (empresaRouterRepository.existsByDbSchema(schemaName)) {
            throw new DomainException("El esquema " + schemaName + " ya existe");
        }

        // 4. Guardar el registro de enrutamiento en public.empresas_router
        String empAccessKey = request.getAccessKey();
        if (empAccessKey != null && !empAccessKey.trim().isEmpty()) {
            empAccessKey = empAccessKey.trim();
            if (empresaRouterRepository.existsByAccessKey(empAccessKey)) {
                throw new DomainException("La accessKey ya está en uso por otra empresa");
            }
        } else {
            empAccessKey = "ak_emp_" + UUID.randomUUID().toString().replace("-", "");
        }

        EmpresaRouter router = EmpresaRouter.builder()
                .cuentaSaas(cuentaSaas)
                .ruc(request.getRuc().trim())
                .nombre(request.getRazonSocial().trim())
                .dbSchema(schemaName)
                .accessKey(empAccessKey)
                .estado("ACTIVO")
                .build();

        EmpresaRouter savedRouter = empresaRouterRepository.save(router);

        // 5. Ejecutar aprovisionamiento y migraciones de Liquibase para el nuevo esquema
        log.info("Aprovisionando esquema PostgreSQL: {}", schemaName);
        tenantLiquibaseMigrator.migrateTenant(schemaName);

        // 6. Sembrar el registro inicial de la empresa dentro de su propio esquema tenant
        try {
            String insertEmpresaSql = String.format(
                    "INSERT INTO %s.empresas (ruc, razon_social, direccion_fiscal, logo, entorno) " +
                    "VALUES (?, ?, ?, ?, 'BETA') ON CONFLICT (ruc) DO NOTHING",
                    schemaName
            );
            jdbcTemplate.update(
                    insertEmpresaSql,
                    request.getRuc().trim(),
                    request.getRazonSocial().trim(),
                    request.getDireccionFiscal(),
                    request.getLogo()
            );
            log.info("Empresa sembrada exitosamente en esquema {}.empresas con entorno BETA", schemaName);

            // 6.1. Sembrar configuración inicial mínima con valores por defecto (sin credenciales inventadas)
            String insertConfigSql = String.format(
                    "INSERT INTO %s.empresa_config (id, empresa_id, envio_asincrono, modo_emision) " +
                    "SELECT uuidv7(), e.id, true, 'PROPIO' " +
                    "FROM %s.empresas e WHERE NOT EXISTS (SELECT 1 FROM %s.empresa_config WHERE empresa_id = e.id)",
                    schemaName, schemaName, schemaName
            );
            jdbcTemplate.update(insertConfigSql);

            // 6.2. Sembrar Sucursal Principal '0000' por defecto
            String insertSucursalSql = String.format(
                    "INSERT INTO %s.sucursales (id, empresa_id, codigo, nombre_sucursal, ubigeo, direccion, impuesto_porcentaje, activo) " +
                    "SELECT uuidv7(), e.id, '0000', 'Casa Matriz Principal', '150101', COALESCE(e.direccion_fiscal, 'Dirección Principal'), 18.00, true " +
                    "FROM %s.empresas e WHERE NOT EXISTS (SELECT 1 FROM %s.sucursales WHERE codigo = '0000')",
                    schemaName, schemaName, schemaName
            );
            jdbcTemplate.update(insertSucursalSql);

            // 6.3. Sembrar Series oficiales completas para la Sucursal Principal (01, 03, 07, 08, 09, 00)
            String insertSeriesSql = String.format(
                    "INSERT INTO %s.series (id, sucursal_id, tipo_comprobante, serie, correlativo) " +
                    "SELECT uuidv7(), s.id, v.tipo, v.serie, 0 FROM %s.sucursales s " +
                    "CROSS JOIN (VALUES " +
                    "   ('01', 'F001'), " +
                    "   ('03', 'B001'), " +
                    "   ('07', 'FC01'), " +
                    "   ('07', 'BC01'), " +
                    "   ('08', 'FD01'), " +
                    "   ('08', 'BD01'), " +
                    "   ('09', 'T001'), " +
                    "   ('00', 'NV01') " +
                    ") AS v(tipo, serie) " +
                    "WHERE s.codigo = '0000' AND NOT EXISTS (SELECT 1 FROM %s.series s2 WHERE s2.sucursal_id = s.id AND s2.tipo_comprobante = v.tipo AND s2.serie = v.serie)",
                    schemaName, schemaName, schemaName
            );
            jdbcTemplate.update(insertSeriesSql);
            log.info("Sucursal principal '0000' y series iniciales (incluyendo NV01) sembradas para esquema {}", schemaName);

        } catch (Exception e) {
            log.error("Error al sembrar la empresa en el esquema {}: {}", schemaName, e.getMessage(), e);
            throw new DomainException("Se creó el esquema pero falló la inicialización de los datos de la empresa: " + e.getMessage());
        }

        // 7. Crear estructura de almacenamiento físico para el Tenant (certificados, XMLs, CDRs, PDFs)
        crearEstructuraStorageTenant(request.getRuc().trim());

        log.info("Onboarding completado exitosamente para RUC: {} en esquema: {}", request.getRuc(), schemaName);
        return EmpresaRouterResponse.fromEntity(savedRouter);
    }

    private void crearEstructuraStorageTenant(String ruc) {
        try {
            Path tenantPath = Paths.get(storageBasePath, "tenants", ruc);
            Files.createDirectories(tenantPath.resolve("certificates"));
            Files.createDirectories(tenantPath.resolve("xml"));
            Files.createDirectories(tenantPath.resolve("cdr"));
            Files.createDirectories(tenantPath.resolve("pdf"));
            log.info("Estructura de almacenamiento físico creada para tenant RUC: {} en {}", ruc, tenantPath.toAbsolutePath());
        } catch (IOException e) {
            log.warn("No se pudo crear la estructura física de storage para tenant {}: {}", ruc, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaRouterResponse getEmpresaRouterById(Long id) {
        return empresaRouterRepository.findById(id)
                .map(EmpresaRouterResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa Router", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpresaRouterResponse> getAllEmpresas() {
        return empresaRouterRepository.findAll().stream()
                .map(EmpresaRouterResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpresaRouterResponse> getEmpresasBySaasId(Long saasId) {
        return empresaRouterRepository.findByCuentaSaasId(saasId).stream()
                .map(EmpresaRouterResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public EmpresaRouterResponse updateEmpresa(Long id, EmpresaUpdateRequest request) {
        EmpresaRouter router = empresaRouterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa Router", id));

        String nuevoNombre = null;
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            nuevoNombre = request.getNombre().trim();
            router.setNombre(nuevoNombre);
        } else if (request.getRazonSocial() != null && !request.getRazonSocial().isBlank()) {
            nuevoNombre = request.getRazonSocial().trim();
            router.setNombre(nuevoNombre);
        }

        if (request.getEstado() != null && !request.getEstado().isBlank()) {
            String st = request.getEstado().trim().toUpperCase();
            if (!st.equals("ACTIVO") && !st.equals("INACTIVO") && !st.equals("SUSPENDIDO")) {
                throw new DomainException("Estado no permitido. Valores válidos: ACTIVO, INACTIVO, SUSPENDIDO");
            }
            router.setEstado(st);
        }

        EmpresaRouter saved = empresaRouterRepository.save(router);

        // Sincronizar también con el esquema del tenant si corresponde
        try {
            String updateEmpresaSql = String.format(
                    "UPDATE %s.empresas SET razon_social = COALESCE(?, razon_social), direccion_fiscal = COALESCE(?, direccion_fiscal), logo = COALESCE(?, logo) WHERE ruc = ?",
                    router.getDbSchema()
            );
            jdbcTemplate.update(
                    updateEmpresaSql,
                    nuevoNombre,
                    request.getDireccionFiscal(),
                    request.getLogo(),
                    router.getRuc()
            );
        } catch (Exception e) {
            log.warn("No se pudo sincronizar actualización en el esquema {}: {}", router.getDbSchema(), e.getMessage());
        }

        return EmpresaRouterResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public EmpresaRouterResponse updateEstado(Long id, String estado) {
        EmpresaRouter router = empresaRouterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa Router", id));

        router.setEstado(estado.toUpperCase().trim());
        return EmpresaRouterResponse.fromEntity(empresaRouterRepository.save(router));
    }

    @Override
    @Transactional
    public EmpresaRouterResponse regenerateAccessKey(Long id) {
        EmpresaRouter router = empresaRouterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa Router", id));

        String newKey = "ak_emp_" + UUID.randomUUID().toString().replace("-", "");
        router.setAccessKey(newKey);
        return EmpresaRouterResponse.fromEntity(empresaRouterRepository.save(router));
    }

    @Override
    @Transactional
    public void deleteEmpresa(Long id) {
        EmpresaRouter router = empresaRouterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa Router", id));

        router.setEstado("INACTIVO"); // Soft delete
        empresaRouterRepository.save(router);
    }
}
