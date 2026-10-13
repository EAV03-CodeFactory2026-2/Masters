package com.udea.masters.controllers;

import com.udea.masters.entities.ModalidadServicio;
import com.udea.masters.repositories.ModalidadServicioRepository;
import com.udea.masters.services.IModalidadService;
import com.udea.masters.services.ModalidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/modalidades")
@Tag(name = "Modalidades", description = "Consulta del catálogo maestro de modalidades de servicio")
public class ModalidadController {

    private final IModalidadService modalidadService;

    @Autowired
    public ModalidadController(IModalidadService modalidadService) {
        this.modalidadService = modalidadService;
    }

    public ModalidadController(ModalidadServicioRepository modalidadRepository) {
        this.modalidadService = new ModalidadService(modalidadRepository);
    }

    @Operation(summary = "Listar modalidades", description = "Retorna el catálogo maestro de modalidades de servicio disponibles (Presencial, Virtual).")
    @GetMapping
    public ResponseEntity<List<ModalidadServicio>> listarModalidades() {
        return ResponseEntity.ok(modalidadService.listarModalidades());
    }
}
