package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.RecursoCreateRequest;
import com.udea.masters.controllers.DTOs.RecursoRegistradoResponse;
import com.udea.masters.controllers.DTOs.RecursosNegocioResponse;
import com.udea.masters.services.IRecursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/negocios/{negocioId}/recursos")
@RequiredArgsConstructor
@Tag(name = "Recursos", description = "Inventario de recursos físicos (espacios y equipos) de un negocio")
public class RecursoController {

    private final IRecursoService recursoService;

    @Operation(
            summary = "Registrar un recurso físico",
            description = "Registra un espacio o equipo en un negocio del propietario autenticado. "
                    + "El nombre es único por negocio (sin distinguir mayúsculas ni tildes). Requiere rol activo Propietario.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recurso registrado correctamente con estado Activo."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (nombre, tipo, cantidad o descripción)."),
            @ApiResponse(responseCode = "403", description = "El rol activo no es Propietario o el negocio no le pertenece."),
            @ApiResponse(responseCode = "404", description = "El negocio no existe."),
            @ApiResponse(responseCode = "409", description = "Ya existe un recurso con este nombre en el negocio.")
    })
    @PostMapping
    public ResponseEntity<RecursoRegistradoResponse> registrarRecurso(@PathVariable("negocioId") Integer negocioId,
                                                                      @RequestBody(required = false) RecursoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recursoService.registrarRecurso(negocioId, request));
    }

    @Operation(
            summary = "Listar recursos de un negocio",
            description = "Retorna los recursos del negocio indicado. Si no tiene, retorna una lista vacía. "
                    + "Requiere rol activo Propietario y ser dueño del negocio.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado retornado (puede estar vacío)."),
            @ApiResponse(responseCode = "403", description = "El rol activo no es Propietario o el negocio no le pertenece."),
            @ApiResponse(responseCode = "404", description = "El negocio no existe.")
    })
    @GetMapping
    public ResponseEntity<RecursosNegocioResponse> listarRecursos(@PathVariable("negocioId") Integer negocioId) {
        return ResponseEntity.ok(recursoService.listarRecursos(negocioId));
    }
}
