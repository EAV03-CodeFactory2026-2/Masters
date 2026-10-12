package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.UsuarioCreateRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.entities.Rol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.repositories.RolRepository;
import com.udea.masters.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Rol rolCliente;
    private Rol rolPropietario;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        rolCliente = Rol.builder().idRol(1).nombreRol("Cliente").build();
        rolPropietario = Rol.builder().idRol(2).nombreRol("Propietario").build();

        usuario = Usuario.builder()
                .idUsuario(1)
                .nombre("Juan")
                .apellido("Perez")
                .correo("juan@udea.edu.co")
                .telefono("123456789")
                .contrasena("encodedPass")
                .estado("Activo")
                .roles(new HashSet<>(Set.of(rolCliente)))
                .build();
    }

    @Test
    void crearUsuarioExitoso() {
        UsuarioCreateRequest request = UsuarioCreateRequest.builder()
                .nombre("Juan")
                .apellido("Perez")
                .correo("juan@udea.edu.co")
                .telefono("123456789")
                .contrasena("pass123")
                .roles(Set.of("Cliente"))
                .build();

        when(usuarioRepository.existsByCorreo("juan@udea.edu.co")).thenReturn(false);
        when(rolRepository.findByNombreRol("Cliente")).thenReturn(Optional.of(rolCliente));
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        UsuarioResponse response = usuarioService.crearUsuario(request);

        assertNotNull(response);
        assertEquals("juan@udea.edu.co", response.getCorreo());
        assertTrue(response.getRoles().contains("Cliente"));
    }

    @Test
    void crearUsuarioFallaCuandoCorreoDuplicado() {
        UsuarioCreateRequest request = UsuarioCreateRequest.builder()
                .correo("juan@udea.edu.co")
                .build();

        when(usuarioRepository.existsByCorreo("juan@udea.edu.co")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.crearUsuario(request));
        assertTrue(ex.getMessage().contains("Ya existe un usuario con el correo"));
    }

    @Test
    void asignarRolAUsuarioExitoso() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRol("Propietario")).thenReturn(Optional.of(rolPropietario));

        usuarioService.asignarRolAUsuario(1, "Propietario");

        assertTrue(usuario.getRoles().contains(rolPropietario));
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void removerRolDeUsuarioExitoso() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRol("Cliente")).thenReturn(Optional.of(rolCliente));

        usuarioService.removerRolDeUsuario(1, "Cliente");

        assertFalse(usuario.getRoles().contains(rolCliente));
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void listarUsuariosPorRol() {
        when(usuarioRepository.findByRolesNombreRol("Cliente")).thenReturn(List.of(usuario));

        List<UsuarioResponse> list = usuarioService.listarPorRol("Cliente");

        assertEquals(1, list.size());
        assertEquals("juan@udea.edu.co", list.get(0).getCorreo());
    }
}
