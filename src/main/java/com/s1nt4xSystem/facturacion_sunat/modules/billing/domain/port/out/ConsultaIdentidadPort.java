package com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.port.out;

import com.s1nt4xSystem.facturacion_sunat.modules.billing.domain.model.DatosClienteIdentidad;

import java.util.Optional;

public interface ConsultaIdentidadPort {

    /**
     * Consulta los datos de una persona o empresa a través de su documento de identidad (DNI o RUC).
     *
     * @param tipoDocumento Tipo de documento según catálogo SUNAT ("1" para DNI, "6" para RUC).
     * @param numeroDocumento Número de documento.
     * @return Optional con los datos del cliente verificado, o Optional.empty() si no fue encontrado o el servicio no está disponible.
     */
    Optional<DatosClienteIdentidad> consultarDocumento(String tipoDocumento, String numeroDocumento);
}
