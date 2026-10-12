package com.udea.masters.controllers.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private String estado;
    private OffsetDateTime fechaRegistro;
    private Set<String> roles;
}
