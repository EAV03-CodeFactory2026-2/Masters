package com.udea.masters.entities;

public enum TipoModalidad {
    PRESENCIAL("Presencial"),
    VIRTUAL("Virtual");

    private final String nombre;

    TipoModalidad(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
