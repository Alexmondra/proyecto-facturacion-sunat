-- liquibase formatted sql

-- changeset alex:003-create-empresas-router
CREATE TABLE empresas_router (
    id BIGSERIAL PRIMARY KEY,
    saas_id BIGINT NOT NULL,
    ruc VARCHAR(20) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    db_schema VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL
);

ALTER TABLE empresas_router
ADD CONSTRAINT fk_router_saas
FOREIGN KEY (saas_id) REFERENCES cuentas_saas(id);

-- rollback DROP TABLE empresas_router;
