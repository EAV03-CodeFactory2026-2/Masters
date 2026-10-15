package com.udea.masters.controllers.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Las validaciones se hacen en RecursoValidator, después de verificar permisos,
 * para que un usuario sin permiso reciba 403 antes que cualquier error de datos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursoCreateRequest {

    @Schema(example = "Camilla de masajes", maxLength = 100)
    private String nombre;

    @Schema(example = "Equipo", allowableValues = {"Espacio", "Equipo"})
    private String tipo;

    // Se recibe como Object para rechazar con el mensaje de la HU valores como 2.5, "abc" o vacío,
    // que con Integer harían fallar la lectura del JSON con un error genérico.
    @Schema(type = "integer", example = "3", minimum = "1", maximum = "999")
    private Object cantidad;

    @Schema(example = "Camilla plegable con apoyacabezas", maxLength = 500, nullable = true)
    private String descripcion;
}
