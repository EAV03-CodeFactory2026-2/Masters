package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RegistroResponse;
import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.Rol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.RolRepository;
import com.udea.masters.repositories.UsuarioRepository;
import com.udea.masters.security.IJwtService;
import com.udea.masters.services.strategy.IRegistroRolStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    private static final String MENSAJE_ROL = "Debes indicar el rol con el que te registras.";
    private static final String MENSAJE_CONTRASENA = "La contraseña debe tener entre 8 y 64 caracteres, una mayúscula, una minúscula, un número y un carácter especial.";

    @Mock
    private IRegistroRolStrategy strategy;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IJwtService jwtService;

    private RegistroService registroService;
    private RegistroUsuarioRequest request;
    private Rol rolCliente;

    @BeforeEach
    void setUp() {
        registroService = new RegistroService(List.of(strategy), usuarioRepository, rolRepository, passwordEncoder, jwtService);
        rolCliente = Rol.builder().idRol(1).nombreRol("Cliente").build();
        request = RegistroUsuarioRequest.builder()
                .rol("Cliente")
                .correo("  Laura@Correo.COM ")
                .contrasena("Reservas#2024")
                .nombre(" Laura ")
                .apellido(" Gómez ")
                .telefono(" 3001234567 ")
                .build();
    }

    private void prepararRegistroExitoso() {
        when(strategy.soporta("Cliente")).thenReturn(true);
        when(usuarioRepository.existsByCorreoIgnoreCase("Laura@Correo.COM")).thenReturn(false);
        when(strategy.getNombreRol()).thenReturn("Cliente");
        when(passwordEncoder.encode(request.getContrasena())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(7);
            return u;
        });
        when(jwtService.generateToken(7, "laura@correo.com", List.of("Cliente"), "Cliente")).thenReturn("jwt-token");
    }

    @Test
    void debeLanzarReglaNegocio_cuandoRequestEsNull() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> registroService.registrar(null));

        assertEquals(MENSAJE_ROL, ex.getMessage());
        verifyNoInteractions(strategy, usuarioRepository, rolRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void debeLanzarReglaNegocio_cuandoRolNoSeIndica(String rol) {
        request.setRol(rol);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> registroService.registrar(request));

        assertEquals(MENSAJE_ROL, ex.getMessage());
        verifyNoInteractions(strategy, usuarioRepository);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoNingunaStrategySoportaElRol() {
        request.setRol("Administrador");
        when(strategy.soporta("Administrador")).thenReturn(false);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> registroService.registrar(request));

        // H-05: Mensaje específico para rol no soportado
        assertEquals("El rol indicado no es válido.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void debeLanzarReglaNegocio_cuandoCorreoNoSeIndica(String correo) {
        request.setCorreo(correo);
        when(strategy.soporta("Cliente")).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> registroService.registrar(request));

        assertEquals("El correo es obligatorio", ex.getMessage());
        verify(usuarioRepository, never()).existsByCorreoIgnoreCase(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarRecursoDuplicado_cuandoCorreoYaExiste() {
        when(strategy.soporta("Cliente")).thenReturn(true);
        when(usuarioRepository.existsByCorreoIgnoreCase("Laura@Correo.COM")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class, () -> registroService.registrar(request));

        assertTrue(ex.getMessage().contains("Ya existe una cuenta con este correo"));
        verify(strategy, never()).validar(any());
        verify(usuarioRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "Ab1#xyz",
            "reservas#2024",
            "RESERVAS#2024",
            "Reservas#Dos",
            "Reservas2024",
            "Aa1#aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    void debeLanzarReglaNegocio_cuandoContrasenaNoCumplePatron(String contrasena) {
        request.setContrasena(contrasena);
        when(strategy.soporta("Cliente")).thenReturn(true);
        when(usuarioRepository.existsByCorreoIgnoreCase("Laura@Correo.COM")).thenReturn(false);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> registroService.registrar(request));

        assertEquals(MENSAJE_CONTRASENA, ex.getMessage());
        verify(strategy, never()).validar(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeAceptarContrasena_cuandoTieneLongitudMaxima() {
        String contrasena = "Aa1#" + "a".repeat(60);
        request.setContrasena(contrasena);
        prepararRegistroExitoso();
        when(rolRepository.findByNombreRol("Cliente")).thenReturn(Optional.of(rolCliente));

        RegistroResponse response = registroService.registrar(request);

        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void debePropagarExcepcion_cuandoStrategyRechazaLosDatos() {
        when(strategy.soporta("Cliente")).thenReturn(true);
        when(usuarioRepository.existsByCorreoIgnoreCase("Laura@Correo.COM")).thenReturn(false);
        doThrow(new ReglaNegocioException("Teléfono de contacto es obligatorio")).when(strategy).validar(request);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> registroService.registrar(request));

        assertEquals("Teléfono de contacto es obligatorio", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(strategy, never()).procesarRegistro(any(), any());
    }

    @Test
    void debeRegistrarUsuario_cuandoRolExiste() {
        prepararRegistroExitoso();
        when(rolRepository.findByNombreRol("Cliente")).thenReturn(Optional.of(rolCliente));

        RegistroResponse response = registroService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("Laura", guardado.getNombre());
        assertEquals("Gómez", guardado.getApellido());
        assertEquals("laura@correo.com", guardado.getCorreo());
        assertEquals("3001234567", guardado.getTelefono());
        assertEquals("hash", guardado.getContrasena());
        assertEquals("Activo", guardado.getEstado());
        assertEquals(1, guardado.getRoles().size());
        assertTrue(guardado.getRoles().contains(rolCliente));

        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTipoToken());
        assertEquals(7, response.getIdUsuario());
        assertEquals("Laura", response.getNombre());
        assertEquals("Gómez", response.getApellido());
        assertEquals("laura@correo.com", response.getCorreo());
        assertEquals("Cliente", response.getRolActivo());

        verify(strategy).validar(request);
        verify(strategy).procesarRegistro(request, guardado);
        verify(rolRepository, never()).save(any());
    }

    @Test
    void debeCrearRol_cuandoRolNoExiste() {
        prepararRegistroExitoso();
        when(rolRepository.findByNombreRol("Cliente")).thenReturn(Optional.empty());
        when(rolRepository.save(any(Rol.class))).thenAnswer(inv -> {
            Rol r = inv.getArgument(0);
            r.setIdRol(3);
            return r;
        });

        registroService.registrar(request);

        ArgumentCaptor<Rol> rolCaptor = ArgumentCaptor.forClass(Rol.class);
        verify(rolRepository).save(rolCaptor.capture());
        assertEquals("Cliente", rolCaptor.getValue().getNombreRol());

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertTrue(usuarioCaptor.getValue().getRoles().contains(rolCaptor.getValue()));
        verify(strategy).procesarRegistro(same(request), any(Usuario.class));
    }

    @Test
    void debeAsignarCadenasVacias_cuandoNombreApellidoYTelefonoSonNull() {
        request.setNombre(null);
        request.setApellido(null);
        request.setTelefono(null);
        prepararRegistroExitoso();
        when(rolRepository.findByNombreRol("Cliente")).thenReturn(Optional.of(rolCliente));

        RegistroResponse response = registroService.registrar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("", captor.getValue().getNombre());
        assertEquals("", captor.getValue().getApellido());
        assertEquals("", captor.getValue().getTelefono());
        assertEquals("", response.getNombre());
        assertEquals("", response.getApellido());
    }
}
