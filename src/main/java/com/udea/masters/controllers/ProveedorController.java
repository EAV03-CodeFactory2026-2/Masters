package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.DirectorioProveedoresResponse;
import com.udea.masters.services.IProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Operaciones sobre proveedores y catálogo del directorio profesional")
public class ProveedorController {

    private final IProveedorService proveedorService;

    @Operation(
            summary = "Buscar en el directorio de proveedores",
            description = "Busca proveedores activos con rol Proveedor por nombre, apellido o especialidad (coincidencia "
                    + "parcial) o por número de documento (exacto), sin distinguir mayúsculas. El criterio requiere "
                    + "mínimo 3 caracteres. Devuelve páginas de 10 con el tipo y número de documento de cada proveedor. "
                    + "Requiere rol Propietario.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de resultados (puede estar vacía)."),
            @ApiResponse(responseCode = "400", description = "Criterio con menos de 3 caracteres o página inválida."),
            @ApiResponse(responseCode = "403", description = "El usuario no tiene rol Propietario.")
    })
    @PreAuthorize("hasRole('PROPIETARIO')")
    @GetMapping("/directorio")
    public ResponseEntity<DirectorioProveedoresResponse> consultarDirectorio(
            @Parameter(description = "Texto a buscar (mínimo 3 caracteres)", example = "Car")
            @RequestParam(required = false) String query,
            @Parameter(description = "Alias de query para búsqueda", example = "Car")
            @RequestParam(required = false) String criterio,
            @Parameter(description = "Número de página, desde 1", example = "1")
            @RequestParam(required = false) Integer pagina,
            @Parameter(description = "Índice de página base 0 (opcional)", example = "0")
            @RequestParam(required = false) Integer page
    ) {
        String textoBusqueda = (query != null && !query.isBlank()) ? query : criterio;
        Integer numeroPagina = pagina != null ? pagina : (page != null ? (page >= 0 ? page + 1 : page) : null);
        return ResponseEntity.ok(proveedorService.consultarDirectorio(textoBusqueda, numeroPagina));
    }
}
