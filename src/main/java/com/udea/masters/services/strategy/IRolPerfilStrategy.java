package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.entities.Usuario;

public interface IRolPerfilStrategy {
    boolean soporta(String nombreRol);
    String getNombreRol();
    void asignarPerfil(Usuario usuario, RolAsignacionRequest request);
    void removerPerfil(Integer idUsuario);
}
