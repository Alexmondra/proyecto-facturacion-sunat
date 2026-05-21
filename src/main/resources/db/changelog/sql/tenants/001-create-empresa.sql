-- liquibase formatted sql

-- changeset alex:tenant-001-create-empresas
CREATE TABLE empresas (
    id BIGSERIAL PRIMARY KEY,
    ruc VARCHAR(20) NOT NULL UNIQUE,
    envio_asincrono BOOLEAN NOT NULL DEFAULT FALSE,
    logo TEXT,
    incluido_tributo BOOLEAN NOT NULL DEFAULT TRUE,
    razon_social VARCHAR(255) NOT NULL,
    direccion_fiscal VARCHAR(255),
    entorno VARCHAR(30) NOT NULL DEFAULT 'PSE',
    limite_asignado INTEGER NOT NULL DEFAULT 0,
    consumido_mes INTEGER NOT NULL DEFAULT 0
);

-- rollback DROP TABLE empresas;
