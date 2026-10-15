package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.HorarioSemanalRequest;
import com.udea.masters.controllers.DTOs.HorarioSemanalResponse;

public interface IHorarioService {
    HorarioSemanalResponse consultarMiHorario();
    HorarioSemanalResponse guardarMiHorario(HorarioSemanalRequest request);
}
