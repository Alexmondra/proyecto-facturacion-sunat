-- liquibase formatted sql

-- changeset alex:20260926021000-alter-series-unique-empresa
-- comment: Restriccion unica de series a nivel de empresa (evita series duplicadas entre sucursales)

ALTER TABLE series DROP CONSTRAINT IF EXISTS uq_serie_sucursal_tipo;
ALTER TABLE series ADD CONSTRAINT uq_serie_tipo_comprobante UNIQUE (tipo_comprobante, serie);

-- rollback ALTER TABLE series DROP CONSTRAINT IF EXISTS uq_serie_tipo_comprobante;
-- rollback ALTER TABLE series ADD CONSTRAINT uq_serie_sucursal_tipo UNIQUE (sucursal_id, tipo_comprobante, serie);
