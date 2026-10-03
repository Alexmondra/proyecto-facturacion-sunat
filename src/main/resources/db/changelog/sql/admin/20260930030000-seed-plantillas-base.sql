-- liquibase formatted sql
-- ============================================================================
-- SEEDER ADMIN: PLANTILLAS DE IMPRESIÓN BASE (TICKET 80MM Y HOJA A4)
-- Formato dinámico y visual basado en zonas, filas, coordenadas milimétricas y elementos
-- ============================================================================

-- changeset alex:006-seed-plantillas-base.sql runOnChange:true
-- comment: Sembrar plantillas base dinámicas y editables (Ticket 80mm y A4) con coordenadas milimetricas y RIPCE SUNAT

-- 1. PLANTILLA BASE TICKET 80MM (COMPACTO, COORDENADAS MILIMÉTRICAS EXACTAS)
INSERT INTO plantillas_base (nombre, tipo_formato, layout_base, activo)
SELECT 
    'Plantilla Estándar Ticket 80mm',
    'TICKET_80',
    '{
      "formato": {
        "tipo": "TICKET_80",
        "ancho_total_mm": 80.0,
        "margen_lateral_mm": 2.0,
        "margen_superior_mm": 3.0,
        "margen_inferior_mm": 3.0
      },
      "zonas": {
        "cabecera": {
          "filas": [
            {
              "id": "fila_logo",
              "margen_inferior_mm": 3.0,
              "elementos": [
                {
                  "tipo": "logo",
                  "pos_x_mm": 24.0,
                  "ancho_mm": 32.0,
                  "alto_max_mm": 18.0
                }
              ]
            },
            {
              "id": "fila_emisor",
              "margen_inferior_mm": 3.0,
              "elementos": [
                {
                  "tipo": "texto_dinamico",
                  "campo": "empresa.nombre_comercial",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 10.5, "negrita": true, "alineacion": "center" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "empresa.razon_social",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 8.0, "alineacion": "center" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "empresa.ruc",
                  "etiqueta": "RUC: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "estilo": { "tamano_pt": 9.0, "negrita": true, "alineacion": "center" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "sucursal.direccion",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 7.5, "alineacion": "center" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "sucursal.telefono",
                  "etiqueta": "TELF: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "estilo": { "tamano_pt": 7.5, "alineacion": "center" }
                }
              ]
            },
            {
              "id": "fila_caja_fiscal",
              "margen_inferior_mm": 4.0,
              "elementos": [
                {
                  "tipo": "recuadro_fiscal",
                  "pos_x_mm": 2.0,
                  "ancho_mm": 72.0,
                  "borde": "solido_1px",
                  "alineacion": "center",
                  "elementos": [
                    {
                      "tipo": "texto_dinamico",
                      "campo": "documento.tipo_comprobante_descripcion",
                      "pos_x_mm": 0.0,
                      "ancho_mm": 72.0,
                      "estilo": { "tamano_pt": 9.5, "negrita": true, "alineacion": "center" }
                    },
                    {
                      "tipo": "texto_dinamico",
                      "campo": "documento.serie_numero",
                      "pos_x_mm": 0.0,
                      "ancho_mm": 72.0,
                      "estilo": { "tamano_pt": 11.0, "negrita": true, "alineacion": "center" }
                    }
                  ]
                }
              ]
            },
            {
              "id": "fila_datos_cliente_y_emision",
              "margen_inferior_mm": 4.0,
              "elementos": [
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.fecha_emision",
                  "etiqueta": "Fecha: ",
                  "formato": "yyyy-MM-dd HH:mm:ss",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "estilo": { "tamano_pt": 8.0 }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.cliente_numero_doc",
                  "etiqueta": "RUC/DNI: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "estilo": { "tamano_pt": 8.0 }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.cliente_nombre",
                  "etiqueta": "Cliente: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 8.5, "negrita": true }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.cliente_direccion",
                  "etiqueta": "Dirección: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 7.5 }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.moneda",
                  "etiqueta": "Moneda: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 38.0,
                  "estilo": { "tamano_pt": 8.0 }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.forma_pago",
                  "etiqueta": "Pago: ",
                  "pos_x_mm": 39.0,
                  "ancho_mm": 37.0,
                  "estilo": { "tamano_pt": 8.0 }
                }
              ]
            }
          ]
        },
        "cuerpo_venta": {
          "margen_inferior_mm": 4.0,
          "columnas": [
            { "clave": "cant", "titulo": "Cant", "pos_x_mm": 0.0, "ancho_mm": 11.0, "alineacion": "center" },
            { "clave": "descripcion", "titulo": "Descripción", "pos_x_mm": 12.0, "ancho_mm": 38.0, "alineacion": "left" },
            { "clave": "precio_unit", "titulo": "P.U.", "pos_x_mm": 51.0, "ancho_mm": 12.0, "alineacion": "right" },
            { "clave": "total", "titulo": "Total", "pos_x_mm": 64.0, "ancho_mm": 12.0, "alineacion": "right" }
          ]
        },
        "pie": {
          "filas": [
            {
              "id": "fila_totales_y_qr",
              "margen_inferior_mm": 4.0,
              "elementos": [
                {
                  "tipo": "codigo_qr",
                  "pos_x_mm": 1.0,
                  "ancho_mm": 27.0,
                  "alto_mm": 27.0
                },
                {
                  "tipo": "bloque_totales",
                  "pos_x_mm": 29.0,
                  "ancho_mm": 47.0,
                  "alineacion": "right",
                  "campos": [
                    { "campo": "documento.total_gravada", "etiqueta": "Op. Gravada: " },
                    { "campo": "documento.total_exonerada", "etiqueta": "Op. Exon: " },
                    { "campo": "documento.total_igv", "etiqueta": "I.G.V. (18%): " },
                    { "campo": "documento.total_icbper", "etiqueta": "ICBPER: " },
                    { "campo": "documento.total", "etiqueta": "TOTAL: ", "estilo": { "tamano_pt": 10.0, "negrita": true } }
                  ]
                }
              ]
            },
            {
              "id": "fila_monto_letras",
              "margen_inferior_mm": 3.0,
              "elementos": [
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.monto_letras",
                  "etiqueta": "SON: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 7.5, "negrita": true }
                }
              ]
            },
            {
              "id": "fila_legal_sunat",
              "margen_inferior_mm": 3.0,
              "elementos": [
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.hash_cpe",
                  "etiqueta": "Hash: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "estilo": { "tamano_pt": 6.5, "alineacion": "center" }
                },
                {
                  "tipo": "texto_fijo",
                  "contenido": "Representación Impresa del Comprobante Electrónico. Consulte en www.sunat.gob.pe",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 6.5, "alineacion": "center" }
                }
              ]
            },
            {
              "id": "fila_agradecimiento",
              "elementos": [
                {
                  "tipo": "texto_fijo",
                  "contenido": "¡Muchas gracias por su preferencia!",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 76.0,
                  "estilo": { "tamano_pt": 8.0, "negrita": true, "alineacion": "center" }
                }
              ]
            }
          ]
        }
      }
    }'::jsonb,
    true
WHERE NOT EXISTS (SELECT 1 FROM plantillas_base WHERE tipo_formato = 'TICKET_80');

-- 2. PLANTILLA BASE HOJA A4 (MULTIPÁGINA, COORDENADAS MILIMÉTRICAS A 190MM ÚTILES)
INSERT INTO plantillas_base (nombre, tipo_formato, layout_base, activo)
SELECT 
    'Plantilla Estándar Hoja A4',
    'A4',
    '{
      "formato": {
        "tipo": "A4",
        "ancho_total_mm": 210.0,
        "alto_total_mm": 297.0,
        "margen_superior_mm": 10.0,
        "margen_inferior_mm": 10.0,
        "margen_lateral_mm": 10.0,
        "fuente_familia": "Arial, Helvetica, sans-serif"
      },
      "zonas": {
        "cabecera": {
          "repetir_en_cada_pagina": false,
          "filas": [
            {
              "id": "fila_superior_empresa_ruc",
              "margen_inferior_mm": 5.0,
              "elementos": [
                {
                  "tipo": "logo",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 45.0,
                  "alto_max_mm": 25.0
                },
                {
                  "tipo": "datos_empresa",
                  "pos_x_mm": 48.0,
                  "ancho_mm": 80.0,
                  "estilo": { "tamano_pt": 8.5, "color": "#333333" }
                },
                {
                  "tipo": "caja_ruc_factura",
                  "pos_x_mm": 132.0,
                  "ancho_mm": 58.0,
                  "estilo": {
                    "tamano_pt": 11.0,
                    "negrita": true,
                    "color": "#0d47a1",
                    "borde_grosor_px": 1.5,
                    "borde_color": "#0d47a1",
                    "alineacion": "center"
                  }
                }
              ]
            },
            {
              "id": "fila_datos_cliente",
              "margen_inferior_mm": 6.0,
              "elementos": [
                {
                  "tipo": "bloque_cliente",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 190.0,
                  "estilo": {
                    "tamano_pt": 8.5,
                    "borde_color": "#e0e0e0",
                    "borde_grosor_px": 1.0
                  },
                  "propiedades": {
                    "mostrar_direccion": true,
                    "mostrar_forma_pago": true,
                    "mostrar_guia_remision": true
                  }
                }
              ]
            }
          ]
        },
        "cuerpo_venta": {
          "ancho_mm": 190.0,
          "margen_inferior_mm": 6.0,
          "repetir_encabezado_tabla": true,
          "estilo": {
            "tamano_encabezado_pt": 8.5,
            "tamano_filas_pt": 8.0,
            "fondo_encabezado": "#0d47a1",
            "color_encabezado": "#ffffff",
            "color_filas": "#212121",
            "borde_color": "#e0e0e0"
          },
          "columnas": [
            { "clave": "item", "titulo": "Ítem", "ancho_mm": 12.0, "alineacion": "center" },
            { "clave": "cantidad", "titulo": "Cant.", "ancho_mm": 16.0, "alineacion": "center" },
            { "clave": "unidad_medida", "titulo": "Unidad", "ancho_mm": 16.0, "alineacion": "center" },
            { "clave": "descripcion", "titulo": "Descripción del Bien o Servicio", "ancho_mm": 88.0, "alineacion": "left" },
            { "clave": "valor_unitario", "titulo": "V. Unit", "ancho_mm": 28.0, "alineacion": "right" },
            { "clave": "total", "titulo": "Importe", "ancho_mm": 30.0, "alineacion": "right" }
          ]
        },
        "pie": {
          "solo_en_ultima_pagina": true,
          "filas": [
            {
              "id": "fila_totales_y_pagos",
              "margen_inferior_mm": 4.0,
              "elementos": [
                {
                  "tipo": "bloque_cuentas_y_qr",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 115.0,
                  "propiedades": {
                    "qr_tamano_mm": 26.0,
                    "mostrar_cuentas_bancarias": true,
                    "mostrar_monto_letras": true
                  }
                },
                {
                  "tipo": "totales_fiscales",
                  "pos_x_mm": 120.0,
                  "ancho_mm": 70.0,
                  "estilo": {
                    "tamano_pt": 9.0,
                    "negrita": true,
                    "borde_color": "#dcdcdc",
                    "borde_grosor_px": 1.0,
                    "alineacion": "right"
                  }
                }
              ]
            },
            {
              "id": "fila_leyenda_legal",
              "elementos": [
                {
                  "tipo": "texto_fijo",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 190.0,
                  "contenido": "Representación impresa de la Factura Electrónica. Consulte su comprobante en nuestro portal.",
                  "estilo": {
                    "tamano_pt": 7.0,
                    "color": "#666666",
                    "alineacion": "center"
                  }
                }
              ]
            }
          ]
        }
      }
    }'::jsonb,
    true
WHERE NOT EXISTS (SELECT 1 FROM plantillas_base WHERE tipo_formato = 'A4');

-- Actualizar layout A4 existente en caso de reejecución
UPDATE plantillas_base
SET layout_base = '{
  "formato": {
    "tipo": "A4",
    "ancho_total_mm": 210.0,
    "alto_total_mm": 297.0,
    "margen_superior_mm": 10.0,
    "margen_inferior_mm": 10.0,
    "margen_lateral_mm": 10.0,
    "fuente_familia": "Arial, Helvetica, sans-serif"
  },
  "zonas": {
    "cabecera": {
      "repetir_en_cada_pagina": false,
      "filas": [
        {
          "id": "fila_superior_empresa_ruc",
          "margen_inferior_mm": 5.0,
          "elementos": [
            {
              "tipo": "logo",
              "pos_x_mm": 0.0,
              "ancho_mm": 45.0,
              "alto_max_mm": 25.0
            },
            {
              "tipo": "datos_empresa",
              "pos_x_mm": 48.0,
              "ancho_mm": 80.0,
              "estilo": { "tamano_pt": 8.5, "color": "#333333" }
            },
            {
              "tipo": "caja_ruc_factura",
              "pos_x_mm": 132.0,
              "ancho_mm": 58.0,
              "estilo": {
                "tamano_pt": 11.0,
                "negrita": true,
                "color": "#0d47a1",
                "borde_grosor_px": 1.5,
                "borde_color": "#0d47a1",
                "alineacion": "center"
              }
            }
          ]
        },
        {
          "id": "fila_datos_cliente",
          "margen_inferior_mm": 6.0,
          "elementos": [
            {
              "tipo": "bloque_cliente",
              "pos_x_mm": 0.0,
              "ancho_mm": 190.0,
              "estilo": {
                "tamano_pt": 8.5,
                "borde_color": "#e0e0e0",
                "borde_grosor_px": 1.0
              },
              "propiedades": {
                "mostrar_direccion": true,
                "mostrar_forma_pago": true,
                "mostrar_guia_remision": true
              }
            }
          ]
        }
      ]
    },
    "cuerpo_venta": {
      "ancho_mm": 190.0,
      "margen_inferior_mm": 6.0,
      "repetir_encabezado_tabla": true,
      "estilo": {
        "tamano_encabezado_pt": 8.5,
        "tamano_filas_pt": 8.0,
        "fondo_encabezado": "#0d47a1",
        "color_encabezado": "#ffffff",
        "color_filas": "#212121",
        "borde_color": "#e0e0e0"
      },
      "columnas": [
        { "clave": "item", "titulo": "Ítem", "ancho_mm": 12.0, "alineacion": "center" },
        { "clave": "cantidad", "titulo": "Cant.", "ancho_mm": 16.0, "alineacion": "center" },
        { "clave": "unidad_medida", "titulo": "Unidad", "ancho_mm": 16.0, "alineacion": "center" },
        { "clave": "descripcion", "titulo": "Descripción del Bien o Servicio", "ancho_mm": 88.0, "alineacion": "left" },
        { "clave": "valor_unitario", "titulo": "V. Unit", "ancho_mm": 28.0, "alineacion": "right" },
        { "clave": "total", "titulo": "Importe", "ancho_mm": 30.0, "alineacion": "right" }
      ]
    },
    "pie": {
      "solo_en_ultima_pagina": true,
      "filas": [
        {
          "id": "fila_totales_y_pagos",
          "margen_inferior_mm": 4.0,
          "elementos": [
            {
              "tipo": "bloque_cuentas_y_qr",
              "pos_x_mm": 0.0,
              "ancho_mm": 115.0,
              "propiedades": {
                "qr_tamano_mm": 26.0,
                "mostrar_cuentas_bancarias": true,
                "mostrar_monto_letras": true
              }
            },
            {
              "tipo": "totales_fiscales",
              "pos_x_mm": 120.0,
              "ancho_mm": 70.0,
              "estilo": {
                "tamano_pt": 9.0,
                "negrita": true,
                "borde_color": "#dcdcdc",
                "borde_grosor_px": 1.0,
                "alineacion": "right"
              }
            }
          ]
        },
        {
          "id": "fila_leyenda_legal",
          "elementos": [
            {
              "tipo": "texto_fijo",
              "pos_x_mm": 0.0,
              "ancho_mm": 190.0,
              "contenido": "Representación impresa de la Factura Electrónica. Consulte su comprobante en nuestro portal.",
              "estilo": {
                "tamano_pt": 7.0,
                "color": "#666666",
                "alineacion": "center"
              }
            }
          ]
        }
      ]
    }
  }
}'::jsonb
WHERE tipo_formato = 'A4';

-- 3. PLANTILLA BASE TICKET 80MM COMPACTO (AHORRO MÁXIMO DE PAPEL TÉRMICO)
INSERT INTO plantillas_base (nombre, tipo_formato, layout_base, activo)
SELECT 
    'Plantilla Ticket 80mm Compacto',
    'TICKET_80_COMPACTO',
    '{
      "formato": {
        "tipo": "TICKET_80_COMPACTO",
        "ancho_total_mm": 80.0,
        "margen_lateral_mm": 1.5,
        "margen_superior_mm": 2.0,
        "margen_inferior_mm": 2.0,
        "fuente_familia": "monospace, Arial, sans-serif"
      },
      "zonas": {
        "cabecera": {
          "filas": [
            {
              "id": "fila_logo_compacto",
              "margen_inferior_mm": 2.0,
              "elementos": [
                {
                  "tipo": "logo",
                  "pos_x_mm": 26.5,
                  "ancho_mm": 24.0,
                  "alto_max_mm": 12.0
                }
              ]
            },
            {
              "id": "fila_emisor_compacto",
              "margen_inferior_mm": 2.0,
              "elementos": [
                {
                  "tipo": "texto_dinamico",
                  "campo": "empresa.nombre_comercial",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 9.5, "negrita": true, "alineacion": "center" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "empresa.razon_social",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 7.5, "alineacion": "center" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "empresa.ruc",
                  "etiqueta": "RUC: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "estilo": { "tamano_pt": 8.5, "negrita": true, "alineacion": "center" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "sucursal.direccion",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 7.0, "alineacion": "center" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "sucursal.telefono",
                  "etiqueta": "TEL: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "estilo": { "tamano_pt": 7.0, "alineacion": "center" }
                }
              ]
            },
            {
              "id": "fila_caja_fiscal_compacta",
              "margen_inferior_mm": 2.5,
              "elementos": [
                {
                  "tipo": "recuadro_fiscal",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "borde": "punteado_1px",
                  "alineacion": "center",
                  "elementos": [
                    {
                      "tipo": "texto_dinamico",
                      "campo": "documento.tipo_comprobante_descripcion",
                      "pos_x_mm": 0.0,
                      "ancho_mm": 77.0,
                      "estilo": { "tamano_pt": 9.0, "negrita": true, "alineacion": "center" }
                    },
                    {
                      "tipo": "texto_dinamico",
                      "campo": "documento.serie_numero",
                      "pos_x_mm": 0.0,
                      "ancho_mm": 77.0,
                      "estilo": { "tamano_pt": 10.0, "negrita": true, "alineacion": "center" }
                    }
                  ]
                }
              ]
            },
            {
              "id": "fila_datos_cliente_compacto",
              "margen_inferior_mm": 2.5,
              "elementos": [
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.fecha_emision",
                  "etiqueta": "Fecha: ",
                  "formato": "yyyy-MM-dd HH:mm",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 45.0,
                  "estilo": { "tamano_pt": 7.5 }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.moneda",
                  "etiqueta": "Moneda: ",
                  "pos_x_mm": 46.0,
                  "ancho_mm": 31.0,
                  "estilo": { "tamano_pt": 7.5, "alineacion": "right" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.cliente_numero_doc",
                  "etiqueta": "Doc: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "estilo": { "tamano_pt": 7.5 }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.cliente_nombre",
                  "etiqueta": "Cliente: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 7.5, "negrita": true }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.forma_pago",
                  "etiqueta": "Condición: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "estilo": { "tamano_pt": 7.0 }
                }
              ]
            }
          ]
        },
        "cuerpo_venta": {
          "ancho_mm": 77.0,
          "margen_inferior_mm": 2.5,
          "estilo": {
            "tamano_encabezado_pt": 7.0,
            "tamano_filas_pt": 7.0,
            "linea_separadora": "punteada"
          },
          "columnas": [
            { "clave": "cant", "titulo": "Cant", "pos_x_mm": 0.0, "ancho_mm": 9.0, "alineacion": "center" },
            { "clave": "descripcion", "titulo": "Descripción", "pos_x_mm": 9.0, "ancho_mm": 44.0, "alineacion": "left" },
            { "clave": "precio_unit", "titulo": "P.U.", "pos_x_mm": 53.0, "ancho_mm": 12.0, "alineacion": "right" },
            { "clave": "total", "titulo": "Total", "pos_x_mm": 65.0, "ancho_mm": 12.0, "alineacion": "right" }
          ]
        },
        "pie": {
          "filas": [
            {
              "id": "fila_totales_y_qr_compacto",
              "margen_inferior_mm": 2.0,
              "elementos": [
                {
                  "tipo": "codigo_qr",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 22.0,
                  "alto_mm": 22.0
                },
                {
                  "tipo": "bloque_totales",
                  "pos_x_mm": 24.0,
                  "ancho_mm": 53.0,
                  "alineacion": "right",
                  "campos": [
                    { "campo": "documento.total_gravada", "etiqueta": "Op. Gravada: ", "estilo": { "tamano_pt": 7.0 } },
                    { "campo": "documento.total_exonerada", "etiqueta": "Op. Exon: ", "estilo": { "tamano_pt": 7.0 } },
                    { "campo": "documento.total_igv", "etiqueta": "IGV (18%): ", "estilo": { "tamano_pt": 7.0 } },
                    { "campo": "documento.total_icbper", "etiqueta": "ICBPER: ", "estilo": { "tamano_pt": 7.0 } },
                    { "campo": "documento.total", "etiqueta": "TOTAL: ", "estilo": { "tamano_pt": 9.0, "negrita": true } }
                  ]
                }
              ]
            },
            {
              "id": "fila_monto_letras_compacto",
              "margen_inferior_mm": 2.0,
              "elementos": [
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.monto_letras",
                  "etiqueta": "SON: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 7.0, "negrita": true }
                }
              ]
            },
            {
              "id": "fila_legal_sunat_compacto",
              "margen_inferior_mm": 2.0,
              "elementos": [
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.hash_cpe",
                  "etiqueta": "Hash: ",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "estilo": { "tamano_pt": 6.0, "alineacion": "center" }
                },
                {
                  "tipo": "texto_fijo",
                  "contenido": "Representación Impresa del CPE. Consulte en www.sunat.gob.pe",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "ajuste_texto": "wrap",
                  "estilo": { "tamano_pt": 6.0, "alineacion": "center" }
                }
              ]
            },
            {
              "id": "fila_agradecimiento_compacto",
              "elementos": [
                {
                  "tipo": "texto_fijo",
                  "contenido": "¡Gracias por su compra!",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "estilo": { "tamano_pt": 7.5, "negrita": true, "alineacion": "center" }
                }
              ]
            }
          ]
        }
      }
    }'::jsonb,
    true
WHERE NOT EXISTS (SELECT 1 FROM plantillas_base WHERE tipo_formato = 'TICKET_80_COMPACTO');

-- Actualizar layout TICKET_80_COMPACTO existente en caso de reejecución
UPDATE plantillas_base
SET layout_base = '{
  "formato": {
    "tipo": "TICKET_80_COMPACTO",
    "ancho_total_mm": 80.0,
    "margen_lateral_mm": 1.5,
    "margen_superior_mm": 2.0,
    "margen_inferior_mm": 2.0,
    "fuente_familia": "monospace, Arial, sans-serif"
  },
  "zonas": {
    "cabecera": {
      "filas": [
        {
          "id": "fila_logo_compacto",
          "margen_inferior_mm": 2.0,
          "elementos": [
            {
              "tipo": "logo",
              "pos_x_mm": 26.5,
              "ancho_mm": 24.0,
              "alto_max_mm": 12.0
            }
          ]
        },
        {
          "id": "fila_emisor_compacto",
          "margen_inferior_mm": 2.0,
          "elementos": [
            {
              "tipo": "texto_dinamico",
              "campo": "empresa.nombre_comercial",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "ajuste_texto": "wrap",
              "estilo": { "tamano_pt": 9.5, "negrita": true, "alineacion": "center" }
            },
            {
              "tipo": "texto_dinamico",
              "campo": "empresa.razon_social",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "ajuste_texto": "wrap",
              "estilo": { "tamano_pt": 7.5, "alineacion": "center" }
            },
            {
              "tipo": "texto_dinamico",
              "campo": "empresa.ruc",
              "etiqueta": "RUC: ",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "estilo": { "tamano_pt": 8.5, "negrita": true, "alineacion": "center" }
            },
            {
              "tipo": "texto_dinamico",
              "campo": "sucursal.direccion",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "ajuste_texto": "wrap",
              "estilo": { "tamano_pt": 7.0, "alineacion": "center" }
            },
            {
              "tipo": "texto_dinamico",
              "campo": "sucursal.telefono",
              "etiqueta": "TEL: ",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "estilo": { "tamano_pt": 7.0, "alineacion": "center" }
            }
          ]
        },
        {
          "id": "fila_caja_fiscal_compacta",
          "margen_inferior_mm": 2.5,
          "elementos": [
            {
              "tipo": "recuadro_fiscal",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "borde": "punteado_1px",
              "alineacion": "center",
              "elementos": [
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.tipo_comprobante_descripcion",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "estilo": { "tamano_pt": 9.0, "negrita": true, "alineacion": "center" }
                },
                {
                  "tipo": "texto_dinamico",
                  "campo": "documento.serie_numero",
                  "pos_x_mm": 0.0,
                  "ancho_mm": 77.0,
                  "estilo": { "tamano_pt": 10.0, "negrita": true, "alineacion": "center" }
                }
              ]
            }
          ]
        },
        {
          "id": "fila_datos_cliente_compacto",
          "margen_inferior_mm": 2.5,
          "elementos": [
            {
              "tipo": "texto_dinamico",
              "campo": "documento.fecha_emision",
              "etiqueta": "Fecha: ",
              "formato": "yyyy-MM-dd HH:mm",
              "pos_x_mm": 0.0,
              "ancho_mm": 45.0,
              "estilo": { "tamano_pt": 7.5 }
            },
            {
              "tipo": "texto_dinamico",
              "campo": "documento.moneda",
              "etiqueta": "Moneda: ",
              "pos_x_mm": 46.0,
              "ancho_mm": 31.0,
              "estilo": { "tamano_pt": 7.5, "alineacion": "right" }
            },
            {
              "tipo": "texto_dinamico",
              "campo": "documento.cliente_numero_doc",
              "etiqueta": "Doc: ",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "estilo": { "tamano_pt": 7.5 }
            },
            {
              "tipo": "texto_dinamico",
              "campo": "documento.cliente_nombre",
              "etiqueta": "Cliente: ",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "ajuste_texto": "wrap",
              "estilo": { "tamano_pt": 7.5, "negrita": true }
            },
            {
              "tipo": "texto_dinamico",
              "campo": "documento.forma_pago",
              "etiqueta": "Condición: ",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "estilo": { "tamano_pt": 7.0 }
            }
          ]
        }
      ]
    },
    "cuerpo_venta": {
      "ancho_mm": 77.0,
      "margen_inferior_mm": 2.5,
      "estilo": {
        "tamano_encabezado_pt": 7.0,
        "tamano_filas_pt": 7.0,
        "linea_separadora": "punteada"
      },
      "columnas": [
        { "clave": "cant", "titulo": "Cant", "pos_x_mm": 0.0, "ancho_mm": 9.0, "alineacion": "center" },
        { "clave": "descripcion", "titulo": "Descripción", "pos_x_mm": 9.0, "ancho_mm": 44.0, "alineacion": "left" },
        { "clave": "precio_unit", "titulo": "P.U.", "pos_x_mm": 53.0, "ancho_mm": 12.0, "alineacion": "right" },
        { "clave": "total", "titulo": "Total", "pos_x_mm": 65.0, "ancho_mm": 12.0, "alineacion": "right" }
      ]
    },
    "pie": {
      "filas": [
        {
          "id": "fila_totales_y_qr_compacto",
          "margen_inferior_mm": 2.0,
          "elementos": [
            {
              "tipo": "codigo_qr",
              "pos_x_mm": 0.0,
              "ancho_mm": 22.0,
              "alto_mm": 22.0
            },
            {
              "tipo": "bloque_totales",
              "pos_x_mm": 24.0,
              "ancho_mm": 53.0,
              "alineacion": "right",
              "campos": [
                { "campo": "documento.total_gravada", "etiqueta": "Op. Gravada: ", "estilo": { "tamano_pt": 7.0 } },
                { "campo": "documento.total_exonerada", "etiqueta": "Op. Exon: ", "estilo": { "tamano_pt": 7.0 } },
                { "campo": "documento.total_igv", "etiqueta": "IGV (18%): ", "estilo": { "tamano_pt": 7.0 } },
                { "campo": "documento.total_icbper", "etiqueta": "ICBPER: ", "estilo": { "tamano_pt": 7.0 } },
                { "campo": "documento.total", "etiqueta": "TOTAL: ", "estilo": { "tamano_pt": 9.0, "negrita": true } }
              ]
            }
          ]
        },
        {
          "id": "fila_monto_letras_compacto",
          "margen_inferior_mm": 2.0,
          "elementos": [
            {
              "tipo": "texto_dinamico",
              "campo": "documento.monto_letras",
              "etiqueta": "SON: ",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "ajuste_texto": "wrap",
              "estilo": { "tamano_pt": 7.0, "negrita": true }
            }
          ]
        },
        {
          "id": "fila_legal_sunat_compacto",
          "margen_inferior_mm": 2.0,
          "elementos": [
            {
              "tipo": "texto_dinamico",
              "campo": "documento.hash_cpe",
              "etiqueta": "Hash: ",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "estilo": { "tamano_pt": 6.0, "alineacion": "center" }
            },
            {
              "tipo": "texto_fijo",
              "contenido": "Representación Impresa del CPE. Consulte en www.sunat.gob.pe",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "ajuste_texto": "wrap",
              "estilo": { "tamano_pt": 6.0, "alineacion": "center" }
            }
          ]
        },
        {
          "id": "fila_agradecimiento_compacto",
          "elementos": [
            {
              "tipo": "texto_fijo",
              "contenido": "¡Gracias por su compra!",
              "pos_x_mm": 0.0,
              "ancho_mm": 77.0,
              "estilo": { "tamano_pt": 7.5, "negrita": true, "alineacion": "center" }
            }
          ]
        }
      ]
    }
  }
}'::jsonb
WHERE tipo_formato = 'TICKET_80_COMPACTO';

