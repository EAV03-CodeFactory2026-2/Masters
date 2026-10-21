package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.NegocioCreateRequest;
import com.udea.masters.entities.Negocio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.repositories.NegocioRepository;
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

import java.util.Arrays;
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

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarCon(String... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("10", null, authorities));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_PROPIETARIO", "PROPIETARIO", "role_propietario", "Propietario"})
    void debeCrearNegocioActivo_cuandoContextoTieneRolPropietario(String rol) {
        autenticarCon("ROLE_CLIENTE", rol);
        when(negocioRepository.existsByIdentificacionFiscalIgnoreCase("NIT-123456")).thenReturn(false);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Negocio> captor = ArgumentCaptor.forClass(Negocio.class);

        Negocio resultado = negocioService.crearNegocio(request, 10);

        verify(negocioRepository).save(captor.capture());
        Negocio guardado = captor.getValue();
        assertSame(guardado, resultado);
        assertEquals(10, guardado.getPropietarioId());
        assertEquals("Peluquería Central", guardado.getNombreNegocio());
        assertEquals("Calle 10 # 20-30", guardado.getDireccionNegocio());
        assertEquals("NIT-123456", guardado.getIdentificacionFiscal());
        assertEquals("3001234567", guardado.getTelefono());
        assertEquals("COP", guardado.getMonedaCodigoIso());
        assertEquals("Activo", guardado.getEstado());
    }

    @Test
    void debeLanzarIllegalArgument_cuandoContextoSinAutenticacion() {
        when(negocioRepository.existsByIdentificacionFiscalIgnoreCase("NIT-123456")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.crearNegocio(request, 10));

        assertEquals("El usuario especificado no tiene el rol de Propietario.", ex.getMessage());
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    void debeLanzarIllegalArgument_cuandoAutenticacionSinAuthorities() {
        Authentication auth = mock(Authentication.class);
        doReturn(null).when(auth).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(negocioRepository.existsByIdentificacionFiscalIgnoreCase("NIT-123456")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.crearNegocio(request, 10));

        assertEquals("El usuario especificado no tiene el rol de Propietario.", ex.getMessage());
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_CLIENTE", "PROVEEDOR", "ROLE_PROPIETARIOS"})
    void debeLanzarIllegalArgument_cuandoContextoConRolDistintoAPropietario(String rol) {
        autenticarCon(rol);
        when(negocioRepository.existsByIdentificacionFiscalIgnoreCase("NIT-123456")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.crearNegocio(request, 10));

        assertEquals("El usuario especificado no tiene el rol de Propietario.", ex.getMessage());
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    void debeLanzar409AntesDeValidarRol_cuandoIdentificacionDuplicadaYSinRol() {
        when(negocioRepository.existsByIdentificacionFiscalIgnoreCase("NIT-123456")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> negocioService.crearNegocio(request, 10));

        assertEquals("La identificación fiscal ya está en uso en la plataforma.", ex.getMessage());
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    void debeListarNegociosDelUsuario_cuandoContextoTieneRolPropietario() {
        autenticarCon("ROLE_PROPIETARIO");
        when(negocioRepository.findByPropietarioId(10)).thenReturn(List.of(negocio));

        List<Negocio> lista = negocioService.listarMisNegocios(10);

        assertEquals(List.of(negocio), lista);
    }

    @Test
    void debeRetornarListaVacia_cuandoPropietarioSinNegocios() {
        when(negocioRepository.findByPropietarioId(10)).thenReturn(List.of());

        List<Negocio> lista = negocioService.listarMisNegocios(10, true);

        assertTrue(lista.isEmpty());
    }

    @Test
    void debeLanzarIllegalArgument_cuandoListaSinRolPropietarioEnContexto() {
        autenticarCon("ROLE_PROVEEDOR");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> negocioService.listarMisNegocios(10));

        assertEquals("El usuario no tiene el rol de Propietario.", ex.getMessage());
        verify(negocioRepository, never()).findByPropietarioId(any());
    }
}
