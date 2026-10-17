package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.HorarioSemanalRequest;
import com.udea.masters.controllers.DTOs.HorarioSemanalResponse;
import com.udea.masters.services.IHorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/proveedores/me/horario", "/api/v1/proveedores/horario"})
@RequiredArgsConstructor
@Tag(name = "Horario de atención", description = "Horario semanal recurrente del proveedor autenticado")
public class HorarioController {

    private final IHorarioService horarioService;

    @Operation(
            summary = "Consultar mi horario de atención",
            description = "Retorna los 7 días de la semana (lunes a domingo) con su estado y bloques. "
                    + "Si nunca se ha guardado un horario, todos los días figuran inactivos. Requiere rol activo Proveedor.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horario semanal del proveedor."),
            @ApiResponse(responseCode = "403", description = "La cuenta no opera con el rol Proveedor."),
            @ApiResponse(responseCode = "404", description = "La cuenta no tiene perfil de proveedor.")
    })
    @GetMapping
    public ResponseEntity<HorarioSemanalResponse> consultar() {
        return ResponseEntity.ok(horarioService.consultarMiHorario());
    }

    @Operation(
            summary = "Guardar mi horario de atención",
            description = "Reemplaza por completo el horario anterior. Cada día activo admite de 1 a 4 bloques en "
                    + "formato HH:mm de 24 horas, en intervalos de 15 minutos, sin superponerse (los contiguos sí se "
                    + "permiten). Debe haber al menos un día activo. Requiere rol activo Proveedor.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horario guardado correctamente."),
            @ApiResponse(responseCode = "400", description = "El horario no cumple las reglas; indica el día y el bloque afectados."),
            @ApiResponse(responseCode = "403", description = "La cuenta no opera con el rol Proveedor."),
            @ApiResponse(responseCode = "404", description = "La cuenta no tiene perfil de proveedor.")
    })
    @PutMapping
    public ResponseEntity<HorarioSemanalResponse> guardar(@RequestBody(required = false) HorarioSemanalRequest request) {
        return ResponseEntity.ok(horarioService.guardarMiHorario(request));
    }
}
