package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.CrearFranjaRequest;
import com.udea.masters.controllers.DTOs.FranjaServicioResponse;
import com.udea.masters.services.IFranjaServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Franjas Horarias de Servicio", description = "Definición y control de horarios reservables para los servicios")
public class FranjaServicioController {

    private final IFranjaServicioService franjaServicioService;

    @Operation(
            summary = "Definir una franja horaria para un servicio",
            description = "Permite al proveedor asignado o al propietario del negocio definir franjas reservables dentro del horario del proveedor.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Franja creada correctamente."),
            @ApiResponse(responseCode = "400", description = "La franja no cabe en el horario de atención o se cruza con otra ya existente."),
            @ApiResponse(responseCode = "403", description = "No tienes permiso para definir franjas de este servicio."),
            @ApiResponse(responseCode = "404", description = "Servicio o proveedor no encontrado.")
    })
    @PostMapping({"/servicios/{servicioId}/franjas"})
    public ResponseEntity<FranjaServicioResponse> crearFranja(
            @PathVariable(value = "servicioId", required = false) Integer servicioId,
            @RequestBody CrearFranjaRequest request) {
        FranjaServicioResponse respuesta = franjaServicioService.crearFranja(servicioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(
            summary = "Consultar franjas horarias de un servicio",
            description = "Recupera las franjas horarias configuradas para un servicio, con filtros opcionales de proveedor y día.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de franjas horarias retornado exitosamente."),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado.")
    })
    @GetMapping("/servicios/{servicioId}/franjas")
    public ResponseEntity<List<FranjaServicioResponse>> consultarFranjas(
            @PathVariable("servicioId") Integer servicioId,
            @Parameter(description = "Identificador del proveedor (opcional)")
            @RequestParam(required = false) Integer proveedorId,
            @Parameter(description = "Día de la semana, ej. LUNES (opcional)")
            @RequestParam(required = false) String diaSemana) {
        List<FranjaServicioResponse> franjas = franjaServicioService.consultarFranjas(servicioId, proveedorId, diaSemana);
        return ResponseEntity.ok(franjas);
    }

    @Operation(
            summary = "Eliminar una franja horaria",
            description = "Elimina una franja horaria previamente definida. No afecta las reservas ya confirmadas.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Franja eliminada exitosamente."),
            @ApiResponse(responseCode = "403", description = "No tienes permiso para modificar este servicio."),
            @ApiResponse(responseCode = "404", description = "Franja no encontrada.")
    })
    @DeleteMapping({"/servicios/{servicioId}/franjas/{franjaId}"})
    public ResponseEntity<Void> eliminarFranja(
            @PathVariable(value = "servicioId", required = false) Integer servicioId,
            @PathVariable("franjaId") Integer franjaId) {
        franjaServicioService.eliminarFranja(servicioId, franjaId);
        return ResponseEntity.noContent().build();
    }
}
