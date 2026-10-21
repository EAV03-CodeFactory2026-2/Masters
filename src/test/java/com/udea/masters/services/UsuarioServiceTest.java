package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.entities.Rol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.RolRepository;
import com.udea.masters.repositories.UsuarioRepository;
import com.udea.masters.security.IAuthenticationFacade;
import com.udea.masters.services.strategy.IRolPerfilStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private IAuthenticationFacade authenticationFacade;

    @Mock
    private IRolPerfilStrategy rolPerfilStrategy;

    private UsuarioService usuarioService;

    private Rol rolCliente;
    private Rol rolPropietario;
    private Rol rolProveedor;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        rolCliente = Rol.builder().idRol(1).nombreRol("Cliente").build();
        rolPropietario = Rol.builder().idRol(2).nombreRol("Propietario").build();
        rolProveedor = Rol.builder().idRol(3).nombreRol("Proveedor").build();

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

    private UsuarioService servicioConStrategy() {
        return new UsuarioService(usuarioRepository, rolRepository, List.of(rolPerfilStrategy), authenticationFacade);
    }

    private Usuario usuarioConRoles(Integer id, Rol... roles) {
        return Usuario.builder()
                .idUsuario(id)
                .nombre("Usuario" + id)
                .correo("usuario" + id + "@udea.edu.co")
                .roles(new HashSet<>(Set.of(roles)))
                .build();
    }

    @Test
    void debeRetornarUsuarioMapeado_cuandoNoHayUsuarioAutenticado() {
        usuario.getRoles().add(rolPropietario);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);

        UsuarioResponse response = usuarioService.obtenerUsuarioPorId(1);

        assertEquals(1, response.getIdUsuario());
        assertEquals("Juan", response.getNombre());
        assertEquals("Perez", response.getApellido());
        assertEquals("juan@udea.edu.co", response.getCorreo());
        assertEquals("123456789", response.getTelefono());
        assertEquals("Activo", response.getEstado());
        assertEquals(Set.of("Cliente", "Propietario"), response.getRoles());
    }

    @Test
    void debeRetornarUsuario_cuandoConsultaSuPropioPerfil() {
        Usuario propietario = usuarioConRoles(5, rolPropietario);
        when(usuarioRepository.findById(5)).thenReturn(Optional.of(propietario));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(5);

        UsuarioResponse response = usuarioService.obtenerUsuarioPorId(5);

        assertEquals(5, response.getIdUsuario());
        assertEquals(Set.of("Propietario"), response.getRoles());
    }

    @Test
    void debeRetornarUsuario_cuandoOtroUsuarioEsProveedor() {
        Usuario proveedor = usuarioConRoles(6, rolProveedor);
        when(usuarioRepository.findById(6)).thenReturn(Optional.of(proveedor));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(99);

        UsuarioResponse response = usuarioService.obtenerUsuarioPorId(6);

        assertEquals(6, response.getIdUsuario());
    }

    @Test
    void debeRetornarUsuario_cuandoOtroUsuarioEsCliente() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(99);

        UsuarioResponse response = usuarioService.obtenerUsuarioPorId(1);

        assertEquals(Set.of("Cliente"), response.getRoles());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoConsultaOtroPropietario() {
        Usuario otroPropietario = usuarioConRoles(7, rolPropietario);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(otroPropietario));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(99);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> usuarioService.obtenerUsuarioPorId(7));

        assertEquals("No tienes permisos para consultar la información de otros propietarios.", ex.getMessage());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoUsuarioConsultadoNoTieneIdNiRoles() {
        Usuario sinDatos = Usuario.builder().nombre("Sin datos").roles(null).build();
        when(usuarioRepository.findById(8)).thenReturn(Optional.of(sinDatos));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(99);

        assertThrows(AccessDeniedException.class, () -> usuarioService.obtenerUsuarioPorId(8));
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoUsuarioNoExisteAlConsultar() {
        when(usuarioRepository.findById(404)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> usuarioService.obtenerUsuarioPorId(404));

        assertEquals("Usuario no encontrado con ID: 404", ex.getMessage());
        verifyNoInteractions(authenticationFacade);
    }

    @Test
    void debeRetornarRolesVacios_cuandoUsuarioNoTieneRolesYNoHayAutenticado() {
        Usuario sinRoles = Usuario.builder().idUsuario(9).roles(null).build();
        when(usuarioRepository.findById(9)).thenReturn(Optional.of(sinRoles));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);

        UsuarioResponse response = usuarioService.obtenerUsuarioPorId(9);

        assertNotNull(response.getRoles());
        assertTrue(response.getRoles().isEmpty());
    }

    @Test
    void debeListarTodos_cuandoNoHayUsuarioAutenticado() {
        Usuario propietario = usuarioConRoles(2, rolPropietario);
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario, propietario));

        List<UsuarioResponse> lista = usuarioService.listarUsuarios();

        assertEquals(2, lista.size());
    }

    @Test
    void debeOcultarOtrosPropietarios_cuandoListaUsuarioAutenticado() {
        Usuario propio = usuarioConRoles(2, rolPropietario);
        Usuario otroPropietario = usuarioConRoles(3, rolPropietario);
        Usuario proveedor = usuarioConRoles(4, rolProveedor);
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(2);
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario, propio, otroPropietario, proveedor));

        List<UsuarioResponse> lista = usuarioService.listarUsuarios();

        assertEquals(List.of(1, 2, 4), lista.stream().map(UsuarioResponse::getIdUsuario).toList());
    }

    @Test
    void debeListarSoloElPropio_cuandoFiltraPorRolPropietario() {
        Usuario propio = usuarioConRoles(2, rolPropietario);
        Usuario otroPropietario = usuarioConRoles(3, rolPropietario);
        Usuario sinId = Usuario.builder().roles(new HashSet<>(Set.of(rolPropietario))).build();
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(2);
        when(usuarioRepository.findByRolesNombreRol("propietario")).thenReturn(List.of(propio, otroPropietario, sinId));

        List<UsuarioResponse> lista = usuarioService.listarPorRol("propietario");

        assertEquals(1, lista.size());
        assertEquals(2, lista.get(0).getIdUsuario());
    }

    @Test
    void debeRetornarVacio_cuandoFiltraPorRolPropietarioSinUsuarioAutenticado() {
        Usuario propietario = usuarioConRoles(2, rolPropietario);
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);
        when(usuarioRepository.findByRolesNombreRol("Propietario")).thenReturn(List.of(propietario));

        List<UsuarioResponse> lista = usuarioService.listarPorRol("Propietario");

        assertTrue(lista.isEmpty());
    }

    @Test
    void debeAplicarVisibilidad_cuandoFiltraPorRolDistintoAPropietario() {
        Usuario proveedor = usuarioConRoles(4, rolProveedor);
        Usuario soloPropietario = usuarioConRoles(5, rolPropietario);
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(99);
        when(usuarioRepository.findByRolesNombreRol("Proveedor")).thenReturn(List.of(proveedor, soloPropietario));

        List<UsuarioResponse> lista = usuarioService.listarPorRol("Proveedor");

        assertEquals(1, lista.size());
        assertEquals(4, lista.get(0).getIdUsuario());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void debeLanzarReglaNegocio_cuandoNombreRolAAsignarNoSeIndica(String nombreRol) {
        RolAsignacionRequest request = RolAsignacionRequest.builder().nombreRol(nombreRol).build();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> usuarioService.asignarRolAUsuario(1, request));

        assertEquals("Debes indicar el rol a asignar.", ex.getMessage());
        verifyNoInteractions(usuarioRepository, rolRepository);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoRequestDeAsignacionEsNull() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> usuarioService.asignarRolAUsuario(1, (RolAsignacionRequest) null));

        assertEquals("Debes indicar el rol a asignar.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoUsuarioNoExisteAlAsignarRol() {
        when(usuarioRepository.findById(404)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> usuarioService.asignarRolAUsuario(404, "Propietario"));

        assertEquals("Usuario no encontrado con ID: 404", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoRolAAsignarNoExiste() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Administrador")).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> usuarioService.asignarRolAUsuario(1, "  Administrador  "));

        assertEquals("Rol no encontrado con nombre: Administrador", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoUsuarioYaTieneElRol() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("cliente")).thenReturn(Optional.of(rolCliente));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> usuarioService.asignarRolAUsuario(1, "cliente"));

        assertEquals("El usuario ya tiene asignado el rol Cliente.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeCrearPerfil_cuandoStrategySoportaElRolAsignado() {
        UsuarioService servicio = servicioConStrategy();
        RolAsignacionRequest request = RolAsignacionRequest.builder()
                .nombreRol("Proveedor")
                .tipoDocumento("CC")
                .numeroDocumento("123")
                .especialidad("Barbería")
                .build();
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Proveedor")).thenReturn(Optional.of(rolProveedor));
        when(rolPerfilStrategy.soporta("Proveedor")).thenReturn(true);

        servicio.asignarRolAUsuario(1, request);

        verify(rolPerfilStrategy).asignarPerfil(usuario, request);
        assertTrue(usuario.getRoles().contains(rolProveedor));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void debeAsignarRolSinPerfil_cuandoStrategyNoSoportaElRol() {
        UsuarioService servicio = servicioConStrategy();
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Propietario")).thenReturn(Optional.of(rolPropietario));
        when(rolPerfilStrategy.soporta("Propietario")).thenReturn(false);

        servicio.asignarRolAUsuario(1, "Propietario");

        verify(rolPerfilStrategy, never()).asignarPerfil(any(), any());
        assertTrue(usuario.getRoles().contains(rolPropietario));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void debeNoGuardarUsuario_cuandoStrategyRechazaElPerfil() {
        UsuarioService servicio = servicioConStrategy();
        RolAsignacionRequest request = RolAsignacionRequest.builder().nombreRol("Proveedor").build();
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Proveedor")).thenReturn(Optional.of(rolProveedor));
        when(rolPerfilStrategy.soporta("Proveedor")).thenReturn(true);
        doThrow(new ReglaNegocioException("Tipo de documento es obligatorio"))
                .when(rolPerfilStrategy).asignarPerfil(usuario, request);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.asignarRolAUsuario(1, request));

        assertEquals("Tipo de documento es obligatorio", ex.getMessage());
        assertFalse(usuario.getRoles().contains(rolProveedor));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoUsuarioNoExisteAlRemoverRol() {
        when(usuarioRepository.findById(404)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> usuarioService.removerRolDeUsuario(404, "Cliente"));

        assertEquals("Usuario no encontrado con ID: 404", ex.getMessage());
        verifyNoInteractions(rolRepository);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoRolARemoverNoExiste() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Administrador")).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> usuarioService.removerRolDeUsuario(1, "Administrador"));

        assertEquals("Rol no encontrado con nombre: Administrador", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoUsuarioNoTieneElRolARemover() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Proveedor")).thenReturn(Optional.of(rolProveedor));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> usuarioService.removerRolDeUsuario(1, "Proveedor"));

        assertEquals("El usuario no tiene asignado el rol Proveedor.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoSeRemueveElUnicoRol() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("cliente")).thenReturn(Optional.of(rolCliente));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> usuarioService.removerRolDeUsuario(1, "cliente"));

        assertEquals("No se puede eliminar el único rol del usuario.", ex.getMessage());
        assertTrue(usuario.getRoles().contains(rolCliente));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeRemoverPerfil_cuandoStrategySoportaElRolRemovido() {
        UsuarioService servicio = servicioConStrategy();
        usuario.getRoles().add(rolProveedor);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Proveedor")).thenReturn(Optional.of(rolProveedor));
        when(rolPerfilStrategy.soporta("Proveedor")).thenReturn(true);

        servicio.removerRolDeUsuario(1, "Proveedor");

        verify(rolPerfilStrategy).removerPerfil(1);
        assertFalse(usuario.getRoles().contains(rolProveedor));
        assertTrue(usuario.getRoles().contains(rolCliente));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void debeRemoverRolSinTocarPerfil_cuandoStrategyNoSoportaElRol() {
        UsuarioService servicio = servicioConStrategy();
        usuario.getRoles().add(rolPropietario);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreRolIgnoreCase("Propietario")).thenReturn(Optional.of(rolPropietario));
        when(rolPerfilStrategy.soporta("Propietario")).thenReturn(false);

        servicio.removerRolDeUsuario(1, "Propietario");

        verify(rolPerfilStrategy, never()).removerPerfil(anyInt());
        assertFalse(usuario.getRoles().contains(rolPropietario));
        verify(usuarioRepository).save(usuario);
    }
}

