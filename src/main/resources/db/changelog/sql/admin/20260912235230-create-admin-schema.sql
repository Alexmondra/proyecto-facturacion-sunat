-- liquibase formatted sql

-- ============================================================================
-- ESQUEMA ADMINISTRATIVO GENERAL (ADMIN / EMPRESAS / SAAS)
-- ============================================================================

-- changeset alex:001-initial-admin-schema.sql
-- validCheckSum: 9:eb6c6ea9ed9655033757ace2645bfeed
-- validCheckSum: ANY

-- 1. TABLA PLANES
CREATE TABLE planes (
    id SERIAL PRIMARY KEY,
    nombre_plan VARCHAR(50) NOT NULL,
    codigo VARCHAR(10) UNIQUE,
    limite_mensual_bolsa INTEGER,
    precio_mensual DECIMAL(10,2),
    estado BOOLEAN DEFAULT TRUE
);

-- 2. TABLA CUENTAS SAAS
CREATE TABLE cuentas_saas (
    id BIGSERIAL PRIMARY KEY,
    access_key VARCHAR(100) NOT NULL UNIQUE,
    plan_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    consumido_mes INT DEFAULT 0,
    fecha_corte INT,
    tipo VARCHAR(20) NOT NULL DEFAULT 'CLIENTE',
    estado BOOLEAN DEFAULT TRUE
);

ALTER TABLE cuentas_saas
ADD CONSTRAINT fk_cuenta_plan
FOREIGN KEY (plan_id) REFERENCES planes(id);

CREATE INDEX idx_cuentas_saas_plan_id ON cuentas_saas(plan_id);

-- 3. TABLA EMPRESAS ROUTER
CREATE TABLE empresas_router (
    id BIGSERIAL PRIMARY KEY,
    saas_id BIGINT NOT NULL,
    access_key VARCHAR(100) UNIQUE,
    ruc VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    db_schema VARCHAR(100) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL
);

ALTER TABLE empresas_router
ADD CONSTRAINT fk_router_saas
FOREIGN KEY (saas_id) REFERENCES cuentas_saas(id) ON DELETE CASCADE;

CREATE INDEX idx_empresas_router_saas_id ON empresas_router(saas_id);

-- 4. TABLA MODULOS
CREATE TABLE modulos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255),
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

-- 5. TABLA EMPRESA_MODULOS
CREATE TABLE empresa_modulos (
    id BIGSERIAL PRIMARY KEY,
    empresa_router_id BIGINT NOT NULL,
    modulo_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,
    configuracion_extra JSONB,
    CONSTRAINT uk_empresa_modulo UNIQUE (empresa_router_id, modulo_id)
);

ALTER TABLE empresa_modulos
ADD CONSTRAINT fk_empresa_modulo_router
FOREIGN KEY (empresa_router_id) REFERENCES empresas_router(id) ON DELETE CASCADE;

ALTER TABLE empresa_modulos
ADD CONSTRAINT fk_empresa_modulo_modulo
FOREIGN KEY (modulo_id) REFERENCES modulos(id) ON DELETE CASCADE;

CREATE INDEX idx_empresa_modulos_modulo_id ON empresa_modulos(modulo_id);

-- 6. TABLA PROVEEDORES PSE
CREATE TABLE proveedores_pse (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    url_base VARCHAR(255),
    api_key VARCHAR(255),
    usuario VARCHAR(120),
    password VARCHAR(255),
    token TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 7. TABLA PLANTILLAS BASE (PLANTILLAS DEL SISTEMA)
CREATE TABLE plantillas_base (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    tipo_formato VARCHAR(20) NOT NULL,
    layout_base JSONB NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- rollback DROP TABLE plantillas_base;
-- rollback ALTER TABLE empresa_modulos DROP CONSTRAINT fk_empresa_modulo_modulo;
-- rollback ALTER TABLE empresa_modulos DROP CONSTRAINT fk_empresa_modulo_router;
-- rollback DROP TABLE empresa_modulos;
-- rollback DROP TABLE modulos;
-- rollback ALTER TABLE empresas_router DROP CONSTRAINT fk_router_saas;
-- rollback DROP TABLE empresas_router;
-- rollback ALTER TABLE cuentas_saas DROP CONSTRAINT fk_cuenta_plan;
-- rollback DROP TABLE cuentas_saas;
-- rollback DROP TABLE planes;
-- rollback DROP TABLE proveedores_pse;
