package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.ServicioCreateRequest;
import com.udea.masters.entities.Servicio;

import java.util.List;

public interface IServicioService {
    Servicio crearServicio(ServicioCreateRequest request, Integer usuarioIdAutenticado, boolean esPropietario);
    Servicio crearServicio(ServicioCreateRequest request, Integer usuarioIdAutenticado);
    List<Servicio> listarServiciosPorNegocio(Integer negocioId);
}
