package com.udea.masters.controllers;

import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.services.ITipoDocumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-documento")
@RequiredArgsConstructor
@Tag(name = "Tipos de Documento", description = "Consulta del catálogo maestro de tipos de documento de identidad")
public class TipoDocumentoController {

    private final ITipoDocumentoService tipoDocumentoService;

    @Operation(summary = "Listar tipos de documento", description = "Retorna el catálogo de tipos de documento admitidos (CC, CE, PA, PPT).")
    @GetMapping
    public ResponseEntity<List<TipoDocumento>> listarTiposDocumento() {
        return ResponseEntity.ok(tipoDocumentoService.listarTiposDocumento());
    }
}
