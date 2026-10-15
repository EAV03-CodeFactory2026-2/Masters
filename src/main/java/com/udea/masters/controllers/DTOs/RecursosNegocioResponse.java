package com.udea.masters.controllers.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursosNegocioResponse {

    private String mensaje;
    private List<RecursoResponse> recursos;
}
