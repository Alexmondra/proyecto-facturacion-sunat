-- liquibase formatted sql
-- ============================================================================
-- SEEDER INICIAL: PLANES, MÓDULO FACTURACIÓN, CUENTAS SAAS Y TENANT OFICIAL SUNAT
-- ============================================================================

-- changeset alex:005-seed-tenants-iniciales.sql
-- comment: Sembrar planes base, módulo de facturación, cuentas permanentes y tenant oficial SUNAT BETA (20000000001)

-- 1. PLANES BASE
INSERT INTO planes (nombre_plan, codigo, limite_mensual_bolsa, precio_mensual, estado)
VALUES 
    ('Plan Básico Emprendedor', 'BAS_01', 300, 29.00, true),
    ('Plan Pyme Profesional', 'PYME_01', 1000, 59.90, true),
    ('Plan Corporativo Multi-Empresa', 'CORP_01', 10000, 199.00, true)
ON CONFLICT (codigo) DO NOTHING;

-- 2. MÓDULO BASE DE FACTURACIÓN ELECTRÓNICA
INSERT INTO modulos (codigo, nombre, descripcion, estado)
VALUES (
    'FACTURACION',
    'Facturación Electrónica SUNAT',
    'Emisión de Facturas, Boletas, Notas de Crédito y Débito oficiales',
    true
)
ON CONFLICT (codigo) DO UPDATE 
SET nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    estado = EXCLUDED.estado;

-- 3. CUENTAS SAAS PERMANENTES
-- Cuenta 1: SuperAdmin FactuCorp Platform (Permanente)
INSERT INTO cuentas_saas (access_key, plan_id, nombre, consumido_mes, fecha_corte, estado, tipo)
SELECT 
    'ak_admin_factucorp_master_2026', 
    p.id, 
    'FactuCorp Platform Admin', 
    0, 
    31, 
    true, 
    'ADMIN'
FROM planes p
WHERE p.codigo = 'CORP_01'
ON CONFLICT (access_key) DO UPDATE SET tipo = 'ADMIN';

-- Cuenta 2: Cuenta Oficial de Testing SUNAT (Permanente)
INSERT INTO cuentas_saas (access_key, plan_id, nombre, consumido_mes, fecha_corte, estado, tipo)
SELECT 
    'ak_sunat_testing_saas_2026', 
    p.id, 
    'Entorno Oficial Testing SUNAT', 
    0, 
    30, 
    true, 
    'CLIENTE'
FROM planes p
WHERE p.codigo = 'CORP_01'
ON CONFLICT (access_key) DO UPDATE SET tipo = 'CLIENTE';

-- 4. EMPRESA ROUTER: TENANT OFICIAL SUNAT BETA (20000000001)
INSERT INTO empresas_router (saas_id, ruc, nombre, db_schema, estado, access_key)
SELECT 
    c.id,
    '20000000001',
    'EMPRESA DE PRUEBA SUNAT S.A.',
    'tenant_20000000001',
    'ACTIVO',
    'ak_test_sunat_20000000001'
FROM cuentas_saas c
WHERE c.access_key = 'ak_sunat_testing_saas_2026'
ON CONFLICT (ruc) DO UPDATE SET access_key = 'ak_test_sunat_20000000001', estado = 'ACTIVO';

-- 5. CREAR ESQUEMA POSTGRESQL PARA EL TENANT OFICIAL
CREATE SCHEMA IF NOT EXISTS tenant_20000000001;

-- rollback DELETE FROM empresas_router WHERE ruc = '20000000001';
-- rollback DROP SCHEMA IF EXISTS tenant_20000000001 CASCADE;
