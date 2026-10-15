package com.udea.masters.entities;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tipos de recurso físico. Se guardan en la BD con su nombre ("Espacio", "Equipo").
 */
public enum TipoRecurso {
    ESPACIO("Espacio"),
    EQUIPO("Equipo");

    private final String nombre;

    TipoRecurso(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    /** Busca por nombre sin distinguir mayúsculas ni tildes ya normalizadas. */
    public static Optional<TipoRecurso> desdeNombre(String nombreNormalizado) {
        return Arrays.stream(values())
                .filter(t -> t.nombre.equalsIgnoreCase(nombreNormalizado))
                .findFirst();
    }
}
