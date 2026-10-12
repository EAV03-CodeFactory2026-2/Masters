package com.udea.masters.entities;

public enum TipoRol {
    CLIENTE("Cliente"),
    PROVEEDOR("Proveedor"),
    PROPIETARIO("Propietario");

    private final String nombre;

    TipoRol(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
