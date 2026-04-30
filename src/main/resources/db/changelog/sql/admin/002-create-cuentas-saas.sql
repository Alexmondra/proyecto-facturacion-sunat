-- liquibase formatted sql

-- changeset alex:002-create-cuentas-saas
CREATE TABLE cuentas_saas (
    id BIGSERIAL PRIMARY KEY,
    access_key VARCHAR(100) NOT NULL,
    plan_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    consumido_mes INT DEFAULT 0,
    fecha_corte INT,
    estado BOOLEAN DEFAULT TRUE
);
ALTER TABLE cuentas_saas
ADD CONSTRAINT fk_cuenta_plan
FOREIGN KEY (plan_id) REFERENCES planes(id);

-- rollback DROP TABLE cuentas_saas;