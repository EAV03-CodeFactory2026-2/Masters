package com.udea.masters.controllers.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Franja horaria en formato 24h "HH:mm". Se recibe como texto para validar el formato con mensajes propios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueHorarioDTO {

    @Schema(example = "08:00")
    private String horaInicio;

    @Schema(example = "12:00")
    private String horaFin;

    public String comoTexto() {
        return horaInicio + "-" + horaFin;
    }
}
