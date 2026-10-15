package com.udea.masters.exceptions;

/**
 * El usuario no opera con el rol que exige la operación (HTTP 403).
 */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String message) {
        super(message);
    }
}
