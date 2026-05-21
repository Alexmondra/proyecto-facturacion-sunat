-- liquibase formatted sql

-- changeset alex:005-create-modulos-y-empresa-modulos
CREATE TABLE modulos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255),
    estado BOOLEAN NOT NULL DEFAULT TRUE
);

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
FOREIGN KEY (empresa_router_id) REFERENCES empresas_router(id);

ALTER TABLE empresa_modulos
ADD CONSTRAINT fk_empresa_modulo_modulo
FOREIGN KEY (modulo_id) REFERENCES modulos(id);

-- rollback ALTER TABLE empresa_modulos DROP CONSTRAINT fk_empresa_modulo_modulo;
-- rollback ALTER TABLE empresa_modulos DROP CONSTRAINT fk_empresa_modulo_router;
-- rollback DROP TABLE empresa_modulos;
-- rollback DROP TABLE modulos;
