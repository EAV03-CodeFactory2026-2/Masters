package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.ServicioCreateRequest;
import com.udea.masters.entities.Servicio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.security.IAuthenticationFacade;
import com.udea.masters.services.IServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Servicios", description = "Endpoints para la gestión del catálogo de servicios de los negocios")
public class ServicioController {

    private final IServicioService servicioService;
    private final IAuthenticationFacade authenticationFacade;

    @Operation(
            summary = "Registrar un nuevo servicio",
            description = "Crea un nuevo servicio dentro del catálogo de un negocio perteneciente al propietario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Servicio registrado exitosamente con estado inicial No Asignado."),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos, negocio no encontrado o sin permisos de propietario."),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido."),
            @ApiResponse(responseCode = "409", description = "Conflicto: Ya existe un servicio con ese nombre en este negocio.")
    })
    @PostMapping("/servicios")
    public ResponseEntity<?> crearServicio(@Valid @RequestBody ServicioCreateRequest request) {
        // H-03: Obtener el id autenticado desde el facade; responder 401 si no hay sesión válida
        Integer usuarioId = authenticationFacade.getAuthenticatedUserId();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado o token inválido."));
        }
        try {
            Servicio servicio = servicioService.crearServicio(request, usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "mensaje", "Servicio registrado exitosamente en el catálogo.",
                            "idServicio", servicio.getIdServicio(),
                            "estado", servicio.getEstado()
                    ));
        } catch (RecursoDuplicadoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Listar servicios de un negocio",
            description = "Retorna la lista de todos los servicios asociados a un negocio específico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de servicios retornado exitosamente.")
    })
    @GetMapping("/servicios/negocio/{id}")
    public ResponseEntity<List<Servicio>> listarServiciosPorNegocio(@PathVariable("id") Integer negocioId) {
        return ResponseEntity.ok(servicioService.listarServiciosPorNegocio(negocioId));
    }
}
