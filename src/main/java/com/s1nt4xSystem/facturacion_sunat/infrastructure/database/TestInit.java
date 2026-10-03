package com.s1nt4xSystem.facturacion_sunat.infrastructure.database;

import com.s1nt4xSystem.facturacion_sunat.platform.account.model.CuentaSaas;
import com.s1nt4xSystem.facturacion_sunat.platform.account.repository.CuentaSaasRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaOnboardingRequest;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.dto.EmpresaRouterResponse;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.repository.EmpresaRouterRepository;
import com.s1nt4xSystem.facturacion_sunat.platform.tenantregistry.service.EmpresaOnboardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
@Order(15)
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true", matchIfMissing = false)
public class TestInit implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestInit.class);

    public static final String RUC_TEST_SUNAT = "20000000001";
    public static final String SCHEMA_TEST_SUNAT = "tenant_20000000001";

    private final EmpresaOnboardingService onboardingService;
    private final CuentaSaasRepository cuentaSaasRepository;
    private final EmpresaRouterRepository empresaRouterRepository;
    private final JdbcTemplate jdbcTemplate;

    public TestInit(
            EmpresaOnboardingService onboardingService,
            CuentaSaasRepository cuentaSaasRepository,
            EmpresaRouterRepository empresaRouterRepository,
            JdbcTemplate jdbcTemplate) {
        this.onboardingService = onboardingService;
        this.cuentaSaasRepository = cuentaSaasRepository;
        this.empresaRouterRepository = empresaRouterRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("==================================================================");
        log.info(" [TEST-INIT] Ejecutando flujo de Onboarding para RUC: {}", RUC_TEST_SUNAT);
        log.info("==================================================================");

        try {
            // 1. Resolver cuenta SaaS para la empresa de pruebas
            CuentaSaas cuentaSaas = cuentaSaasRepository.findByAccessKey("ak_sunat_testing_saas_2026")
                    .orElseGet(() -> cuentaSaasRepository.findAll().stream()
                            .filter(c -> !"ADMIN".equalsIgnoreCase(c.getTipo()))
                            .findFirst()
                            .orElse(cuentaSaasRepository.findAll().get(0)));

            // 2. Si ya existe en empresas_router pero el esquema no fue aprovisionado por onboarding,
            // permitimos resetear el router para ejecutar el onboarding auténtico
            if (empresaRouterRepository.existsByRuc(RUC_TEST_SUNAT)) {
                try {
                    Integer countEmpresas = jdbcTemplate.queryForObject(
                            String.format("SELECT COUNT(*) FROM %s.empresas WHERE ruc = ?", SCHEMA_TEST_SUNAT),
                            Integer.class,
                            RUC_TEST_SUNAT
                    );
                    if (countEmpresas == null || countEmpresas == 0) {
                        log.info("[TEST-INIT] Registro previo en router sin datos en tenant, limpiando para Onboarding completo...");
                        empresaRouterRepository.findByRuc(RUC_TEST_SUNAT)
                                .ifPresent(empresaRouterRepository::delete);
                    }
                } catch (Exception e) {
                    log.info("[TEST-INIT] Esquema no inicializado aún, procediendo con Onboarding...");
                    empresaRouterRepository.findByRuc(RUC_TEST_SUNAT)
                            .ifPresent(empresaRouterRepository::delete);
                }
            }

            // 3. Ejecutar Onboarding si no está registrado
            if (!empresaRouterRepository.existsByRuc(RUC_TEST_SUNAT)) {
                EmpresaOnboardingRequest request = EmpresaOnboardingRequest.builder()
                        .saasId(cuentaSaas.getId())
                        .ruc(RUC_TEST_SUNAT)
                        .razonSocial("DISTRIBUIDORA COMERCIAL NORTE S.A.C.")
                        .direccionFiscal("AV. INDUSTRIAL 789, CALLAO")
                        .logo("")
                        .accessKey("ak_test_sunat_20000000001")
                        .build();

                log.info("[TEST-INIT] Enviando petición de Onboarding: RUC={}, SaasId={}", request.getRuc(), request.getSaasId());
                EmpresaRouterResponse response = onboardingService.onboardEmpresa(request);
                log.info("[TEST-INIT] Onboarding completado con éxito: Schema={}, Ruc={}", response.getDbSchema(), response.getRuc());
            } else {
                log.info("[TEST-INIT] Empresa {} ya se encuentra totalmente aprovisionada.", RUC_TEST_SUNAT);
            }

            // 4. Configurar credenciales oficiales SOL (MODDATOS) para entorno de pruebas SUNAT BETA
            configurarCredencialesTestingBeta();

        } catch (Exception e) {
            log.error("[TEST-INIT] Error durante la inicialización vía Onboarding: {}", e.getMessage(), e);
        }
    }

    private void configurarCredencialesTestingBeta() {
        try {
            jdbcTemplate.update(String.format(
                    "UPDATE %s.empresa_config SET user_sol = 'MODDATOS', pass_sol = 'moddatos' " +
                    "WHERE user_sol IS NULL OR user_sol = '' OR user_sol = 'MODDATOS'",
                    SCHEMA_TEST_SUNAT
            ));
            log.info("[TEST-INIT] Credenciales SOL de prueba (MODDATOS) configuradas para {}", SCHEMA_TEST_SUNAT);
        } catch (Exception e) {
            log.warn("[TEST-INIT] No se pudo actualizar credenciales SOL de prueba: {}", e.getMessage());
        }
    }
}
