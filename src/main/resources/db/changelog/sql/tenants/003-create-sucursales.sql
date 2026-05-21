-- liquibase formatted sql

-- changeset alex:tenant-003-create-sucursales
CREATE TABLE sucursales (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    ubigeo VARCHAR(10),
    direccion VARCHAR(255) NOT NULL,
    telefono VARCHAR(20),
    email VARCHAR(120),
    nombre_sucursal VARCHAR(120) NOT NULL,
    imagen_sucursal TEXT,
    impuesto_porcentaje NUMERIC(7,4),
    configuracion_extra JSONB,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_sucursal_empresa_codigo UNIQUE (empresa_id, codigo)
);

ALTER TABLE sucursales
ADD CONSTRAINT fk_sucursal_empresa
FOREIGN KEY (empresa_id) REFERENCES empresas(id);

-- rollback ALTER TABLE sucursales DROP CONSTRAINT fk_sucursal_empresa;
-- rollback DROP TABLE sucursales;
