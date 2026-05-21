-- liquibase formatted sql

-- changeset alex:tenant-007-create-documento-relaciones
CREATE TABLE documento_detalles (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    item INTEGER NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    cantidad NUMERIC(14,4) NOT NULL,
    unidad_medida VARCHAR(10),
    valor_unitario NUMERIC(14,6) NOT NULL,
    tipo_afectacion VARCHAR(10),
    total_tributos NUMERIC(14,2) NOT NULL DEFAULT 0,
    total NUMERIC(14,2) NOT NULL DEFAULT 0,
    CONSTRAINT uk_documento_detalle_item UNIQUE (documento_id, item)
);

CREATE TABLE documento_detalle_tributos (
    id BIGSERIAL PRIMARY KEY,
    detalle_id BIGINT NOT NULL,
    tributo_id BIGINT NOT NULL,
    base_imponible NUMERIC(14,2) NOT NULL DEFAULT 0,
    porcentaje NUMERIC(12,6),
    cantidad_base NUMERIC(14,4),
    monto NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE documento_tributos (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    tributo_id BIGINT NOT NULL,
    base_imponible NUMERIC(14,2) NOT NULL DEFAULT 0,
    monto NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE documento_sunat (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL UNIQUE,
    ticket VARCHAR(120),
    estado_sunat VARCHAR(30),
    codigo_respuesta_sunat VARCHAR(20),
    mensaje_sunat TEXT,
    fecha_envio TIMESTAMP,
    fecha_respuesta TIMESTAMP
);

CREATE TABLE documento_detracciones (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL UNIQUE,
    codigo_bien_servicio VARCHAR(10),
    porcentaje NUMERIC(7,4),
    monto_detraccion NUMERIC(14,2),
    moneda VARCHAR(10),
    medio_pago VARCHAR(30),
    numero_cuenta_detraccion VARCHAR(30)
);

CREATE TABLE documento_referencias (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    tipo_relacion VARCHAR(10),
    documento_referenciado_id BIGINT,
    tipo_documento_ref VARCHAR(10),
    serie_ref VARCHAR(10),
    numero_ref VARCHAR(30),
    motivo_codigo VARCHAR(10),
    motivo_descripcion VARCHAR(255),
    fecha_emision_ref DATE,
    moneda_ref VARCHAR(10)
);

CREATE TABLE guias_remision (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL UNIQUE,
    tipo_gre VARCHAR(10),
    ambito_traslado VARCHAR(10),
    documento_sustento_id BIGINT,
    tipo_doc_sustento VARCHAR(10),
    serie_doc_sustento VARCHAR(10),
    numero_doc_sustento VARCHAR(30),
    motivo_traslado_codigo VARCHAR(10),
    motivo_traslado_descripcion VARCHAR(255),
    modalidad_transporte VARCHAR(10),
    fecha_inicio_traslado TIMESTAMP,
    peso_total NUMERIC(14,4),
    unidad_peso VARCHAR(10),
    numero_bultos INTEGER,
    ubigeo_partida VARCHAR(10),
    direccion_partida VARCHAR(255),
    ubigeo_llegada VARCHAR(10),
    direccion_llegada VARCHAR(255),
    destinatario_tipo_doc VARCHAR(10),
    destinatario_doc VARCHAR(20),
    destinatario_nombre VARCHAR(255),
    transportista_doc_tipo VARCHAR(10),
    transportista_doc_numero VARCHAR(20),
    transportista_nombre VARCHAR(255),
    vehiculo_placa VARCHAR(20),
    vehiculo_secundario_placa VARCHAR(20),
    conductor_doc_tipo VARCHAR(10),
    conductor_doc_numero VARCHAR(20),
    conductor_nombre VARCHAR(255),
    licencia_conducir VARCHAR(30),
    observaciones TEXT,
    estado_traslado VARCHAR(20)
);

CREATE TABLE guia_remision_detalles (
    id BIGSERIAL PRIMARY KEY,
    guia_remision_id BIGINT NOT NULL,
    item INTEGER NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    unidad_medida VARCHAR(10),
    cantidad NUMERIC(14,4) NOT NULL,
    peso NUMERIC(14,4),
    lote VARCHAR(120),
    CONSTRAINT uk_guia_remision_detalle_item UNIQUE (guia_remision_id, item)
);

CREATE TABLE documento_cuotas (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    numero_cuota INTEGER NOT NULL,
    fecha_vencimiento DATE,
    monto NUMERIC(14,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL
);

CREATE TABLE documento_totales_afectacion (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    tipo_afectacion_codigo VARCHAR(10) NOT NULL,
    tipo_total VARCHAR(20) NOT NULL,
    base_imponible NUMERIC(14,2) NOT NULL DEFAULT 0,
    monto_tributo NUMERIC(14,2) NOT NULL DEFAULT 0,
    monto_total NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE TABLE documento_archivos (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    tipo_archivo VARCHAR(30) NOT NULL,
    proveedor_almacenamiento VARCHAR(30),
    bucket VARCHAR(120),
    ruta_archivo VARCHAR(500) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL
);

ALTER TABLE documento_detalles
ADD CONSTRAINT fk_doc_detalle_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

ALTER TABLE documento_detalles
ADD CONSTRAINT fk_doc_detalle_tipo_afectacion
FOREIGN KEY (tipo_afectacion) REFERENCES catalogo_tipo_afectacion(codigo);

ALTER TABLE documento_detalle_tributos
ADD CONSTRAINT fk_doc_det_tributo_detalle
FOREIGN KEY (detalle_id) REFERENCES documento_detalles(id);

ALTER TABLE documento_detalle_tributos
ADD CONSTRAINT fk_doc_det_tributo_tributo
FOREIGN KEY (tributo_id) REFERENCES catalogo_tributos(id);

ALTER TABLE documento_tributos
ADD CONSTRAINT fk_doc_tributo_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

ALTER TABLE documento_tributos
ADD CONSTRAINT fk_doc_tributo_catalogo
FOREIGN KEY (tributo_id) REFERENCES catalogo_tributos(id);

ALTER TABLE documento_sunat
ADD CONSTRAINT fk_doc_sunat_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

ALTER TABLE documento_detracciones
ADD CONSTRAINT fk_doc_detraccion_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

ALTER TABLE documento_detracciones
ADD CONSTRAINT fk_doc_detraccion_catalogo
FOREIGN KEY (codigo_bien_servicio) REFERENCES catalogo_detracciones(codigo_bien_servicio);

ALTER TABLE documento_referencias
ADD CONSTRAINT fk_doc_referencia_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

ALTER TABLE documento_referencias
ADD CONSTRAINT fk_doc_referencia_documento_ref
FOREIGN KEY (documento_referenciado_id) REFERENCES documento(id);

ALTER TABLE guias_remision
ADD CONSTRAINT fk_guia_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

ALTER TABLE guias_remision
ADD CONSTRAINT fk_guia_documento_sustento
FOREIGN KEY (documento_sustento_id) REFERENCES documento(id);

ALTER TABLE guias_remision
ADD CONSTRAINT fk_guia_ubigeo_partida
FOREIGN KEY (ubigeo_partida) REFERENCES catalogo_ubigeos(codigo);

ALTER TABLE guias_remision
ADD CONSTRAINT fk_guia_ubigeo_llegada
FOREIGN KEY (ubigeo_llegada) REFERENCES catalogo_ubigeos(codigo);

ALTER TABLE guia_remision_detalles
ADD CONSTRAINT fk_guia_detalle_guia
FOREIGN KEY (guia_remision_id) REFERENCES guias_remision(id);

ALTER TABLE documento_cuotas
ADD CONSTRAINT fk_doc_cuota_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

ALTER TABLE documento_totales_afectacion
ADD CONSTRAINT fk_doc_total_afectacion_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

ALTER TABLE documento_totales_afectacion
ADD CONSTRAINT fk_doc_total_afectacion_catalogo
FOREIGN KEY (tipo_afectacion_codigo) REFERENCES catalogo_tipo_afectacion(codigo);

ALTER TABLE documento_archivos
ADD CONSTRAINT fk_doc_archivo_documento
FOREIGN KEY (documento_id) REFERENCES documento(id);

-- rollback ALTER TABLE documento_archivos DROP CONSTRAINT fk_doc_archivo_documento;
-- rollback ALTER TABLE documento_totales_afectacion DROP CONSTRAINT fk_doc_total_afectacion_catalogo;
-- rollback ALTER TABLE documento_totales_afectacion DROP CONSTRAINT fk_doc_total_afectacion_documento;
-- rollback ALTER TABLE documento_cuotas DROP CONSTRAINT fk_doc_cuota_documento;
-- rollback ALTER TABLE guia_remision_detalles DROP CONSTRAINT fk_guia_detalle_guia;
-- rollback ALTER TABLE guias_remision DROP CONSTRAINT fk_guia_ubigeo_llegada;
-- rollback ALTER TABLE guias_remision DROP CONSTRAINT fk_guia_ubigeo_partida;
-- rollback ALTER TABLE guias_remision DROP CONSTRAINT fk_guia_documento_sustento;
-- rollback ALTER TABLE guias_remision DROP CONSTRAINT fk_guia_documento;
-- rollback ALTER TABLE documento_referencias DROP CONSTRAINT fk_doc_referencia_documento_ref;
-- rollback ALTER TABLE documento_referencias DROP CONSTRAINT fk_doc_referencia_documento;
-- rollback ALTER TABLE documento_detracciones DROP CONSTRAINT fk_doc_detraccion_catalogo;
-- rollback ALTER TABLE documento_detracciones DROP CONSTRAINT fk_doc_detraccion_documento;
-- rollback ALTER TABLE documento_sunat DROP CONSTRAINT fk_doc_sunat_documento;
-- rollback ALTER TABLE documento_tributos DROP CONSTRAINT fk_doc_tributo_catalogo;
-- rollback ALTER TABLE documento_tributos DROP CONSTRAINT fk_doc_tributo_documento;
-- rollback ALTER TABLE documento_detalle_tributos DROP CONSTRAINT fk_doc_det_tributo_tributo;
-- rollback ALTER TABLE documento_detalle_tributos DROP CONSTRAINT fk_doc_det_tributo_detalle;
-- rollback ALTER TABLE documento_detalles DROP CONSTRAINT fk_doc_detalle_tipo_afectacion;
-- rollback ALTER TABLE documento_detalles DROP CONSTRAINT fk_doc_detalle_documento;
-- rollback DROP TABLE documento_archivos;
-- rollback DROP TABLE documento_totales_afectacion;
-- rollback DROP TABLE documento_cuotas;
-- rollback DROP TABLE guia_remision_detalles;
-- rollback DROP TABLE guias_remision;
-- rollback DROP TABLE documento_referencias;
-- rollback DROP TABLE documento_detracciones;
-- rollback DROP TABLE documento_sunat;
-- rollback DROP TABLE documento_tributos;
-- rollback DROP TABLE documento_detalle_tributos;
-- rollback DROP TABLE documento_detalles;
