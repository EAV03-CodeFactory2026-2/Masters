package com.udea.masters.controllers;

import com.udea.masters.entities.Moneda;
import com.udea.masters.repositories.MonedaRepository;
import com.udea.masters.services.IMonedaService;
import com.udea.masters.services.MonedaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monedas")
@Tag(name = "Monedas", description = "Consulta del catálogo maestro de monedas admitidas")
public class MonedaController {

    private final IMonedaService monedaService;

    @Autowired
    public MonedaController(IMonedaService monedaService) {
        this.monedaService = monedaService;
    }

    public MonedaController(MonedaRepository monedaRepository) {
        this.monedaService = new MonedaService(monedaRepository);
    }

    @Operation(summary = "Listar monedas", description = "Retorna el listado maestro de todas las monedas activas en la plataforma.")
    @GetMapping
    public ResponseEntity<List<Moneda>> listarMonedas() {
        return ResponseEntity.ok(monedaService.listarMonedas());
    }
}
