package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RecursoCreateRequest;
import com.udea.masters.controllers.DTOs.RecursoRegistradoResponse;
import com.udea.masters.controllers.DTOs.RecursosNegocioResponse;

public interface IRecursoService {
    RecursoRegistradoResponse registrarRecurso(Integer negocioId, RecursoCreateRequest request);
    RecursosNegocioResponse listarRecursos(Integer negocioId);
}
