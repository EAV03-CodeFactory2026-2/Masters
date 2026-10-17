package com.udea.masters.controllers.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroUsuarioRequest {

    private String rol;
    private String correo;
    @ToString.Exclude
    private String contrasena;
    private String nombre;
    private String apellido;
    private String telefono;

    // Campos específicos para Propietario y Proveedor
    private String tipoDocumento;
    private String numeroDocumento;

    // Campos específicos para Proveedor
    private String especialidad;
    private String descripcionProfesional;
}
