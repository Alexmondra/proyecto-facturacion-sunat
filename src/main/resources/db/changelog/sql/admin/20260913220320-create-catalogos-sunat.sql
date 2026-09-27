-- liquibase formatted sql

-- changeset alex:002-initial-catalogos-sunat.sql

CREATE TABLE catalogo_tipo_afectacion (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    tipo VARCHAR(30),
    tipotributo_codigo VARCHAR(10),
    afecta_igv BOOLEAN DEFAULT FALSE,
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE catalogo_detracciones (
    id BIGSERIAL PRIMARY KEY,
    codigo_bien_servicio VARCHAR(10) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    porcentaje NUMERIC(7,4) NOT NULL,
    monto_minimo NUMERIC(14,2),
    vigencia_desde DATE,
    vigencia_hasta DATE,
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE catalogo_tipo_documento (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE catalogo_ubigeos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    departamento VARCHAR(120) NOT NULL,
    provincia VARCHAR(120) NOT NULL,
    distrito VARCHAR(120) NOT NULL,
    es_amazonia BOOLEAN NOT NULL DEFAULT FALSE,
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_ubigeos_busqueda ON catalogo_ubigeos(departamento, provincia, distrito);
CREATE INDEX idx_ubigeos_amazonia ON catalogo_ubigeos(es_amazonia);

CREATE TABLE catalogo_tributos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255),
    tipo_calculo VARCHAR(30),
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE catalogo_tributo_tasas (
    id BIGSERIAL PRIMARY KEY,
    tributo_id BIGINT NOT NULL,
    valor NUMERIC(12,6) NOT NULL,
    vigencia_desde DATE,
    vigencia_hasta DATE,
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE catalogo_tributo_tasas
ADD CONSTRAINT fk_tasa_tributo
FOREIGN KEY (tributo_id) REFERENCES catalogo_tributos(id);

CREATE INDEX idx_tributo_tasas_tributo_id ON catalogo_tributo_tasas(tributo_id);

-- rollback ALTER TABLE catalogo_tributo_tasas DROP CONSTRAINT fk_tasa_tributo;
-- rollback DROP TABLE catalogo_tributo_tasas;
-- rollback DROP TABLE catalogo_tributos;
-- rollback DROP TABLE catalogo_ubigeos;
-- rollback DROP TABLE catalogo_tipo_documento;
-- rollback DROP TABLE catalogo_detracciones;
-- rollback DROP TABLE catalogo_tipo_afectacion;
