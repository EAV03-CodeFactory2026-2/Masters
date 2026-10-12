package com.udea.masters.config;

import com.udea.masters.entities.Moneda;
import com.udea.masters.entities.TipoMoneda;
import com.udea.masters.repositories.MonedaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Order(1)
@RequiredArgsConstructor
public class MonedaDataSeeder implements CommandLineRunner {

    private final MonedaRepository monedaRepository;

    @Override
    public void run(String... args) {
        Arrays.stream(TipoMoneda.values()).forEach(tipo -> {
            String iso = tipo.name();
            if (!monedaRepository.existsById(iso)) {
                monedaRepository.save(Moneda.builder()
                        .monedaCodigoIso(iso)
                        .nombreMoneda(tipo.getNombre())
                        .build());
            }
        });
    }
}
