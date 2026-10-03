package com.s1nt4xSystem.facturacion_sunat.shared.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Catálogo unificado de códigos de error de negocio de la plataforma.
 * Rangos numéricos por módulo:
 * - 19000..19999: Company (Empresas, Configuración Fiscal, Certificados)
 * - 18000..18999: Branch (Sucursales)
 * - 17000..17999: Series (Series y Correlativos)
 * - 16000..16999: Document (Comprobantes, Ítems, Tributos, Clientes)
 * - 15000..15999: SUNAT / Integración Externa
 */
@Getter
public enum ErrorCode {

    // =========================================================================
    // 19000 - 19999: COMPANY & CONFIGURACIÓN FISCAL
    // =========================================================================
    COMPANY_NOT_FOUND(19001, HttpStatus.NOT_FOUND, "La empresa con RUC '%s' no existe"),
    COMPANY_RUC_ALREADY_EXISTS(19002, HttpStatus.CONFLICT, "El RUC '%s' ya se encuentra registrado en el sistema"),
    COMPANY_INVALID_RUC(19003, HttpStatus.BAD_REQUEST, "El RUC '%s' es inválido. Debe tener 11 dígitos numéricos y comenzar con 10 o 20"),
    COMPANY_RAZON_SOCIAL_REQUIRED(19004, HttpStatus.BAD_REQUEST, "La razón social de la empresa es obligatoria y no puede estar vacía"),
    COMPANY_CERTIFICATE_NOT_FOUND(19005, HttpStatus.BAD_REQUEST, "Falta certificado para firmar el comprobante"),
    COMPANY_SOL_CREDENTIALS_MISSING(19006, HttpStatus.BAD_REQUEST, "Faltan datos de configuración de la empresa"),
    COMPANY_CERTIFICATE_PASSWORD_MISSING(19007, HttpStatus.BAD_REQUEST, "La contraseña del certificado digital es obligatoria para archivos PFX/P12"),
    COMPANY_INVALID_CERTIFICATE_FILE(19008, HttpStatus.BAD_REQUEST, "El archivo del certificado digital debe tener extensión .pfx o .p12"),
    COMPANY_INVALID_MODO_EMISION(19009, HttpStatus.BAD_REQUEST, "El modo de emisión '%s' es inválido. Valores permitidos: PROPIO, PSE"),
    COMPANY_PSE_REQUIRED(19010, HttpStatus.BAD_REQUEST, "Debe especificar el id_pse cuando el modo de emisión es PSE"),
    COMPANY_CERTIFICATE_LOAD_ERROR(19011, HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar o procesar el certificado digital: %s"),
    COMPANY_CONFIG_NOT_FOUND(19012, HttpStatus.NOT_FOUND, "No existe configuración fiscal registrada para la empresa"),
    COMPANY_CERTIFICATE_FILE_REQUIRED(19013, HttpStatus.BAD_REQUEST, "El archivo de certificado digital es obligatorio"),
    COMPANY_CERTIFICATE_CONTENT_EMPTY(19014, HttpStatus.BAD_REQUEST, "El contenido del certificado digital está vacío"),
    COMPANY_CERTIFICATE_NO_PRIVATE_KEY(19015, HttpStatus.BAD_REQUEST, "El certificado no contiene una clave privada asociada requerida para la firma de comprobantes SUNAT"),
    COMPANY_CERTIFICATE_ENCRYPTED_PEM_PASSWORD_REQUIRED(19016, HttpStatus.BAD_REQUEST, "El archivo PEM contiene una clave privada cifrada. Debe ingresar la contraseña para descifrarla."),
    COMPANY_CERTIFICATE_STORAGE_PATH_REQUIRED(19017, HttpStatus.BAD_REQUEST, "No se ha configurado ninguna ruta de certificado digital"),
    COMPANY_CERTIFICATE_FILE_NOT_FOUND_ON_DISK(19018, HttpStatus.NOT_FOUND, "El archivo físico del certificado no se encuentra en el servidor: %s"),
    COMPANY_ENVIRONMENT_REQUIRED(19019, HttpStatus.BAD_REQUEST, "El entorno no puede estar vacío"),

    // =========================================================================
    // 18000 - 18999: SUCURSALES (BRANCH)
    // =========================================================================
    BRANCH_NOT_FOUND(18001, HttpStatus.NOT_FOUND, "La sucursal solicitada no existe"),
    BRANCH_CODE_ALREADY_EXISTS(18002, HttpStatus.CONFLICT, "Ya existe una sucursal con el código: %s"),
    BRANCH_CANNOT_DISABLE_MAIN(18003, HttpStatus.BAD_REQUEST, "No se puede desactivar la sucursal principal '0000'"),
    BRANCH_INACTIVE(18004, HttpStatus.BAD_REQUEST, "La sucursal '%s' se encuentra inactiva"),
    BRANCH_INVALID_CODE(18005, HttpStatus.BAD_REQUEST, "El código de sucursal debe tener exactamente 4 dígitos"),
    BRANCH_CODE_IN_USE(18006, HttpStatus.CONFLICT, "El código %s ya está en uso"),
    BRANCH_NOT_OWNED(18007, HttpStatus.FORBIDDEN, "La sucursal no pertenece a la empresa actual"),
    BRANCH_UBIGEO_NOT_FOUND(18008, HttpStatus.BAD_REQUEST, "El código de ubigeo '%s' no existe en el catálogo oficial"),
    BRANCH_CANNOT_DELETE_MAIN(18009, HttpStatus.BAD_REQUEST, "No se puede eliminar la sucursal principal '0000'"),

    // =========================================================================
    // 17000 - 17999: SERIES Y CORRELATIVOS
    // =========================================================================
    SERIE_INVALID_FORMAT(17001, HttpStatus.BAD_REQUEST, "La serie '%s' no tiene un formato válido para %s. Debe cumplir con el patrón: %s"),
    SERIE_ALREADY_EXISTS(17002, HttpStatus.CONFLICT, "La serie '%s' para el tipo '%s' ya está registrada en la empresa (las series no pueden repetirse entre sucursales)"),
    SERIE_NOT_FOUND(17003, HttpStatus.NOT_FOUND, "La serie '%s' no está registrada para esta sucursal"),
    SERIE_BRANCH_NOT_OWNED(17004, HttpStatus.FORBIDDEN, "La sucursal indicada no pertenece a la empresa actual"),
    SERIE_NOT_OWNED(17005, HttpStatus.FORBIDDEN, "La serie solicitada no pertenece a la empresa actual"),
    SERIE_CANNOT_CHANGE_BRANCH_WITH_EMISSIONS(17006, HttpStatus.BAD_REQUEST, "No se puede cambiar la sucursal de una serie que ya tiene comprobantes emitidos"),
    SERIE_CANNOT_CHANGE_CODE_WITH_EMISSIONS(17007, HttpStatus.BAD_REQUEST, "No se puede cambiar el código o tipo de comprobante de una serie que ya tiene comprobantes emitidos"),
    SERIE_CORRELATIVO_NEGATIVE(17008, HttpStatus.BAD_REQUEST, "El correlativo no puede ser negativo"),
    SERIE_CANNOT_DELETE_WITH_EMISSIONS(17009, HttpStatus.BAD_REQUEST, "No se puede eliminar una serie que ya ha emitido comprobantes (correlativo > 0)"),

    // =========================================================================
    // 16000 - 16999: COMPROBANTE Y DOCUMENTOS (DOCUMENT)
    // =========================================================================
    DOCUMENT_ITEMS_EMPTY(16001, HttpStatus.BAD_REQUEST, "El comprobante debe contener al menos un ítem o producto"),
    DOCUMENT_CLIENT_INVALID(16002, HttpStatus.BAD_REQUEST, "Datos del cliente inválidos o incompletos"),
    DOCUMENT_ALREADY_EXISTS(16003, HttpStatus.CONFLICT, "El comprobante %s-%s-%s ya fue emitido previamente"),
    DOCUMENT_NOT_FOUND(16004, HttpStatus.NOT_FOUND, "No se encontró el comprobante solicitado"),
    DOCUMENT_NOT_FOUND_BY_ID(16005, HttpStatus.NOT_FOUND, "No se encontró ningún comprobante con ID: %s"),
    DOCUMENT_NOT_FOUND_BY_IDEMPOTENCY(16006, HttpStatus.NOT_FOUND, "No se encontró ningún comprobante con clave de idempotencia: %s"),
    DOCUMENT_TYPE_REQUIRED(16007, HttpStatus.BAD_REQUEST, "El tipo de comprobante es obligatorio"),
    DOCUMENT_TYPE_INVALID(16008, HttpStatus.BAD_REQUEST, "Tipo de comprobante no soportado o inválido: %s"),
    DOCUMENT_SERIE_REQUIRED(16009, HttpStatus.BAD_REQUEST, "La serie del comprobante es obligatoria"),
    DOCUMENT_SERIE_INVALID_FORMAT(16010, HttpStatus.BAD_REQUEST, "La serie '%s' no tiene un formato válido para %s. Debe cumplir con el patrón: %s"),
    DOCUMENT_SERIE_NOT_CONFIGURED_SUCURSAL(16011, HttpStatus.BAD_REQUEST, "La serie '%s' para tipo de comprobante '%s' no está configurada en la sucursal indicada"),
    DOCUMENT_SERIE_NOT_CONFIGURED_EMPRESA(16012, HttpStatus.BAD_REQUEST, "La serie '%s' para tipo de comprobante '%s' no está configurada en la empresa"),
    DOCUMENT_SERIE_NOT_FOUND_FOR_TYPE(16013, HttpStatus.BAD_REQUEST, "No existe ninguna serie activa configurada para el tipo de comprobante '%s'"),
    DOCUMENT_SUCURSAL_NOT_FOUND(16014, HttpStatus.NOT_FOUND, "La sucursal indicada no existe"),
    DOCUMENT_SUCURSAL_ACTIVE_NOT_FOUND(16015, HttpStatus.NOT_FOUND, "No se encontró sucursal activa con código: %s"),
    DOCUMENT_EMISOR_RUC_MISMATCH(16016, HttpStatus.BAD_REQUEST, "El RUC emisor enviado (%s) no coincide con el RUC de la empresa (%s)"),
    DOCUMENT_NOTA_TYPE_INVALID(16017, HttpStatus.BAD_REQUEST, "El tipo de nota debe ser 07 (Nota de Crédito) u 08 (Nota de Débito)"),
    DOCUMENT_REF_NOT_FOUND(16018, HttpStatus.NOT_FOUND, "No se encontró el documento original con ID: %s"),
    DOCUMENT_REF_REQUIRED(16019, HttpStatus.BAD_REQUEST, "Las Notas de Crédito y Débito deben incluir la referencia al comprobante que modifican"),
    DOCUMENT_REF_TYPE_REQUIRED(16020, HttpStatus.BAD_REQUEST, "El tipo de documento referenciado es obligatorio"),
    DOCUMENT_REF_SERIE_REQUIRED(16021, HttpStatus.BAD_REQUEST, "La serie del documento referenciado es obligatoria"),
    DOCUMENT_REF_NUMERO_REQUIRED(16022, HttpStatus.BAD_REQUEST, "El número del documento referenciado debe ser mayor a 0"),
    DOCUMENT_REF_MOTIVO_REQUIRED(16023, HttpStatus.BAD_REQUEST, "El código del motivo de emisión según catálogo SUNAT es obligatorio"),
    DOCUMENT_ITEM_PRICE_REQUIRED(16024, HttpStatus.BAD_REQUEST, "Debe proporcionar el precio para el ítem: %s"),
    DOCUMENT_BOLSA_PRICE_BELOW_ICBPER(16025, HttpStatus.BAD_REQUEST, "Para bolsas plásticas con tributos incluidos (afectación %s), el precio unitario (S/ %s) debe ser mayor a la tasa ICBPER (S/ %s)"),
    DOCUMENT_FACTURA_RUC_REQUIRED(16026, HttpStatus.BAD_REQUEST, "Para emitir una Factura (01), el tipo de documento del cliente debe ser RUC (tipo '6')"),
    DOCUMENT_FACTURA_RUC_INVALID(16027, HttpStatus.BAD_REQUEST, "El RUC del cliente debe tener exactamente 11 dígitos numéricos"),
    DOCUMENT_FACTURA_SERIE_INVALID(16028, HttpStatus.BAD_REQUEST, "La serie de una Factura electrónica debe comenzar con 'F' (ej. F001)"),
    DOCUMENT_BOLETA_SERIE_INVALID(16029, HttpStatus.BAD_REQUEST, "La serie de una Boleta de venta electrónica debe comenzar con 'B' (ej. B001)"),
    DOCUMENT_BOLETA_CLIENT_ID_REQUIRED(16030, HttpStatus.BAD_REQUEST, "Para boletas de venta iguales o mayores a S/ 700.00 es obligatoria la identificación del cliente (DNI, Carnet de extranjería o Pasaporte)"),
    DOCUMENT_CREDITO_CUOTAS_REQUIRED(16031, HttpStatus.BAD_REQUEST, "Para operaciones con forma de pago al CRÉDITO debe especificarse al menos una cuota de pago"),
    DOCUMENT_CREDITO_CUOTAS_MISMATCH(16032, HttpStatus.BAD_REQUEST, "La suma de las cuotas (%s) no coincide con el monto neto pendiente a crédito (%s)"),
    DOCUMENT_DETRACCION_CODE_REQUIRED(16033, HttpStatus.BAD_REQUEST, "El código de bien/servicio sujeto a detracción es obligatorio (Catálogo 54)"),
    DOCUMENT_DETRACCION_PERCENT_INVALID(16034, HttpStatus.BAD_REQUEST, "El porcentaje de detracción debe ser mayor a 0"),
    DOCUMENT_DETRACCION_ACCOUNT_REQUIRED(16035, HttpStatus.BAD_REQUEST, "El número de cuenta de detracción del Banco de la Nación es obligatorio"),
    DOCUMENT_XML_EMPTY(16036, HttpStatus.BAD_REQUEST, "El contenido XML a firmar no puede estar vacío"),
    DOCUMENT_XML_SIGN_NO_KEY(16037, HttpStatus.BAD_REQUEST, "No se encontró una clave privada y certificado válidos en el KeyStore"),
    DOCUMENT_XML_SIGN_NO_EXTENSION(16038, HttpStatus.BAD_REQUEST, "El XML UBL 2.1 no contiene el nodo requerido ext:ExtensionContent para la firma digital"),
    DOCUMENT_CLIENT_NAME_REQUIRED(16039, HttpStatus.BAD_REQUEST, "No se pudo verificar el documento '%s' y no se proporcionó el nombre o razón social del cliente. Por favor, ingréselo manualmente."),
    DOCUMENT_PRINT_ERROR(16040, HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar la representación impresa del comprobante: %s"),

    // =========================================================================
    // 15000 - 15999: SUNAT / SERVICIOS EXTERNOS
    // =========================================================================
    SUNAT_COMMUNICATION_ERROR(15001, HttpStatus.BAD_GATEWAY, "Error de comunicación con los servidores de SUNAT: %s"),
    SUNAT_REJECTION_ERROR(15002, HttpStatus.UNPROCESSABLE_ENTITY, "SUNAT rechazó el comprobante [%s]: %s"),

    // =========================================================================
    // 14000 - 14999: MULTITENANCY (ACCESO Y ENRUTAMIENTO DE TENANTS)
    // =========================================================================
    TENANT_AUTH_REQUIRED(14001, HttpStatus.UNAUTHORIZED, "Se requiere autenticación para acceder a este tenant"),
    TENANT_HEADER_REQUIRED(14002, HttpStatus.BAD_REQUEST, "La cabecera 'X-Tenant-ID' (con el RUC de la empresa) es obligatoria para operar sobre este recurso"),
    TENANT_NOT_FOUND(14003, HttpStatus.NOT_FOUND, "Empresa con RUC %s no encontrada en el sistema"),
    TENANT_ACCESS_DENIED(14004, HttpStatus.FORBIDDEN, "Acceso denegado: Su Access Key solo autoriza operar sobre el RUC %s"),
    TENANT_SAAS_ACCESS_DENIED(14005, HttpStatus.FORBIDDEN, "Acceso denegado: Esta empresa no pertenece a su cuenta SaaS"),
    TENANT_INACTIVE(14006, HttpStatus.FORBIDDEN, "La empresa se encuentra inactiva o suspendida"),
    TENANT_INVALID_SCHEMA(14007, HttpStatus.BAD_REQUEST, "Identificador de esquema tenant '%s' inválido"),

    // =========================================================================
    // 10000 - 10999: SECURITY & AUTENTICACIÓN
    // =========================================================================
    SECURITY_TOKEN_REQUIRED(10001, HttpStatus.UNAUTHORIZED, "Se requiere autenticación. Envíe su Access Key mediante cabecera 'Authorization: Bearer <key>' o 'X-API-Key: <key>'"),
    SECURITY_INVALID_TOKEN(10002, HttpStatus.UNAUTHORIZED, "Access Key inválida o inexistente"),
    SECURITY_ACCOUNT_INACTIVE(10003, HttpStatus.UNAUTHORIZED, "La cuenta SaaS asociada se encuentra inactiva o suspendida"),
    SECURITY_EMPRESA_INACTIVE(10004, HttpStatus.UNAUTHORIZED, "La empresa asociada a este token se encuentra inactiva o suspendida"),
    SECURITY_PLATFORM_ACCESS_DENIED(10005, HttpStatus.FORBIDDEN, "Las llaves de empresa no tienen permisos para acceder a la administración de plataforma"),
    SECURITY_PLAN_ADMIN_REQUIRED(10006, HttpStatus.FORBIDDEN, "Solo el Administrador del sistema puede crear, modificar o eliminar planes comerciales"),
    SECURITY_ACCOUNT_CREATE_ADMIN_REQUIRED(10007, HttpStatus.FORBIDDEN, "Solo el Administrador del sistema puede dar de alta nuevas cuentas SaaS"),
    SECURITY_ACCOUNT_LIST_ADMIN_REQUIRED(10008, HttpStatus.FORBIDDEN, "Solo el Administrador del sistema puede listar todas las cuentas SaaS"),

    // =========================================================================
    // 13000 - 13999: PLATFORM ACCOUNT (CUENTAS SAAS)
    // =========================================================================
    ACCOUNT_NOT_FOUND(13001, HttpStatus.NOT_FOUND, "La cuenta SaaS solicitada no existe"),
    ACCOUNT_ACCESS_KEY_ALREADY_EXISTS(13002, HttpStatus.CONFLICT, "La accessKey ya existe en el sistema"),
    ACCOUNT_NAME_REQUIRED(13003, HttpStatus.BAD_REQUEST, "El nombre de la cuenta SaaS es obligatorio"),
    ACCOUNT_PLAN_REQUIRED(13004, HttpStatus.BAD_REQUEST, "El plan de suscripción es obligatorio para la cuenta SaaS"),

    // =========================================================================
    // 12000 - 12999: PLATFORM PLAN (PLANES COMERCIALES)
    // =========================================================================
    PLAN_NOT_FOUND(12001, HttpStatus.NOT_FOUND, "El plan solicitado no existe"),
    PLAN_CODE_ALREADY_EXISTS(12002, HttpStatus.CONFLICT, "Ya existe un plan con el código: %s"),
    PLAN_CODE_IN_USE(12003, HttpStatus.CONFLICT, "El código %s ya está en uso por otro plan"),
    PLAN_CODE_REQUIRED(12004, HttpStatus.BAD_REQUEST, "El código del plan es obligatorio"),
    PLAN_NAME_REQUIRED(12005, HttpStatus.BAD_REQUEST, "El nombre del plan es obligatorio"),
    PLAN_PRICE_INVALID(12006, HttpStatus.BAD_REQUEST, "El precio mensual del plan no puede ser negativo"),
    PLAN_LIMIT_INVALID(12007, HttpStatus.BAD_REQUEST, "El límite mensual de comprobantes no puede ser negativo"),

    // =========================================================================
    // 11000 - 11999: PLATFORM TENANT REGISTRY / ONBOARDING (EMPRESAS ROUTER)
    // =========================================================================
    ONBOARDING_RUC_ALREADY_EXISTS(11001, HttpStatus.CONFLICT, "El RUC %s ya se encuentra registrado en el sistema"),
    ONBOARDING_SCHEMA_ALREADY_EXISTS(11002, HttpStatus.CONFLICT, "El esquema %s ya existe"),
    ONBOARDING_ACCESS_KEY_ALREADY_EXISTS(11003, HttpStatus.CONFLICT, "La accessKey ya está en uso por otra empresa"),
    ONBOARDING_INVALID_STATE(11004, HttpStatus.BAD_REQUEST, "Estado no permitido. Valores válidos: ACTIVO, INACTIVO, SUSPENDIDO"),
    ONBOARDING_ACCOUNT_ACCESS_DENIED(11005, HttpStatus.FORBIDDEN, "Acceso denegado: Esta empresa no pertenece a su cuenta SaaS"),
    ONBOARDING_CROSS_ACCOUNT_DENIED(11006, HttpStatus.FORBIDDEN, "Acceso denegado: No puede consultar empresas de otra cuenta SaaS"),
    ONBOARDING_INVALID_RUC(11007, HttpStatus.BAD_REQUEST, "El RUC '%s' es inválido. Debe tener 11 dígitos numéricos y comenzar con 10 o 20"),
    ONBOARDING_RAZON_SOCIAL_REQUIRED(11008, HttpStatus.BAD_REQUEST, "La razón social de la empresa es obligatoria"),
    ONBOARDING_INITIALIZATION_ERROR(11009, HttpStatus.INTERNAL_SERVER_ERROR, "Se creó el esquema pero falló la inicialización de los datos de la empresa: %s");

    private final int code;
    private final HttpStatus httpStatus;
    private final String messageTemplate;

    ErrorCode(int code, HttpStatus httpStatus, String messageTemplate) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.messageTemplate = messageTemplate;
    }
}
