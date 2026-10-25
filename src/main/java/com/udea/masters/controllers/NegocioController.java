package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.NegocioCreateRequest;
import com.udea.masters.entities.Negocio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.security.IAuthenticationFacade;
import com.udea.masters.services.INegocioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Negocios", description = "Gestión de negocios del propietario (registro y administración)")
public class NegocioController {

    private final INegocioService negocioService;
    private final IAuthenticationFacade authenticationFacade;

    @Operation(
            summary = "Registrar un nuevo negocio",
            description = "Crea un negocio vinculado al propietario autenticado (extraído del JWT). Requiere rol de Propietario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Negocio creado y activado exitosamente."),
            @ApiResponse(responseCode = "400", description = "Error de validación o usuario no autorizado."),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido."),
            @ApiResponse(responseCode = "409", description = "Conflicto: La identificación fiscal ya está en uso.")
    })
    @PostMapping("/negocios")
    public ResponseEntity<?> crearNegocio(@Valid @RequestBody NegocioCreateRequest request) {
        // H-03: Obtener el id autenticado desde el facade; responder 401 si no hay sesión válida
        Integer usuarioId = authenticationFacade.getAuthenticatedUserId();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado o token inválido."));
        }
        try {
            Negocio negocio = negocioService.crearNegocio(request, usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensaje", "Negocio creado y activado exitosamente.", "idNegocio", negocio.getIdNegocio()));
        } catch (RecursoDuplicadoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Listar mis negocios",
            description = "Retorna todos los negocios que pertenecen al usuario autenticado (Propietario)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de negocios retornado exitosamente."),
            @ApiResponse(responseCode = "400", description = "El usuario no tiene el rol de Propietario."),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido.")
    })
    @GetMapping("/negocios")
    public ResponseEntity<?> listarMisNegocios() {
        // H-03: Obtener el id autenticado desde el facade; responder 401 si no hay sesión válida
        Integer usuarioId = authenticationFacade.getAuthenticatedUserId();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autenticado o token inválido."));
        }
        try {
            return ResponseEntity.ok(negocioService.listarMisNegocios(usuarioId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Listar negocios disponibles para clientes",
            description = "Retorna el catálogo de negocios activos y disponibles en la plataforma para consulta general de clientes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo de negocios retornado exitosamente."),
            @ApiResponse(responseCode = "401", description = "No autenticado o token inválido.")
    })
    @GetMapping("/negocios/disponibles")
    public ResponseEntity<java.util.List<Negocio>> listarNegociosDisponibles() {
        return ResponseEntity.ok(negocioService.listarNegociosDisponibles());
    }
}
