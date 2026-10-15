package com.udea.masters.controllers.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Horario semanal completo. Reemplaza el anterior; los días que no se envían quedan inactivos.
 * Las validaciones se hacen en HorarioValidator, después de verificar el rol.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioSemanalRequest {

    @Schema(description = "Días del horario (de lunes a domingo)")
    private List<DiaHorarioRequest> dias;
}
