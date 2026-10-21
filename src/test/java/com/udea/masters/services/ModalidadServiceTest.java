package com.udea.masters.services;

import com.udea.masters.entities.ModalidadServicio;
import com.udea.masters.repositories.ModalidadServicioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModalidadServiceTest {

    @Mock
    private ModalidadServicioRepository modalidadRepository;

    @InjectMocks
    private ModalidadService modalidadService;

    @Test
    void debeRetornarModalidades_cuandoExistenEnCatalogo() {
        List<ModalidadServicio> modalidades = List.of(
                new ModalidadServicio(1, "Presencial"),
                new ModalidadServicio(2, "Virtual"));
        when(modalidadRepository.findAll()).thenReturn(modalidades);

        List<ModalidadServicio> resultado = modalidadService.listarModalidades();

        assertEquals(modalidades, resultado);
        verify(modalidadRepository).findAll();
    }

    @Test
    void debeRetornarListaVacia_cuandoCatalogoSinModalidades() {
        when(modalidadRepository.findAll()).thenReturn(List.of());

        List<ModalidadServicio> resultado = modalidadService.listarModalidades();

        assertTrue(resultado.isEmpty());
    }
}
