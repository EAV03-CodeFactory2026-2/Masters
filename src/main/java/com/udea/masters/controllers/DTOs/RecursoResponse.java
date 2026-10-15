package com.udea.masters.controllers.DTOs;

import com.udea.masters.entities.Recurso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursoResponse {

    private Integer idRecurso;
    private Integer negocioId;
    private String nombre;
    private String tipo;
    private Integer cantidad;
    private String descripcion;
    private String estado;

    public static RecursoResponse desde(Recurso recurso) {
        return RecursoResponse.builder()
                .idRecurso(recurso.getIdRecurso())
                .negocioId(recurso.getNegocio().getIdNegocio())
                .nombre(recurso.getNombre())
                .tipo(recurso.getTipo().getNombre())
                .cantidad(recurso.getCantidad())
                .descripcion(recurso.getDescripcion())
                .estado(recurso.getEstado())
                .build();
    }
}
