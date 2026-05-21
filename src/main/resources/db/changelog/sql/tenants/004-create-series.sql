-- liquibase formatted sql

-- changeset alex:tenant-004-create-series
CREATE TABLE series (
    id BIGSERIAL PRIMARY KEY,
    sucursal_id BIGINT NOT NULL,
    tipo_comprobante VARCHAR(10) NOT NULL,
    serie VARCHAR(10) NOT NULL,
    correlativo BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_series UNIQUE (sucursal_id, tipo_comprobante, serie)
);

ALTER TABLE series
ADD CONSTRAINT fk_serie_sucursal
FOREIGN KEY (sucursal_id) REFERENCES sucursales(id);

-- rollback ALTER TABLE series DROP CONSTRAINT fk_serie_sucursal;
-- rollback DROP TABLE series;
