package com.udea.masters.config;

import com.udea.masters.entities.ModalidadServicio;
import com.udea.masters.entities.TipoModalidad;
import com.udea.masters.repositories.ModalidadServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Order(1)
@RequiredArgsConstructor
public class ModalidadDataSeeder implements CommandLineRunner {

    private final ModalidadServicioRepository modalidadRepository;

    @Override
    public void run(String... args) {
        Arrays.stream(TipoModalidad.values()).forEach(tipo -> {
            if (modalidadRepository.findByNombreModalidad(tipo.getNombre()).isEmpty()) {
                modalidadRepository.save(ModalidadServicio.builder()
                        .nombreModalidad(tipo.getNombre())
                        .build());
            }
        });
    }
}
