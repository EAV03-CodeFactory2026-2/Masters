package com.udea.masters.entities;

/**
 * Días de la semana en el orden en que se muestra el horario (inicia el lunes).
 */
public enum DiaSemana {
    LUNES("lunes"),
    MARTES("martes"),
    MIERCOLES("miércoles"),
    JUEVES("jueves"),
    VIERNES("viernes"),
    SABADO("sábado"),
    DOMINGO("domingo");

    private final String nombre;

    DiaSemana(String nombre) {
        this.nombre = nombre;
    }

    /** Nombre en minúscula para los mensajes, ej. "miércoles". */
    public String getNombre() {
        return nombre;
    }
}
