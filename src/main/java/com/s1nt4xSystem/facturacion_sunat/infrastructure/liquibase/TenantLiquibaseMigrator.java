package com.s1nt4xSystem.facturacion_sunat.infrastructure.liquibase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import liquibase.integration.spring.SpringLiquibase;

import javax.sql.DataSource;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class TenantLiquibaseMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantLiquibaseMigrator.class);
    private static final Pattern SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    public TenantLiquibaseMigrator(DataSource dataSource, JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting programmatic Liquibase migrations for tenant schemas...");
        try {
            List<String> schemas = jdbcTemplate.queryForList(
                    "SELECT db_schema FROM public.empresas_router", String.class);
            
            for (String schema : schemas) {
                if (schema != null && !schema.trim().isEmpty() && !"public".equalsIgnoreCase(schema)) {
                    migrateTenant(schema);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query tenant schemas from empresas_router. Tenant migrations skipped.", e);
        }
    }

    public void migrateTenant(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            log.warn("Skipped migration: schema name is empty");
            return;
        }

        String cleanedSchema = schemaName.trim();
        if (!SCHEMA_PATTERN.matcher(cleanedSchema).matches()) {
            log.error("Skipped migration: invalid schema name format '{}'", cleanedSchema);
            return;
        }

        log.info("Preparing database schema and running migrations for: {}", cleanedSchema);
        try {
            // Ensure the schema exists on an independent autocommit connection so it commits immediately
            try (java.sql.Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(true);
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE SCHEMA IF NOT EXISTS " + cleanedSchema);
                }
            }

            // Run Liquibase migration
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog("classpath:db/changelog/db.changelog-tenant.xml");
            liquibase.setDefaultSchema(cleanedSchema);
            liquibase.setLiquibaseSchema(cleanedSchema);
            liquibase.setResourceLoader(resourceLoader);
            liquibase.afterPropertiesSet();
            log.info("Successfully executed migrations for schema: {}", cleanedSchema);
        } catch (Exception e) {
            log.error("Failed to run migrations for schema: {}", cleanedSchema, e);
            throw new RuntimeException("Error ejecutando migraciones para el esquema: " + cleanedSchema, e);
        }
    }
}
