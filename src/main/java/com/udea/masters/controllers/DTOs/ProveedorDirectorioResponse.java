package com.udea.masters.controllers.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Proveedor del directorio. Siempre incluye tipo y número de documento para diferenciar homónimos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorDirectorioResponse {

    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private String tipoDocumento;
    private String tipoDocumentoNombre;
    private String numeroDocumento;
    private String especialidad;
    private String descripcionProfesional;
    private boolean esCuentaActual;
}
