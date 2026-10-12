package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.controllers.DTOs.UsuarioCreateRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.services.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios del dominio y asignación de roles")
public class UsuarioController {

    private final IUsuarioService usuarioService;

    @Operation(summary = "Registrar nuevo usuario", description = "Crea un usuario en el maestro de usuarios con sus respectivos roles.")
    @PostMapping
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody UsuarioCreateRequest request) {
        try {
            UsuarioResponse response = usuarioService.crearUsuario(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Consultar usuario por ID", description = "Obtiene los detalles del usuario según su identificador único.")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Listar todos los usuarios", description = "Retorna el listado completo de usuarios registrados.")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @Operation(summary = "Listar usuarios por rol", description = "Filtra usuarios según su rol asignado (ej. Proveedor, Cliente, Propietario).")
    @GetMapping("/rol/{nombreRol}")
    public ResponseEntity<List<UsuarioResponse>> listarPorRol(@PathVariable String nombreRol) {
        return ResponseEntity.ok(usuarioService.listarPorRol(nombreRol));
    }

    @Operation(summary = "Asignar rol a usuario", description = "Agrega un rol al usuario especificado.")
    @PostMapping("/{id}/roles")
    public ResponseEntity<?> asignarRol(@PathVariable Integer id, @Valid @RequestBody RolAsignacionRequest request) {
        try {
            usuarioService.asignarRolAUsuario(id, request.getNombreRol());
            return ResponseEntity.ok(Map.of("mensaje", "Rol asignado correctamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Remover rol de usuario", description = "Elimina un rol del usuario especificado.")
    @DeleteMapping("/{id}/roles/{nombreRol}")
    public ResponseEntity<?> removerRol(@PathVariable Integer id, @PathVariable String nombreRol) {
        try {
            usuarioService.removerRolDeUsuario(id, nombreRol);
            return ResponseEntity.ok(Map.of("mensaje", "Rol removido correctamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
