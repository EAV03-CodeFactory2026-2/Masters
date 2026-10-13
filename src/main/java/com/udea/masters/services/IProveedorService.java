package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.ProveedorDirectorioResponse;

import java.util.List;

public interface IProveedorService {
    List<ProveedorDirectorioResponse> consultarDirectorio(String query);
}
