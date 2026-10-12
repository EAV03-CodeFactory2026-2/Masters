package com.udea.masters.controllers.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCreateRequest {

    @NotBlank(message = "El nombre no puede estar vacío.")
    private String nombre;

    @NotBlank(message = "El apellido no puede estar vacío.")
    private String apellido;

    @NotBlank(message = "El correo no puede estar vacío.")
    @Email(message = "El formato de correo es inválido.")
    private String correo;

    @NotBlank(message = "El teléfono no puede estar vacío.")
    private String telefono;

    @NotBlank(message = "La contraseña no puede estar vacía.")
    private String contrasena;

    private Set<String> roles;
}
