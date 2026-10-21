package com.udea.masters.services;

import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.repositories.TipoDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoDocumentoServiceTest {

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @InjectMocks
    private TipoDocumentoService tipoDocumentoService;

    private TipoDocumento cedula;

    @BeforeEach
    void setUp() {
        cedula = TipoDocumento.builder().idTipoDocumento(1).codigo("CC").nombre("Cédula de ciudadanía").build();
    }

    @Test
    void debeRetornarCatalogo_cuandoExistenTiposDocumento() {
        TipoDocumento pasaporte = TipoDocumento.builder().idTipoDocumento(2).codigo("PA").nombre("Pasaporte").build();
        when(tipoDocumentoRepository.findAll()).thenReturn(List.of(cedula, pasaporte));

        List<TipoDocumento> resultado = tipoDocumentoService.listarTiposDocumento();

        assertEquals(List.of(cedula, pasaporte), resultado);
        verify(tipoDocumentoRepository).findAll();
    }

    @Test
    void debeRetornarListaVacia_cuandoNoHayTiposDocumento() {
        when(tipoDocumentoRepository.findAll()).thenReturn(List.of());

        List<TipoDocumento> resultado = tipoDocumentoService.listarTiposDocumento();

        assertTrue(resultado.isEmpty());
    }

    @Test
    void debeRetornarTipoDocumento_cuandoCodigoExiste() {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("cc")).thenReturn(Optional.of(cedula));

        Optional<TipoDocumento> resultado = tipoDocumentoService.obtenerPorCodigo("cc");

        assertTrue(resultado.isPresent());
        assertSame(cedula, resultado.get());
    }

    @Test
    void debeRetornarVacio_cuandoCodigoNoExiste() {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("XX")).thenReturn(Optional.empty());

        Optional<TipoDocumento> resultado = tipoDocumentoService.obtenerPorCodigo("XX");

        assertTrue(resultado.isEmpty());
    }
}
