package com.udea.masters.controllers.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroResponse {

    private String token;
    @Builder.Default
    private String tipoToken = "Bearer";
    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private String rolActivo;
}
