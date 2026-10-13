package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.Usuario;

public interface IRegistroRolStrategy {
    boolean soporta(String rol);
    String getNombreRol();
    void validar(RegistroUsuarioRequest request);
    void procesarRegistro(RegistroUsuarioRequest request, Usuario usuario);
}
