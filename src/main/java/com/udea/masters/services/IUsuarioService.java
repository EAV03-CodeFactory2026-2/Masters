package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;

import java.util.List;

public interface IUsuarioService {
    UsuarioResponse obtenerUsuarioPorId(Integer idUsuario);
    List<UsuarioResponse> listarUsuarios();
    List<UsuarioResponse> listarPorRol(String nombreRol);
    void asignarRolAUsuario(Integer idUsuario, RolAsignacionRequest request);
    void asignarRolAUsuario(Integer idUsuario, String nombreRol);
    void removerRolDeUsuario(Integer idUsuario, String nombreRol);
}
