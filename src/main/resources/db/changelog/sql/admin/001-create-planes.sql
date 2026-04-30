-- liquibase formatted sql
-- changeset alex:001-create-planes
CREATE TABLE planes (
    id SERIAL PRIMARY KEY,
    nombre_plan VARCHAR(50) NOT NULL,
    codigo VARCHAR(10) UNIQUE,
    limite_mensual_bolsa INTEGER,
    precio_mensual DECIMAL(10,2),
    estado BOOLEAN DEFAULT TRUE
);
-- rollback DROP TABLE planes;