-- liquibase formatted sql

-- ============================================================================
-- SEEDER TENANT: DATOS EMPRESA, CONFIGURACION FISCAL SUNAT, SUCURSAL Y SERIES
-- ============================================================================

-- changeset alex:tenant-002-seed-empresa-config-inicial
-- comment: Sembrar configuración fiscal inicial (MODDATOS), sucursal matriz y series oficiales

-- 1. Si el esquema es tenant_20000000001 (Tenant Oficial SUNAT BETA), sembrar empresa si no existe
INSERT INTO empresas (id, ruc, razon_social, direccion_fiscal, entorno, incluido_tributo)
SELECT 
    uuidv7(),
    '20000000001',
    'EMPRESA DE PRUEBA SUNAT S.A.',
    'AV. GARCILASO DE LA VEGA 1456, LIMA',
    'BETA',
    true
WHERE current_schema() = 'tenant_20000000001'
  AND NOT EXISTS (SELECT 1 FROM empresas WHERE ruc = '20000000001');

-- 2. Sembrar Configuración Fiscal (Credenciales SOL de prueba SUNAT)
INSERT INTO empresa_config (
    id, 
    empresa_id, 
    envio_asincrono, 
    modo_emision, 
    user_sol, 
    pass_sol, 
    numero_cuenta_detraccion,
    tipo_certificado,
    certificado,
    certificado_pass
)
SELECT 
    uuidv7(),
    e.id,
    true,
    'PROPIO',
    'MODDATOS',
    'moddatos',
    '00-000-000000',
    'PFX',
    '',
    ''
FROM empresas e
WHERE NOT EXISTS (SELECT 1 FROM empresa_config WHERE empresa_id = e.id);

-- 3. Sembrar Sucursal Matriz (0000)
INSERT INTO sucursales (id, empresa_id, codigo, nombre_sucursal, ubigeo, direccion, impuesto_porcentaje, activo)
SELECT 
    uuidv7(),
    e.id,
    '0000',
    'Casa Matriz Principal',
    '150101',
    COALESCE(e.direccion_fiscal, 'Av. Principal 123, Lima'),
    18.00,
    true
FROM empresas e
WHERE NOT EXISTS (SELECT 1 FROM sucursales WHERE codigo = '0000');

-- 4. Sembrar Sucursal Amazonía (0001) para el tenant de pruebas
INSERT INTO sucursales (id, empresa_id, codigo, nombre_sucursal, ubigeo, direccion, impuesto_porcentaje, activo)
SELECT 
    uuidv7(),
    e.id,
    '0001',
    'Sucursal Iquitos (Amazonía Exonerada)',
    '160101',
    'Jr. Próspero 450, Iquitos, Maynas, Loreto',
    0.00,
    true
FROM empresas e
WHERE current_schema() = 'tenant_20000000001'
  AND NOT EXISTS (SELECT 1 FROM sucursales WHERE codigo = '0001');

-- 5. Sembrar Series oficiales iniciales para la Casa Matriz (0000)
INSERT INTO series (id, sucursal_id, tipo_comprobante, serie, correlativo)
SELECT uuidv7(), s.id, v.tipo, v.serie, 0
FROM sucursales s
CROSS JOIN (VALUES
    ('01', 'F001'),
    ('03', 'B001'),
    ('07', 'FC01'),
    ('08', 'FD01'),
    ('07', 'BC01'),
    ('08', 'BD01'),
    ('09', 'T001')
) AS v(tipo, serie)
WHERE s.codigo = '0000'
  AND NOT EXISTS (
      SELECT 1 FROM series s2
      WHERE s2.sucursal_id = s.id
        AND s2.tipo_comprobante = v.tipo
        AND s2.serie = v.serie
  );

-- 6. Sembrar Series para Sucursal Amazonía (F002, B002, FC02)
INSERT INTO series (id, sucursal_id, tipo_comprobante, serie, correlativo)
SELECT uuidv7(), s.id, v.tipo, v.serie, 0
FROM sucursales s
CROSS JOIN (VALUES
    ('01', 'F002'),
    ('03', 'B002'),
    ('07', 'FC02')
) AS v(tipo, serie)
WHERE s.codigo = '0001'
  AND NOT EXISTS (
      SELECT 1 FROM series s2
      WHERE s2.sucursal_id = s.id
        AND s2.tipo_comprobante = v.tipo
        AND s2.serie = v.serie
  );

-- rollback DELETE FROM series;
-- rollback DELETE FROM sucursales;
-- rollback DELETE FROM empresa_config;
-- rollback DELETE FROM empresas;
