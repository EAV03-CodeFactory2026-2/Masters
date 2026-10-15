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
public class DirectorioProveedoresResponse {

    private String mensaje;
    private List<ProveedorDirectorioResponse> proveedores;
    private int pagina;
    private int tamanoPagina;
    private long totalResultados;
    private int totalPaginas;
    private boolean hayMasPaginas;
}
