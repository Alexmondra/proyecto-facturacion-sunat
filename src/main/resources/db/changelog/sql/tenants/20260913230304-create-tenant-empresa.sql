-- liquibase formatted sql

-- ============================================================================
-- ESQUEMA TENANT (EMPRESA MULTI-INQUILINO)
-- ============================================================================

-- changeset alex:tenant-001-create-tenant-empresa
-- validCheckSum: ANY

-- 1. TABLA EMPRESAS
CREATE TABLE empresas (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    ruc VARCHAR(20) NOT NULL UNIQUE,
    logo TEXT,
    incluido_tributo BOOLEAN NOT NULL DEFAULT TRUE,
    razon_social VARCHAR(255) NOT NULL,
    direccion_fiscal VARCHAR(255),
    entorno VARCHAR(30) NOT NULL DEFAULT 'dev',
    limite_asignado INTEGER NOT NULL DEFAULT 0,
    consumido_mes INTEGER NOT NULL DEFAULT 0
);

-- 2. CONFIGURACIÓN DE EMPRESA
CREATE TABLE empresa_config (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    empresa_id UUID NOT NULL UNIQUE,
    envio_asincrono BOOLEAN NOT NULL DEFAULT TRUE,
    modo_emision VARCHAR(10) NOT NULL DEFAULT 'PROPIO' CHECK (modo_emision IN ('PROPIO', 'PSE')),
    id_pse BIGINT,
    webhook_url VARCHAR(255),
    tipo_certificado VARCHAR(50),
    certificado TEXT,
    certificado_pass VARCHAR(255),
    user_sol VARCHAR(50),
    pass_sol VARCHAR(255),
    sunat_client_id VARCHAR(150),
    sunat_client_secret VARCHAR(255),
    numero_cuenta_detraccion VARCHAR(50),
    CONSTRAINT fk_empresa_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE
);

COMMENT ON COLUMN empresa_config.id_pse IS 'Obligatorio solo si modo_emision es PSE, sino NULL';
COMMENT ON COLUMN empresa_config.webhook_url IS 'Obligatorio solo si envio_asincrono es FALSE, sino NULL';

-- 3. SUCURSALES
CREATE TABLE sucursales (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    empresa_id UUID NOT NULL,
    codigo VARCHAR(10) NOT NULL,
    ubigeo VARCHAR(10),
    direccion VARCHAR(255),
    telefono VARCHAR(30),
    email VARCHAR(150),
    nombre_sucursal VARCHAR(150) NOT NULL,
    imagen_sucursal TEXT,
    impuesto_porcentaje NUMERIC(5, 2) DEFAULT 18.00,
    configuracion_extra JSONB,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_sucursales_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT uq_sucursal_empresa_codigo UNIQUE (empresa_id, codigo)
);

-- 4. SERIES POR SUCURSAL
CREATE TABLE series (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    sucursal_id UUID NOT NULL,
    tipo_comprobante VARCHAR(5) NOT NULL,
    serie VARCHAR(10) NOT NULL,
    correlativo INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_series_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) ON DELETE CASCADE,
    CONSTRAINT uq_serie_tipo_comprobante UNIQUE (tipo_comprobante, serie)
);

-- 5. DOCUMENTOS FISCALES (CABECERA)
CREATE TABLE documentos (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    clave_idempotencia VARCHAR(100) UNIQUE,
    empresa_id UUID NOT NULL,
    sucursal_id UUID NOT NULL,
    serie_id UUID,
    tipo_comprobante VARCHAR(5) NOT NULL,
    serie VARCHAR(10) NOT NULL,
    numero INTEGER NOT NULL,
    fecha_emision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    moneda VARCHAR(5) NOT NULL DEFAULT 'PEN',
    tipo_operacion VARCHAR(10),
    cliente_tipo_doc VARCHAR(5) NOT NULL,
    cliente_numero_doc VARCHAR(20) NOT NULL,
    cliente_nombre VARCHAR(255) NOT NULL,
    forma_pago VARCHAR(30) DEFAULT 'CONTADO',
    total_otros_cargos NUMERIC(14, 2) DEFAULT 0.00,
    total_tributos NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    estado_interno VARCHAR(30) NOT NULL DEFAULT 'REGISTRADO',
    hash_cpe VARCHAR(150),
    payload_entrada JSONB,
    CONSTRAINT fk_documentos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT fk_documentos_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id),
    CONSTRAINT fk_documentos_serie FOREIGN KEY (serie_id) REFERENCES series(id) ON DELETE SET NULL,
    CONSTRAINT uq_documentos_empresa_emision UNIQUE (empresa_id, tipo_comprobante, serie, numero)
);

-- 6. DETALLES DEL COMPROBANTE
CREATE TABLE documento_detalles (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    documento_id UUID NOT NULL,
    item INTEGER NOT NULL,
    descripcion TEXT NOT NULL,
    cantidad NUMERIC(14, 4) NOT NULL,
    unidad_medida VARCHAR(10) NOT NULL DEFAULT 'NIU',
    valor_unitario NUMERIC(14, 4) NOT NULL,
    tipo_afectacion VARCHAR(10) NOT NULL,
    total_tributos NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_detalles_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
    CONSTRAINT uq_documento_detalles_item UNIQUE (documento_id, item)
);

-- 7. TRIBUTOS POR DETALLE (LINE ITEM)
CREATE TABLE documento_detalle_tributos (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    detalle_id UUID NOT NULL,
    tributo_id BIGINT NOT NULL,
    base_imponible NUMERIC(14, 2) NOT NULL,
    porcentaje NUMERIC(5, 2) NOT NULL,
    cantidad_base NUMERIC(14, 4),
    monto NUMERIC(14, 2) NOT NULL,
    CONSTRAINT fk_det_tributos_detalle FOREIGN KEY (detalle_id) REFERENCES documento_detalles(id) ON DELETE CASCADE
);

-- 8. TRIBUTOS GLOBALES DEL COMPROBANTE
CREATE TABLE documento_tributos (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    documento_id UUID NOT NULL,
    tributo_id BIGINT NOT NULL,
    base_imponible NUMERIC(14, 2) NOT NULL,
    monto NUMERIC(14, 2) NOT NULL,
    CONSTRAINT fk_doc_tributos_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE
);

-- 9. TOTALES AGRUPADOS POR TIPO DE AFECTACIÓN
CREATE TABLE documento_totales_afectacion (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    documento_id UUID NOT NULL,
    tipo_afectacion_codigo VARCHAR(10) NOT NULL,
    tipo_total VARCHAR(50),
    base_imponible NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    monto_tributo NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    monto_total NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_totales_afectacion_doc FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
    CONSTRAINT uq_totales_afectacion_tipo UNIQUE (documento_id, tipo_afectacion_codigo)
);

-- 10. ESTADO Y RESPUESTA SUNAT
CREATE TABLE documento_sunat (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    documento_id UUID NOT NULL UNIQUE,
    ticket VARCHAR(100),
    estado_sunat VARCHAR(50),
    codigo_respuesta_sunat VARCHAR(20),
    mensaje_sunat TEXT,
    fecha_envio TIMESTAMP,
    fecha_respuesta TIMESTAMP,
    CONSTRAINT fk_doc_sunat_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE
);

-- 11. ARCHIVOS DIGITALES ASOCIADOS (XML, CDR, PDF)
CREATE TABLE documento_archivos (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    documento_id UUID NOT NULL,
    tipo_archivo VARCHAR(30) NOT NULL,
    proveedor_almacenamiento VARCHAR(50) DEFAULT 'LOCAL',
    bucket VARCHAR(100),
    ruta_archivo TEXT NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    CONSTRAINT fk_doc_archivos_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE
);

-- 12. CUOTAS DE PAGO (VENTAS A CRÉDITO)
CREATE TABLE documento_cuotas (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    documento_id UUID NOT NULL,
    numero_cuota INTEGER NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    monto NUMERIC(14, 2) NOT NULL,
    moneda VARCHAR(5) NOT NULL DEFAULT 'PEN',
    CONSTRAINT fk_doc_cuotas_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
    CONSTRAINT uq_documento_cuotas_numero UNIQUE (documento_id, numero_cuota)
);

-- 13. INFORMACIÓN DE DETRACCIÓN
CREATE TABLE documento_detracciones (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    documento_id UUID NOT NULL UNIQUE,
    codigo_bien_servicio VARCHAR(10) NOT NULL,
    porcentaje NUMERIC(5, 2) NOT NULL,
    monto_detraccion NUMERIC(14, 2) NOT NULL,
    moneda VARCHAR(5) NOT NULL DEFAULT 'PEN',
    medio_pago VARCHAR(10),
    numero_cuenta_detraccion VARCHAR(50),
    CONSTRAINT fk_doc_detracciones_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE
);

-- 14. REFERENCIAS A COMPROBANTES PREVIOS (NOTAS DE CRÉDITO/DÉBITO)
CREATE TABLE documento_referencias (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    documento_id UUID NOT NULL,
    tipo_relacion VARCHAR(10),
    documento_referenciado_id UUID,
    tipo_documento_ref VARCHAR(5),
    serie_ref VARCHAR(10),
    numero_ref INTEGER,
    motivo_codigo VARCHAR(10),
    motivo_descripcion TEXT,
    fecha_emision_ref DATE,
    moneda_ref VARCHAR(5),
    CONSTRAINT fk_doc_referencias_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_referencias_ref FOREIGN KEY (documento_referenciado_id) REFERENCES documentos(id) ON DELETE SET NULL
);

-- 15. GUÍAS DE REMISIÓN ELECTRÓNICA (GRE)
CREATE TABLE guias_remision (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    documento_id UUID UNIQUE,
    tipo_gre VARCHAR(10) NOT NULL,
    ambito_traslado VARCHAR(30),
    documento_sustento_id UUID,
    tipo_doc_sustento VARCHAR(5),
    serie_doc_sustento VARCHAR(10),
    numero_doc_sustento INTEGER,
    motivo_traslado_codigo VARCHAR(10),
    motivo_traslado_descripcion VARCHAR(255),
    modalidad_transporte VARCHAR(10),
    fecha_inicio_traslado DATE,
    peso_total NUMERIC(12, 3),
    unidad_peso VARCHAR(10) DEFAULT 'KGM',
    numero_bultos INTEGER,
    ubigeo_partida VARCHAR(10),
    direccion_partida VARCHAR(255),
    ubigeo_llegada VARCHAR(10),
    direccion_llegada VARCHAR(255),
    destinatario_tipo_doc VARCHAR(5),
    destinatario_doc VARCHAR(20),
    destinatario_nombre VARCHAR(255),
    transportista_doc_tipo VARCHAR(5),
    transportista_doc_numero VARCHAR(20),
    transportista_nombre VARCHAR(255),
    vehiculo_placa VARCHAR(20),
    vehiculo_secundario_placa VARCHAR(20),
    conductor_doc_tipo VARCHAR(5),
    conductor_doc_numero VARCHAR(20),
    conductor_nombre VARCHAR(255),
    licencia_conducir VARCHAR(30),
    observaciones TEXT,
    estado_traslado VARCHAR(30),
    CONSTRAINT fk_guias_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
    CONSTRAINT fk_guias_sustento FOREIGN KEY (documento_sustento_id) REFERENCES documentos(id) ON DELETE SET NULL
);

-- 16. DETALLES DE GUÍAS DE REMISIÓN
CREATE TABLE guia_remision_detalles (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    guia_remision_id UUID NOT NULL,
    item INTEGER NOT NULL,
    descripcion TEXT NOT NULL,
    unidad_medida VARCHAR(10) NOT NULL DEFAULT 'NIU',
    cantidad NUMERIC(14, 4) NOT NULL,
    peso NUMERIC(12, 3),
    lote VARCHAR(100),
    CONSTRAINT fk_guia_detalles_guia FOREIGN KEY (guia_remision_id) REFERENCES guias_remision(id) ON DELETE CASCADE,
    CONSTRAINT uq_guia_detalles_item UNIQUE (guia_remision_id, item)
);

-- 17. RESÚMENES DIARIOS Y COMUNICACIONES DE BAJA
CREATE TABLE resumen_diarios (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    empresa_id UUID NOT NULL,
    sucursal_id UUID,
    fecha_documentos DATE NOT NULL,
    codigo_resumen VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    ticket_sunat VARCHAR(100),
    mensaje_sunat TEXT,
    fecha_envio TIMESTAMP,
    CONSTRAINT fk_resumen_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT fk_resumen_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) ON DELETE SET NULL,
    CONSTRAINT uq_resumen_empresa_codigo UNIQUE (empresa_id, codigo_resumen)
);

-- 18. DOCUMENTOS INCLUIDOS EN RESUMEN DIARIO
CREATE TABLE resumen_diario_documentos (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    resumen_diario_id UUID NOT NULL,
    documento_id UUID NOT NULL,
    condicion_resumen INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_res_doc_resumen FOREIGN KEY (resumen_diario_id) REFERENCES resumen_diarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_res_doc_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
    CONSTRAINT uq_resumen_documento UNIQUE (resumen_diario_id, documento_id)
);

-- 19. ARCHIVOS DEL RESUMEN DIARIO (XML/CDR)
CREATE TABLE resumen_diario_archivos (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    resumen_diario_id UUID NOT NULL,
    tipo_archivo VARCHAR(30) NOT NULL,
    proveedor_almacenamiento VARCHAR(50) DEFAULT 'LOCAL',
    bucket VARCHAR(100),
    ruta_archivo TEXT NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    CONSTRAINT fk_res_archivos_resumen FOREIGN KEY (resumen_diario_id) REFERENCES resumen_diarios(id) ON DELETE CASCADE
);

-- ----------------------------------------------------------------------------
-- ÍNDICES DE RENDIMIENTO (OPTIMIZACIÓN PARA CONSULTAS FRECUENTES)
-- ----------------------------------------------------------------------------
CREATE INDEX idx_documentos_empresa_id ON documentos(empresa_id);
CREATE INDEX idx_documentos_sucursal_id ON documentos(sucursal_id);
CREATE INDEX idx_documentos_fecha_emision ON documentos(fecha_emision);
CREATE INDEX idx_documentos_estado_interno ON documentos(estado_interno);
CREATE INDEX idx_documentos_tipo_fecha ON documentos(tipo_comprobante, fecha_emision);
CREATE INDEX idx_documentos_cliente ON documentos(cliente_tipo_doc, cliente_numero_doc);
CREATE INDEX idx_documento_detalles_doc_id ON documento_detalles(documento_id);
CREATE INDEX idx_doc_detalle_tributos_det_id ON documento_detalle_tributos(detalle_id);
CREATE INDEX idx_doc_tributos_doc_id ON documento_tributos(documento_id);
CREATE INDEX idx_doc_totales_afectacion_doc_id ON documento_totales_afectacion(documento_id);
CREATE INDEX idx_doc_sunat_estado ON documento_sunat(estado_sunat);
CREATE INDEX idx_doc_sunat_ticket ON documento_sunat(ticket);
CREATE INDEX idx_doc_archivos_doc_id ON documento_archivos(documento_id);
CREATE INDEX idx_doc_cuotas_doc_id ON documento_cuotas(documento_id);
CREATE INDEX idx_doc_referencias_doc_id ON documento_referencias(documento_id);
CREATE INDEX idx_doc_referencias_ref_id ON documento_referencias(documento_referenciado_id);
CREATE INDEX idx_guias_remision_doc_id ON guias_remision(documento_id);
CREATE INDEX idx_guias_sustento_id ON guias_remision(documento_sustento_id);
CREATE INDEX idx_guia_detalles_guia_id ON guia_remision_detalles(guia_remision_id);
CREATE INDEX idx_resumen_fecha_estado ON resumen_diarios(fecha_documentos, estado);
CREATE INDEX idx_resumen_docs_resumen_id ON resumen_diario_documentos(resumen_diario_id);
CREATE INDEX idx_resumen_docs_documento_id ON resumen_diario_documentos(documento_id);

-- 15. TABLA PLANTILLAS DE IMPRESIÓN (PERSONALIZADAS POR EMPRESA / SUCURSAL)
CREATE TABLE plantillas_impresion (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    empresa_id UUID NOT NULL,
    sucursal_id UUID, -- NULL = general para toda la empresa; con UUID = exclusiva de esa sucursal
    nombre VARCHAR(100) NOT NULL DEFAULT 'Plantilla Estándar',
    tipo_formato VARCHAR(20) NOT NULL, -- 'TICKET_80', 'TICKET_58', 'A4'
    layout JSONB NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT true,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plantillas_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE CASCADE,
    CONSTRAINT fk_plantillas_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id) ON DELETE CASCADE
);

-- Asegura solo un diseño activo por formato a nivel global de empresa
CREATE UNIQUE INDEX uq_plantilla_empresa_global 
ON plantillas_impresion (empresa_id, tipo_formato) 
WHERE sucursal_id IS NULL;

-- Asegura solo un diseño activo por formato por sucursal específica
CREATE UNIQUE INDEX uq_plantilla_sucursal_especifica 
ON plantillas_impresion (empresa_id, sucursal_id, tipo_formato) 
WHERE sucursal_id IS NOT NULL;

-- ----------------------------------------------------------------------------
-- ROLLBACK EN ORDEN INVERSO ESTRICTO (HIJOS PRIMERO, PADRES AL FINAL)
-- ----------------------------------------------------------------------------
-- rollback DROP TABLE IF EXISTS plantillas_impresion CASCADE;
-- rollback DROP TABLE IF EXISTS resumen_diario_archivos CASCADE;
-- rollback DROP TABLE IF EXISTS resumen_diario_documentos CASCADE;
-- rollback DROP TABLE IF EXISTS resumen_diarios CASCADE;
-- rollback DROP TABLE IF EXISTS guia_remision_detalles CASCADE;
-- rollback DROP TABLE IF EXISTS guias_remision CASCADE;
-- rollback DROP TABLE IF EXISTS documento_referencias CASCADE;
-- rollback DROP TABLE IF EXISTS documento_detracciones CASCADE;
-- rollback DROP TABLE IF EXISTS documento_cuotas CASCADE;
-- rollback DROP TABLE IF EXISTS documento_archivos CASCADE;
-- rollback DROP TABLE IF EXISTS documento_sunat CASCADE;
-- rollback DROP TABLE IF EXISTS documento_totales_afectacion CASCADE;
-- rollback DROP TABLE IF EXISTS documento_tributos CASCADE;
-- rollback DROP TABLE IF EXISTS documento_detalle_tributos CASCADE;
-- rollback DROP TABLE IF EXISTS documento_detalles CASCADE;
-- rollback DROP TABLE IF EXISTS documentos CASCADE;
-- rollback DROP TABLE IF EXISTS series CASCADE;
-- rollback DROP TABLE IF EXISTS sucursales CASCADE;
-- rollback DROP TABLE IF EXISTS empresa_config CASCADE;
-- rollback DROP TABLE IF EXISTS empresas CASCADE;
