package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.DirectorioProveedoresResponse;

public interface IProveedorService {
    DirectorioProveedoresResponse consultarDirectorio(String criterio, Integer pagina);
}
