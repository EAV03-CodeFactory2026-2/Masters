package com.udea.masters.controllers.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolAsignacionRequest {
    @NotBlank(message = "El nombre del rol no puede estar vacío.")
    private String nombreRol;
}
