package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.ProveedorDirectorioResponse;
import com.udea.masters.services.IProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Operaciones sobre proveedores y catálogo del directorio profesional")
public class ProveedorController {

    private final IProveedorService proveedorService;

    @Operation(
            summary = "Consultar directorio general de proveedores",
            description = "Retorna el directorio general de proveedores registrados y activos. Requiere rol Propietario.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PreAuthorize("hasRole('PROPIETARIO')")
    @GetMapping("/directorio")
    public ResponseEntity<List<ProveedorDirectorioResponse>> consultarDirectorio(
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(proveedorService.consultarDirectorio(query));
    }
}
