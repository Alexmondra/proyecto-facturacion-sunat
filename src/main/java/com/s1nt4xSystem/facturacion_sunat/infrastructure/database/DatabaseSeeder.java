package com.s1nt4xSystem.facturacion_sunat.infrastructure.database;

import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.account.dto.CuentaSaasResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.account.repository.CuentaSaasRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.account.service.CuentaSaasService;
import com.s1nt4xSystem.facturacion_sunat.platform.module.model.Modulo;
import com.s1nt4xSystem.facturacion_sunat.platform.module.repository.ModuloRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.dto.PlanResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.repository.PlanRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.plan.service.PlanService;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaOnboardingRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.service.EmpresaOnboardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(10)
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true", matchIfMissing = false)
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final PlanService planService;
    private final PlanRepository planRepository;
    private final CuentaSaasService cuentaSaasService;
    private final CuentaSaasRepository cuentaSaasRepository;
    private final EmpresaOnboardingService onboardingService;
    private final EmpresaRouterRepository empresaRouterRepository;
    private final ModuloRepository moduloRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final String storageBasePath;

    public DatabaseSeeder(
            PlanService planService,
            PlanRepository planRepository,
            CuentaSaasService cuentaSaasService,
            CuentaSaasRepository cuentaSaasRepository,
            EmpresaOnboardingService onboardingService,
            EmpresaRouterRepository empresaRouterRepository,
            ModuloRepository moduloRepository,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            @org.springframework.beans.factory.annotation.Value("${app.storage.base-path:storage}") String storageBasePath) {
        this.planService = planService;
        this.planRepository = planRepository;
        this.cuentaSaasService = cuentaSaasService;
        this.cuentaSaasRepository = cuentaSaasRepository;
        this.onboardingService = onboardingService;
        this.empresaRouterRepository = empresaRouterRepository;
        this.moduloRepository = moduloRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.storageBasePath = storageBasePath;
    }

    @Override
    public void run(String... args) {
        log.info("==================================================================");
        log.info(" [SEEDER] Iniciando inicialización de datos de prueba (DEV)...");
        log.info("==================================================================");

        try {
            seedPlanes();
            seedModulos();
            seedCuentasYEmpresas();
            asegurarCarpetasStorageTenants();
            log.info("==================================================================");
            log.info(" [SEEDER] Datos de prueba inicializados exitosamente.");
            log.info("==================================================================");
        } catch (Exception e) {
            log.error(" [SEEDER] Error durante la ejecución del seeder: {}", e.getMessage(), e);
        }
    }

    private void asegurarCarpetasStorageTenants() {
        try {
            empresaRouterRepository.findAll().forEach(empresa -> {
                String ruc = empresa.getRuc();
                java.nio.file.Path tenantPath = java.nio.file.Paths.get(storageBasePath, "tenants", ruc);
                try {
                    java.nio.file.Files.createDirectories(tenantPath.resolve("certificates"));
                    java.nio.file.Files.createDirectories(tenantPath.resolve("xml"));
                    java.nio.file.Files.createDirectories(tenantPath.resolve("cdr"));
                    java.nio.file.Files.createDirectories(tenantPath.resolve("pdf"));
                    log.info("[SEEDER] Carpetas de almacenamiento verificadas para tenant RUC {}: {}", ruc, tenantPath);
                } catch (java.io.IOException e) {
                    log.warn("[SEEDER] No se pudieron crear carpetas de storage para RUC {}: {}", ruc, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("[SEEDER] Error al verificar directorios de storage para tenants: {}", e.getMessage());
        }
    }

    private void seedPlanes() {
        if (planRepository.count() == 0) {
            log.info("[SEEDER] Sembrando planes comerciales...");
            planService.createPlan(PlanRequest.builder()
                    .nombrePlan("Plan Básico Emprendedor")
                    .codigo("BAS_01")
                    .limiteMensualBolsa(300)
                    .precioMensual(new BigDecimal("29.00"))
                    .estado(true)
                    .build());

            planService.createPlan(PlanRequest.builder()
                    .nombrePlan("Plan Pyme Profesional")
                    .codigo("PYME_01")
                    .limiteMensualBolsa(1000)
                    .precioMensual(new BigDecimal("59.90"))
                    .estado(true)
                    .build());

            planService.createPlan(PlanRequest.builder()
                    .nombrePlan("Plan Corporativo Multi-Empresa")
                    .codigo("CORP_01")
                    .limiteMensualBolsa(10000)
                    .precioMensual(new BigDecimal("199.00"))
                    .estado(true)
                    .build());
            log.info("[SEEDER] 3 Planes creados exitosamente.");
        }
    }

    private void seedModulos() {
        if (moduloRepository.count() == 0) {
            log.info("[SEEDER] Sembrando módulo de facturación electrónica...");
            moduloRepository.save(Modulo.builder()
                    .codigo("FACTURACION")
                    .nombre("Facturación Electrónica SUNAT")
                    .descripcion("Emisión de Facturas, Boletas, Notas de Crédito y Débito")
                    .estado(true)
                    .build());
            log.info("[SEEDER] Módulo FACTURACION registrado exitosamente.");
        }
    }

    private void seedCuentasYEmpresas() {
        if (cuentaSaasRepository.count() <= 2) {
            log.info("[SEEDER] Verificando/Sembrando Cuentas SaaS y Tenants Demo de Desarrollo...");

            PlanResponse planPyme = planService.getAllPlans().stream()
                    .filter(p -> "PYME_01".equalsIgnoreCase(p.getCodigo()))
                    .findFirst()
                    .orElse(planService.getAllPlans().get(0));

            PlanResponse planCorp = planService.getAllPlans().stream()
                    .filter(p -> "CORP_01".equalsIgnoreCase(p.getCodigo()))
                    .findFirst()
                    .orElse(planPyme);

            // ================================================================
            // USUARIO / CUENTA SAAS 1: COMERCIAL LIMA (1 Tenant)
            // ================================================================
            if (!cuentaSaasRepository.existsByAccessKey("ak_cliente_lima_saas_2026")) {
                CuentaSaasResponse cuentaLima = cuentaSaasService.createAccount(CuentaSaasRequest.builder()
                        .nombre("Comercial Lima Distribuciones S.A.C.")
                        .planId(planCorp.getId())
                        .accessKey("ak_cliente_lima_saas_2026")
                        .tipo("CLIENTE")
                        .fechaCorte(30)
                        .estado(true)
                        .build());

                onboardEmpresaIfNotExists(EmpresaOnboardingRequest.builder()
                        .saasId(cuentaLima.getId())
                        .ruc("20609998877")
                        .razonSocial("DISTRIBUIDORA COMERCIAL NORTE S.A.C.")
                        .direccionFiscal("AV. ARGENTINA 4567, CALLAO")
                        .accessKey("ak_emp_retail_supermercado")
                        .build());
            }

            // ================================================================
            // USUARIO / CUENTA SAAS 2: GASTRONOMÍA SUR (1 Tenant)
            // ================================================================
            if (!cuentaSaasRepository.existsByAccessKey("ak_cliente_sur_saas_2026")) {
                CuentaSaasResponse cuentaSur = cuentaSaasService.createAccount(CuentaSaasRequest.builder()
                        .nombre("Corporación Gastronómica del Sur S.A.C.")
                        .planId(planPyme.getId())
                        .accessKey("ak_cliente_sur_saas_2026")
                        .tipo("CLIENTE")
                        .fechaCorte(28)
                        .estado(true)
                        .build());

                onboardEmpresaIfNotExists(EmpresaOnboardingRequest.builder()
                        .saasId(cuentaSur.getId())
                        .ruc("20701112233")
                        .razonSocial("SABORES Y TRADICIONES PERUANAS S.A.C.")
                        .direccionFiscal("CALLE MERCADERES 310, AREQUIPA")
                        .accessKey("ak_emp_gastro_sur")
                        .build());
            }

            log.info("[SEEDER] 2 Usuarios/Cuentas con 1 Tenant cada uno aprovisionados con sucursal principal y series completas.");
        }
    }

    private void onboardEmpresaIfNotExists(EmpresaOnboardingRequest request) {
        if (!empresaRouterRepository.existsByRuc(request.getRuc())) {
            log.info("[SEEDER] Aprovisionando empresa RUC: {} ({})", request.getRuc(), request.getRazonSocial());
            onboardingService.onboardEmpresa(request);
        }
    }
}
