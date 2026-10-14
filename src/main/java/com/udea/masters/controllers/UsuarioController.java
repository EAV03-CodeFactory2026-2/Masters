package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.RegistroResponse;
import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.services.IRegistroService;
import com.udea.masters.services.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios del dominio, registro de roles y perfiles")
public class UsuarioController {

    private final IUsuarioService usuarioService;
    private final IRegistroService registroService;

    @Operation(summary = "Registrar cuenta con rol (Propietario, Proveedor, Cliente)", description = "Permite a un usuario registrar su cuenta e iniciar sesión automáticamente con su rol activo.")
    @PostMapping("/registro")
    public ResponseEntity<RegistroResponse> registrar(@RequestBody RegistroUsuarioRequest request) {
        RegistroResponse response = registroService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Consultar usuario por ID", description = "Obtiene los detalles del usuario según su identificador único. Requiere rol Propietario o ser el propio usuario.", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('PROPIETARIO') or (authentication != null and authentication.principal != null and authentication.principal.toString() == #id.toString())")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
    }

    @Operation(summary = "Listar todos los usuarios", description = "Retorna el listado completo de usuarios registrados. Requiere rol Propietario.", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('PROPIETARIO')")
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @Operation(summary = "Listar usuarios por rol", description = "Filtra usuarios según su rol asignado (ej. Proveedor, Cliente, Propietario). Requiere rol Propietario.", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('PROPIETARIO')")
    @GetMapping("/rol/{nombreRol}")
    public ResponseEntity<List<UsuarioResponse>> listarPorRol(@PathVariable String nombreRol) {
        return ResponseEntity.ok(usuarioService.listarPorRol(nombreRol));
    }

    @Operation(summary = "Agregar rol a mi cuenta", description = "Permite a un usuario agregar un rol a su propia cuenta desde 'Mi cuenta'.", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("authentication != null and authentication.principal != null and authentication.principal.toString() == #id.toString()")
    @PostMapping("/{id}/roles")
    public ResponseEntity<Map<String, String>> asignarRol(@PathVariable Integer id, @Valid @RequestBody RolAsignacionRequest request) {
        usuarioService.asignarRolAUsuario(id, request);
        return ResponseEntity.ok(Map.of("mensaje", "Rol asignado correctamente."));
    }

    @Operation(summary = "Remover rol de mi cuenta", description = "Permite a un usuario eliminar un rol de su propia cuenta desde 'Mi cuenta'.", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("authentication != null and authentication.principal != null and authentication.principal.toString() == #id.toString()")
    @DeleteMapping("/{id}/roles/{nombreRol}")
    public ResponseEntity<Map<String, String>> removerRol(@PathVariable Integer id, @PathVariable String nombreRol) {
        usuarioService.removerRolDeUsuario(id, nombreRol);
        return ResponseEntity.ok(Map.of("mensaje", "Rol removido correctamente."));
    }
}
