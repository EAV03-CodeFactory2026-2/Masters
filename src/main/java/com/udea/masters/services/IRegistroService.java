package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RegistroResponse;
import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;

public interface IRegistroService {
    RegistroResponse registrar(RegistroUsuarioRequest request);
}
