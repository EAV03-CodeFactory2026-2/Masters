package com.udea.masters.config;

import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.repositories.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
public class TipoDocumentoDataSeeder implements CommandLineRunner {

    private final TipoDocumentoRepository tipoDocumentoRepository;

    @Override
    public void run(String... args) {
        List<TipoDocumento> catalogo = List.of(
                TipoDocumento.builder().codigo("CC").nombre("Cédula de Ciudadanía").build(),
                TipoDocumento.builder().codigo("CE").nombre("Cédula de Extranjería").build(),
                TipoDocumento.builder().codigo("TI").nombre("Tarjeta de Identidad").build(),
                TipoDocumento.builder().codigo("PA").nombre("Pasaporte").build(),
                TipoDocumento.builder().codigo("PPT").nombre("Permiso por Protección Temporal").build()
        );

        for (TipoDocumento doc : catalogo) {
            if (tipoDocumentoRepository.findByCodigoIgnoreCase(doc.getCodigo()).isEmpty()) {
                tipoDocumentoRepository.save(doc);
            }
        }
    }
}
