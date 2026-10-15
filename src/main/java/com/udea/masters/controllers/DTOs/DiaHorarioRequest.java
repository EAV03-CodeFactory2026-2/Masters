package com.udea.masters.controllers.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaHorarioRequest {

    @Schema(description = "Día de la semana", example = "LUNES",
            allowableValues = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"})
    private String dia;

    @Schema(description = "Interruptor del día. Si es false, los bloques se ignoran. Si se omite, "
            + "el día se considera activo cuando trae bloques.", example = "true")
    private Boolean activo;

    @Schema(description = "De 1 a 4 bloques si el día está activo")
    private List<BloqueHorarioDTO> bloques;
}
