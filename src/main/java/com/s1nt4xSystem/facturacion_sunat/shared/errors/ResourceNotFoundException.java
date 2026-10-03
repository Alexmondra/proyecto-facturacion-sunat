package com.s1nt4xSystem.facturacion_sunat.shared.errors;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s no encontrado con el identificador: %s", resourceName, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
