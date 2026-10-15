package com.udea.masters.exceptions;

import lombok.Getter;

/**
 * Horario que no cumple las reglas (HTTP 400). Indica, cuando aplica, el día y el bloque afectados.
 */
@Getter
public class HorarioInvalidoException extends RuntimeException {

    private final String dia;
    private final String bloque;

    public HorarioInvalidoException(String message) {
        this(message, null, null);
    }

    public HorarioInvalidoException(String message, String dia, String bloque) {
        super(message);
        this.dia = dia;
        this.bloque = bloque;
    }
}
