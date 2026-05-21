-- liquibase formatted sql

-- changeset alex:tenant-002-create-empresa-config
CREATE TABLE empresa_config (
    id BIGINT PRIMARY KEY,
    modo_emision VARCHAR(30) NOT NULL,
    id_pse BIGINT,
    tipo_certificado VARCHAR(30),
    certificado TEXT,
    certificado_pass VARCHAR(255),
    user_sol VARCHAR(30),
    pass_sol VARCHAR(255),
    sunat_client_id VARCHAR(150),
    sunat_client_secret VARCHAR(255),
    numero_cuenta_detraccion VARCHAR(30)
);

ALTER TABLE empresa_config
ADD CONSTRAINT fk_empresa_config_empresa
FOREIGN KEY (id) REFERENCES empresas(id);

-- rollback ALTER TABLE empresa_config DROP CONSTRAINT fk_empresa_config_empresa;
-- rollback DROP TABLE empresa_config;
