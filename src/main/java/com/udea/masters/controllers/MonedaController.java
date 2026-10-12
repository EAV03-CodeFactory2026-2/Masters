package com.udea.masters.controllers;

import com.udea.masters.entities.Moneda;
import com.udea.masters.repositories.MonedaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monedas")
@RequiredArgsConstructor
@Tag(name = "Monedas", description = "Consulta del catálogo maestro de monedas admitidas")
public class MonedaController {

    private final MonedaRepository monedaRepository;

    @Operation(summary = "Listar monedas", description = "Retorna el listado maestro de todas las monedas activas en la plataforma.")
    @GetMapping
    public ResponseEntity<List<Moneda>> listarMonedas() {
        return ResponseEntity.ok(monedaRepository.findAll());
    }
}
