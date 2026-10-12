package com.udea.masters.config;

import com.udea.masters.entities.Rol;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.repositories.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Order(1)
@RequiredArgsConstructor
public class RolDataSeeder implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) {
        Arrays.stream(TipoRol.values()).forEach(tipoRol -> {
            if (rolRepository.findByNombreRol(tipoRol.getNombre()).isEmpty()) {
                rolRepository.save(Rol.builder()
                        .nombreRol(tipoRol.getNombre())
                        .build());
            }
        });
    }
}
