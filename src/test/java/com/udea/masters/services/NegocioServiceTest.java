package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.NegocioCreateRequest;
import com.udea.masters.entities.Negocio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.repositories.NegocioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NegocioServiceTest {

    @Mock
    private NegocioRepository negocioRepository;

    @InjectMocks
    private NegocioService negocioService;

    private NegocioCreateRequest request;
    private Negocio negocio;

    @BeforeEach
    void setUp() {
        request = NegocioCreateRequest.builder()
                .nombreNegocio("Peluquería Central")
                .direccionNegocio("Calle 10 # 20-30")
                .telefono("3001234567")
                .identificacionFiscal("NIT-123456")
                .monedaCodigoIso("COP")
                .build();

        negocio = Negocio.builder()
                .idNegocio(1)
                .propietarioId(10)
                .nombreNegocio("Peluquería Central")
                .direccionNegocio("Calle 10 # 20-30")
                .telefono("3001234567")
                .identificacionFiscal("NIT-123456")
                .monedaCodigoIso("COP")
                .estado("Activo")
                .build();
    }

    @Test
    void crearNegocioExitoso() {
        when(negocioRepository.existsByIdentificacionFiscalIgnoreCase("NIT-123456")).thenReturn(false);
        when(negocioRepository.save(any(Negocio.class))).thenReturn(negocio);

        Negocio resultado = negocioService.crearNegocio(request, 10, true);

        assertNotNull(resultado);
        assertEquals("Peluquería Central", resultado.getNombreNegocio());
        assertEquals(10, resultado.getPropietarioId());
        assertEquals("Activo", resultado.getEstado());
        verify(negocioRepository, times(1)).save(any(Negocio.class));
    }

    @Test
    void crearNegocioFallaPorIdentificacionFiscalDuplicada() {
        when(negocioRepository.existsByIdentificacionFiscalIgnoreCase("NIT-123456")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> negocioService.crearNegocio(request, 10, true));

        assertTrue(ex.getMessage().contains("ya está en uso"));
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    void crearNegocioFallaCuandoUsuarioNoEsPropietario() {
        when(negocioRepository.existsByIdentificacionFiscalIgnoreCase("NIT-123456")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.crearNegocio(request, 10, false));

        assertTrue(ex.getMessage().contains("no tiene el rol de Propietario"));
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    void listarMisNegociosExitoso() {
        when(negocioRepository.findByPropietarioId(10)).thenReturn(List.of(negocio));

        List<Negocio> lista = negocioService.listarMisNegocios(10, true);

        assertEquals(1, lista.size());
        assertEquals("Peluquería Central", lista.get(0).getNombreNegocio());
    }

    @Test
    void listarMisNegociosFallaCuandoNoEsPropietario() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.listarMisNegocios(10, false));

        assertTrue(ex.getMessage().contains("no tiene el rol de Propietario"));
    }
}
