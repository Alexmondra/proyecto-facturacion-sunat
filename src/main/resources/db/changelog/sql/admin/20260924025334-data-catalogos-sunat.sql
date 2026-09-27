-- liquibase formatted sql

-- ============================================================================
-- DATOS MAESTROS: CATÁLOGOS TRIBUTARIOS Y FISCALES OFICIALES DE SUNAT
-- ============================================================================

-- changeset alex:006-data-catalogos-sunat.sql
-- comment: Carga inicial de catalogo_tributos, tasas vigentes, tipos de afectacion, tipos de documento y detracciones

-- 1. CATÁLOGO 05: TRIBUTOS SUNAT
INSERT INTO catalogo_tributos (codigo, nombre, descripcion, tipo_calculo, estado)
VALUES
    ('1000', 'IGV', 'IMPUESTO GENERAL A LAS VENTAS', 'PORCENTAJE', true),
    ('2000', 'ISC', 'IMPUESTO SELECTIVO AL CONSUMO', 'PORCENTAJE', true),
    ('7152', 'ICBPER', 'IMPUESTO AL CONSUMO DE BOLSAS PLASTICAS', 'MONTO_FIJO', true),
    ('9995', 'EXPORTACIÓN', 'EXPORTACION DE BIENES Y SERVICIOS', NULL, true),
    ('9996', 'GRATUITO', 'TRANSFERENCIA GRATUITA', NULL, true),
    ('9997', 'EXONERADO', 'OPERACION EXONERADA DEL IGV', NULL, true),
    ('9998', 'INAFECTO', 'OPERACION INAFECTA DEL IGV', NULL, true),
    ('9999', 'OTROS CONCEPTOS DE PAGO', 'OTROS CONCEPTOS DE PAGO', NULL, true)
ON CONFLICT (codigo) DO UPDATE 
SET tipo_calculo = EXCLUDED.tipo_calculo,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion;

-- 2. TASAS VIGENTES DE TRIBUTOS
-- Tasa IGV 18.00%
INSERT INTO catalogo_tributo_tasas (tributo_id, valor, vigencia_desde, estado)
SELECT t.id, 18.00, '2011-03-01'::date, true
FROM catalogo_tributos t
WHERE t.codigo = '1000'
  AND NOT EXISTS (
      SELECT 1 FROM catalogo_tributo_tasas r WHERE r.tributo_id = t.id AND r.valor = 18.00
  );

-- Tasa ICBPER S/ 0.50 por bolsa
INSERT INTO catalogo_tributo_tasas (tributo_id, valor, vigencia_desde, estado)
SELECT t.id, 0.50, '2023-01-01'::date, true
FROM catalogo_tributos t
WHERE t.codigo = '7152'
  AND NOT EXISTS (
      SELECT 1 FROM catalogo_tributo_tasas r WHERE r.tributo_id = t.id AND r.valor = 0.50
  );

-- 3. CATÁLOGO 07: TIPO DE AFECTACIÓN DEL IGV
INSERT INTO catalogo_tipo_afectacion (codigo, descripcion, tipo, tipotributo_codigo, afecta_igv, estado)
VALUES
    ('10', 'Gravado - Operación Onerosa', 'GRAVADO', '1000', true, true),
    ('11', 'Gravado – Retiro por premio', 'GRATUITO', '9996', false, true),
    ('12', 'Gravado – Retiro por donación', 'GRATUITO', '9996', false, true),
    ('13', 'Gravado – Retiro', 'GRATUITO', '9996', false, true),
    ('14', 'Gravado – Retiro por publicidad', 'GRATUITO', '9996', false, true),
    ('15', 'Gravado – Bonificaciones', 'GRATUITO', '9996', false, true),
    ('16', 'Gravado – Retiro por entrega a trabajadores', 'GRATUITO', '9996', false, true),
    ('17', 'Gravado – IVAP', 'GRAVADO', '1000', true, true),
    ('20', 'Exonerado - Operación Onerosa', 'EXONERADO', '9997', false, true),
    ('21', 'Exonerado – Transferencia Gratuita', 'GRATUITO', '9996', false, true),
    ('30', 'Inafecto - Operación Onerosa', 'INAFECTO', '9998', false, true),
    ('31', 'Inafecto – Retiro por Bonificación', 'GRATUITO', '9996', false, true),
    ('32', 'Inafecto – Retiro', 'GRATUITO', '9996', false, true),
    ('33', 'Inafecto – Retiro por Muestras Médicas', 'GRATUITO', '9996', false, true),
    ('34', 'Inafecto - Retiro por Convenio Colectivo', 'GRATUITO', '9996', false, true),
    ('35', 'Inafecto – Retiro por premio', 'GRATUITO', '9996', false, true),
    ('36', 'Inafecto - Retiro por publicidad', 'GRATUITO', '9996', false, true),
    ('40', 'Exportación', 'EXPORTACION', '9995', false, true)
ON CONFLICT (codigo) DO UPDATE 
SET tipo = EXCLUDED.tipo,
    tipotributo_codigo = EXCLUDED.tipotributo_codigo,
    afecta_igv = EXCLUDED.afecta_igv,
    descripcion = EXCLUDED.descripcion;

-- 4. CATÁLOGO 01: TIPO DE COMPROBANTE DE PAGO
INSERT INTO catalogo_tipo_documento (codigo, descripcion, estado)
VALUES
    ('01', 'FACTURA', true),
    ('03', 'BOLETA DE VENTA', true),
    ('07', 'NOTA DE CREDITO', true),
    ('08', 'NOTA DE DEBITO', true),
    ('09', 'GUIA DE REMISIÓN REMITENTE', true),
    ('12', 'TICKET DE MAQUINA REGISTRADORA', true),
    ('14', 'RECIBO SERVICIOS PÚBLICOS', true),
    ('20', 'COMPROBANTE DE RETENCION', true),
    ('31', 'GUIA DE REMISIÓN TRANSPORTISTA', true),
    ('40', 'COMPROBANTE DE PERCEPCION', true)
ON CONFLICT (codigo) DO UPDATE 
SET descripcion = EXCLUDED.descripcion,
    estado = EXCLUDED.estado;

-- 5. CATÁLOGO 54: BIENES Y SERVICIOS SUJETOS A DETRACCIONES SPOT
INSERT INTO catalogo_detracciones (codigo_bien_servicio, descripcion, porcentaje, monto_minimo, vigencia_desde, estado)
VALUES
    ('019', 'Arrendamiento de bienes muebles e inmuebles', 10.0000, 700.00, '2020-01-01', true),
    ('020', 'Mantenimiento y reparación de bienes muebles', 12.0000, 700.00, '2020-01-01', true),
    ('021', 'Movimiento de carga', 10.0000, 700.00, '2020-01-01', true),
    ('022', 'Otros servicios empresariales', 12.0000, 700.00, '2020-01-01', true),
    ('024', 'Comisión mercantil', 10.0000, 700.00, '2020-01-01', true),
    ('025', 'Fabricación de bienes por encargo', 10.0000, 700.00, '2020-01-01', true),
    ('026', 'Servicio de transporte de carga', 4.0000, 400.00, '2020-01-01', true),
    ('030', 'Contratos de construcción', 4.0000, 700.00, '2020-01-01', true),
    ('037', 'Demás servicios gravados con el IGV', 12.0000, 700.00, '2020-01-01', true)
ON CONFLICT (codigo_bien_servicio) DO UPDATE
SET porcentaje = EXCLUDED.porcentaje,
    monto_minimo = EXCLUDED.monto_minimo,
    descripcion = EXCLUDED.descripcion;

-- rollback DELETE FROM catalogo_detracciones;
-- rollback DELETE FROM catalogo_tipo_documento;
-- rollback DELETE FROM catalogo_tipo_afectacion;
-- rollback DELETE FROM catalogo_tributo_tasas;
-- rollback DELETE FROM catalogo_tributos;
