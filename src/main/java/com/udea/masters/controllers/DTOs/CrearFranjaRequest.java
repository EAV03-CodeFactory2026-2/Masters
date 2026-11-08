package com.udea.masters.controllers.DTOs;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearFranjaRequest {

    @Schema(description = "Identificador del servicio (opcional si se pasa en la ruta)", example = "1")
    private Integer servicioId;

    @Schema(description = "Identificador del proveedor asignado (obligatorio si crea el propietario; opcional si crea el proveedor)", example = "12")
    private Integer proveedorId;

    @Schema(description = "Día de la semana de atención recurrente", example = "LUNES")
    @JsonAlias({"dia", "dia_semana"})
    private String diaSemana;

    @Schema(description = "Hora de inicio en formato HH:mm de 24 horas", example = "09:00")
    @JsonAlias({"inicio", "hora_inicio"})
    private String horaInicio;
}
