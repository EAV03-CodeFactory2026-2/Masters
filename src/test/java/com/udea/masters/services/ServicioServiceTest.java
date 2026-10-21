package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.ServicioCreateRequest;
import com.udea.masters.entities.Negocio;
import com.udea.masters.entities.Servicio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.repositories.NegocioRepository;
import com.udea.masters.repositories.ServicioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarCon(String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("10", null, List.of(new SimpleGrantedAuthority(rol))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_PROPIETARIO", "PROPIETARIO", "role_propietario", "Propietario"})
    void debeCrearServicioNoAsignado_cuandoContextoTieneRolPropietario(String rol) {
        autenticarCon(rol);
        when(negocioRepository.findById(5)).thenReturn(Optional.of(negocio));
        when(servicioRepository.existsByNegocioIdNegocioAndNombreIgnoreCase(5, "Masaje Sueco")).thenReturn(false);
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);

        Servicio resultado = servicioService.crearServicio(request, 10);

        verify(servicioRepository).save(captor.capture());
        Servicio guardado = captor.getValue();
        assertSame(guardado, resultado);
        assertSame(negocio, guardado.getNegocio());
        assertEquals(1, guardado.getModalidadId());
        assertEquals("Masaje Sueco", guardado.getNombre());
        assertEquals("Masaje relajante de cuerpo completo", guardado.getDescripcion());
        assertEquals(60, guardado.getDuracionMinutos());
        assertEquals(new BigDecimal("50.00"), guardado.getPrecio());
        assertEquals("No Asignado", guardado.getEstado());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_CLIENTE", "PROVEEDOR", "ROLE_PROPIETARIOS"})
    void debeLanzarIllegalArgument_cuandoContextoConRolDistintoAPropietario(String rol) {
        autenticarCon(rol);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicioService.crearServicio(request, 10));

        assertEquals("El usuario no tiene el rol de Propietario.", ex.getMessage());
        verifyNoInteractions(negocioRepository);
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void debeLanzarIllegalArgument_cuandoContextoSinAutenticacion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicioService.crearServicio(request, 10));

        assertEquals("El usuario no tiene el rol de Propietario.", ex.getMessage());
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void debeLanzarIllegalArgument_cuandoAutenticacionSinAuthorities() {
        Authentication auth = mock(Authentication.class);
        doReturn(null).when(auth).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(auth);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicioService.crearServicio(request, 10));

        assertEquals("El usuario no tiene el rol de Propietario.", ex.getMessage());
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void debeLanzarIllegalArgument_cuandoNegocioNoExiste() {
        when(negocioRepository.findById(5)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicioService.crearServicio(request, 10, true));

        assertEquals("Negocio no encontrado.", ex.getMessage());
        verify(servicioRepository, never()).existsByNegocioIdNegocioAndNombreIgnoreCase(any(), any());
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    void debeRetornarListaVacia_cuandoNegocioSinServicios() {
        when(servicioRepository.findByNegocioIdNegocio(5)).thenReturn(List.of());

        List<Servicio> resultado = servicioService.listarServiciosPorNegocio(5);

        assertTrue(resultado.isEmpty());
    }
}
