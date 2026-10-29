package com.banknode.atm.exception;

/**
 * Excepcion personalizada para el manejo de reglas de negocio del cajero automatico.
 */
public class AtmException extends RuntimeException {

    public AtmException(String mensaje) {
        super(mensaje);
    }
}
