package com.udea.masters.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda TipoRecurso como "Espacio"/"Equipo", el mismo valor que ya existe en la tabla recurso.
 */
@Converter
public class TipoRecursoConverter implements AttributeConverter<TipoRecurso, String> {

    @Override
    public String convertToDatabaseColumn(TipoRecurso tipo) {
        return tipo == null ? null : tipo.getNombre();
    }

    @Override
    public TipoRecurso convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        return TipoRecurso.desdeNombre(valor)
                .orElseThrow(() -> new IllegalStateException("Tipo de recurso desconocido en la BD: " + valor));
    }
}
