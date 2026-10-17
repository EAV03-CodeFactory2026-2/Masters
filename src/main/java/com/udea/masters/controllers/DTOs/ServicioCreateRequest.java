package com.udea.masters.controllers.DTOs;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioCreateRequest {

    @NotNull(message = "El ID del negocio es obligatorio.")
    private Integer negocioId;

    @NotNull(message = "El ID de la modalidad es obligatorio.")
    private Integer modalidadId;

    @NotBlank(message = "El nombre del servicio no puede estar vacío.")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres.")
    private String nombre;

    @NotBlank(message = "La descripción no puede estar vacía.")
    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
    private String descripcion;

    @NotNull(message = "La duración en minutos es obligatoria.")
    @Min(value = 1, message = "La duración debe ser de al menos 1 minuto.")
    private Integer duracionMinutos;

    @NotNull(message = "El precio es obligatorio.")
    @DecimalMin(value = "0.01", inclusive = true, message = "El precio debe ser un número positivo mayor a cero.")
    @Digits(integer = 10, fraction = 2, message = "El precio solo puede tener hasta 2 decimales.")
    private BigDecimal precio;
}
