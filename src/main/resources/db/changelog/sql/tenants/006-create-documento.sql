-- liquibase formatted sql

-- changeset alex:tenant-006-create-documento
CREATE TABLE documento (
    id BIGSERIAL PRIMARY KEY,
    clave_idempotencia VARCHAR(120) NOT NULL UNIQUE,
    empresa_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    serie_id BIGINT,
    tipo_comprobante VARCHAR(10) NOT NULL,
    serie VARCHAR(10) NOT NULL,
    numero BIGINT NOT NULL,
    fecha_emision TIMESTAMP NOT NULL,
    moneda VARCHAR(10) NOT NULL,
    tipo_operacion VARCHAR(10),
    cliente_tipo_doc VARCHAR(5),
    cliente_numero_doc VARCHAR(20),
    cliente_nombre VARCHAR(255),
    forma_pago VARCHAR(30),
    total_otros_cargos NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_tributos NUMERIC(14,2) NOT NULL DEFAULT 0,
    total NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado_interno VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    hash_cpe VARCHAR(120),
    payload_entrada JSONB,
    CONSTRAINT uk_documento_serie_numero UNIQUE (empresa_id, tipo_comprobante, serie, numero)
);

ALTER TABLE documento
ADD CONSTRAINT fk_documento_empresa
FOREIGN KEY (empresa_id) REFERENCES empresas(id);

ALTER TABLE documento
ADD CONSTRAINT fk_documento_sucursal
FOREIGN KEY (sucursal_id) REFERENCES sucursales(id);

ALTER TABLE documento
ADD CONSTRAINT fk_documento_serie
FOREIGN KEY (serie_id) REFERENCES series(id);

-- rollback ALTER TABLE documento DROP CONSTRAINT fk_documento_serie;
-- rollback ALTER TABLE documento DROP CONSTRAINT fk_documento_sucursal;
-- rollback ALTER TABLE documento DROP CONSTRAINT fk_documento_empresa;
-- rollback DROP TABLE documento;
