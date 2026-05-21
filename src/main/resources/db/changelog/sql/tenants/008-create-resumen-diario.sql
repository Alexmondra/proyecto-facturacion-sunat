-- liquibase formatted sql

-- changeset alex:tenant-008-create-resumen-diario
CREATE TABLE resumen_diarios (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    fecha_documentos DATE NOT NULL,
    codigo_resumen VARCHAR(30) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL,
    ticket_sunat VARCHAR(120),
    mensaje_sunat TEXT,
    fecha_envio TIMESTAMP
);

CREATE TABLE resumen_diario_documentos (
    id BIGSERIAL PRIMARY KEY,
    resumen_diario_id BIGINT NOT NULL,
    documento_id BIGINT NOT NULL,
    condicion_resumen VARCHAR(10),
    CONSTRAINT uk_resumen_documento UNIQUE (resumen_diario_id, documento_id)
);

CREATE TABLE resumen_diario_archivos (
    id BIGSERIAL PRIMARY KEY,
    resumen_diario_id BIGINT NOT NULL,
    tipo_archivo VARCHAR(30) NOT NULL,
    proveedor_almacenamiento VARCHAR(30),
    bucket VARCHAR(120),
    ruta_archivo VARCHAR(500) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL
);

ALTER TABLE resumen_diarios
ADD CONSTRAINT fk_resumen_empresa
FOREIGN KEY (empresa_id) REFERENCES empresas(id);

ALTER TABLE resumen_diarios
ADD CONSTRAINT fk_resumen_sucursal
FOREIGN KEY (sucursal_id) REFERENCES sucursales(id);

ALTER TABLE resumen_diario_documentos
ADD CONSTRAINT fk_resumen_doc_resumen
FOREIGN KEY (resumen_diario_id) REFERENCES resumen_diarios(id);

ALTER TABLE resumen_diario_documentos
ADD CONSTRAINT fk_resumen_doc_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

ALTER TABLE resumen_diario_archivos
ADD CONSTRAINT fk_resumen_archivo_resumen
FOREIGN KEY (resumen_diario_id) REFERENCES resumen_diarios(id);

-- rollback ALTER TABLE resumen_diario_archivos DROP CONSTRAINT fk_resumen_archivo_resumen;
-- rollback ALTER TABLE resumen_diario_documentos DROP CONSTRAINT fk_resumen_doc_documento;
-- rollback ALTER TABLE resumen_diario_documentos DROP CONSTRAINT fk_resumen_doc_resumen;
-- rollback ALTER TABLE resumen_diarios DROP CONSTRAINT fk_resumen_sucursal;
-- rollback ALTER TABLE resumen_diarios DROP CONSTRAINT fk_resumen_empresa;
-- rollback DROP TABLE resumen_diario_archivos;
-- rollback DROP TABLE resumen_diario_documentos;
-- rollback DROP TABLE resumen_diarios;
