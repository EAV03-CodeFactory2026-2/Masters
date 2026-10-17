package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.entities.Rol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.repositories.RolRepository;
import com.udea.masters.repositories.UsuarioRepository;
import com.udea.masters.security.IAuthenticationFacade;
import com.udea.masters.services.strategy.IRolPerfilStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private IAuthenticationFacade authenticationFacade;

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

        List<IRolPerfilStrategy> strategies = List.of();
        usuarioService = new UsuarioService(usuarioRepository, rolRepository, strategies, authenticationFacade);
    }

    @Test
    void asignarRolAUsuarioExitoso() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Propietario")).thenReturn(Optional.of(rolPropietario));

        usuarioService.asignarRolAUsuario(1, "Propietario");

        assertTrue(usuario.getRoles().contains(rolPropietario));
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void removerRolDeUsuarioExitoso() {
        usuario.getRoles().add(rolPropietario);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Cliente")).thenReturn(Optional.of(rolCliente));

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

