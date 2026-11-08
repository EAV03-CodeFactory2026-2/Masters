package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.CrearFranjaRequest;
import com.udea.masters.controllers.DTOs.FranjaServicioResponse;

import java.util.List;

public interface IFranjaServicioService {

    FranjaServicioResponse crearFranja(Integer servicioId, CrearFranjaRequest request);

    List<FranjaServicioResponse> consultarFranjas(Integer servicioId, Integer proveedorId, String diaSemana);

    void eliminarFranja(Integer servicioId, Integer franjaId);
}
