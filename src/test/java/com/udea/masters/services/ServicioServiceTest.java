package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.ServicioCreateRequest;
import com.udea.masters.entities.Negocio;
import com.udea.masters.entities.Servicio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.repositories.NegocioRepository;
import com.udea.masters.repositories.ServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @InjectMocks
    private ServicioService servicioService;

    private Negocio negocio;
    private ServicioCreateRequest request;
    private Servicio servicio;

    @BeforeEach
    void setUp() {
        negocio = Negocio.builder()
                .idNegocio(5)
                .propietarioId(10)
                .nombreNegocio("Spa Relax")
                .estado("Activo")
                .build();

        request = ServicioCreateRequest.builder()
                .negocioId(5)
                .modalidadId(1)
                .nombre("Masaje Sueco")
                .descripcion("Masaje relajante de cuerpo completo")
                .duracionMinutos(60)
                .precio(new BigDecimal("50.00"))
                .build();

        servicio = Servicio.builder()
                .idServicio(100)
                .negocio(negocio)
                .modalidadId(1)
                .nombre("Masaje Sueco")
                .descripcion("Masaje relajante de cuerpo completo")
                .duracionMinutos(60)
                .precio(new BigDecimal("50.00"))
                .estado("No Asignado")
                .build();
    }

    @Test
    void crearServicioExitoso() {
        when(negocioRepository.findById(5)).thenReturn(Optional.of(negocio));
        when(servicioRepository.existsByNegocioIdNegocioAndNombreIgnoreCase(5, "Masaje Sueco")).thenReturn(false);
        when(servicioRepository.save(any(Servicio.class))).thenReturn(servicio);

        Servicio resultado = servicioService.crearServicio(request, 10, true);

        assertNotNull(resultado);
        assertEquals("Masaje Sueco", resultado.getNombre());
        assertEquals("No Asignado", resultado.getEstado());
        verify(servicioRepository, times(1)).save(any(Servicio.class));
    }

    @Test
    void crearServicioFallaCuandoNoEsPropietario() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicioService.crearServicio(request, 10, false));

        assertTrue(ex.getMessage().contains("no tiene el rol de Propietario"));
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void crearServicioFallaCuandoNegocioNoPerteneceAlUsuario() {
        when(negocioRepository.findById(5)).thenReturn(Optional.of(negocio));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicioService.crearServicio(request, 99, true));

        assertTrue(ex.getMessage().contains("No tienes permisos para agregar servicios"));
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void crearServicioFallaPorNombreDuplicadoEnMismoNegocio() {
        when(negocioRepository.findById(5)).thenReturn(Optional.of(negocio));
        when(servicioRepository.existsByNegocioIdNegocioAndNombreIgnoreCase(5, "Masaje Sueco")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> servicioService.crearServicio(request, 10, true));

        assertTrue(ex.getMessage().contains("Ya existe un servicio con ese nombre"));
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void listarServiciosPorNegocioExitoso() {
        when(servicioRepository.findByNegocioIdNegocio(5)).thenReturn(List.of(servicio));

        List<Servicio> resultado = servicioService.listarServiciosPorNegocio(5);

        assertEquals(1, resultado.size());
        assertEquals("Masaje Sueco", resultado.get(0).getNombre());
    }
}
