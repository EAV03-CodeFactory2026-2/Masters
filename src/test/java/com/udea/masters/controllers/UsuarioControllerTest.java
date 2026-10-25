package com.udea.masters.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.masters.controllers.DTOs.RegistroResponse;
import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.exceptions.GlobalExceptionHandler;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.security.IAuthenticationFacade;
import com.udea.masters.services.IRegistroService;
import com.udea.masters.services.IUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private IUsuarioService usuarioService;

    @Mock
    private IRegistroService registroService;

    @Mock
    private IAuthenticationFacade authenticationFacade;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UsuarioController controller = new UsuarioController(usuarioService, registroService, authenticationFacade);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registrarUsuarioRetorna201() throws Exception {
        RegistroUsuarioRequest request = RegistroUsuarioRequest.builder()
                .nombre("Maria")
                .apellido("Lopez")
                .correo("maria@udea.edu.co")
                .telefono("3001234567")
                .contrasena("Password123!")
                .rol("Proveedor")
                .build();

        RegistroResponse response = RegistroResponse.builder()
                .idUsuario(1)
                .nombre("Maria")
                .apellido("Lopez")
                .correo("maria@udea.edu.co")
                .rolActivo("Proveedor")
                .token("jwt-token")
                .tipoToken("Bearer")
                .build();

        when(registroService.registrar(any(RegistroUsuarioRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.correo").value("maria@udea.edu.co"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    void listarUsuariosRetorna200() throws Exception {
        UsuarioResponse response = UsuarioResponse.builder()
                .idUsuario(1)
                .nombre("Maria")
                .correo("maria@udea.edu.co")
                .build();

        when(usuarioService.listarUsuarios()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("maria@udea.edu.co"))
                .andExpect(jsonPath("$[0].contrasena").doesNotExist());
    }

    @Test
    void asignarRolRetorna200() throws Exception {
        RolAsignacionRequest request = RolAsignacionRequest.builder()
                .nombreRol("Propietario")
                .build();

        doNothing().when(usuarioService).asignarRolAUsuario(eq(1), any(RolAsignacionRequest.class));

        mockMvc.perform(post("/api/v1/usuarios/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Rol asignado correctamente."));
    }

    @Test
    void usuarioYRegistroRequestNoExponenContrasenaEnJsonNiToString() throws Exception {
        com.udea.masters.entities.Usuario usuario = com.udea.masters.entities.Usuario.builder()
                .idUsuario(1)
                .nombre("Maria")
                .apellido("Lopez")
                .correo("maria@udea.edu.co")
                .telefono("3001234567")
                .contrasena("SecretoSuperSeguro123!")
                .build();

        RegistroUsuarioRequest request = RegistroUsuarioRequest.builder()
                .correo("maria@udea.edu.co")
                .contrasena("SecretoSuperSeguro123!")
                .build();

        String json = objectMapper.writeValueAsString(usuario);
        org.junit.jupiter.api.Assertions.assertFalse(json.contains("contrasena"));
        org.junit.jupiter.api.Assertions.assertFalse(json.contains("SecretoSuperSeguro123!"));
        org.junit.jupiter.api.Assertions.assertFalse(usuario.toString().contains("SecretoSuperSeguro123!"));
        org.junit.jupiter.api.Assertions.assertFalse(request.toString().contains("SecretoSuperSeguro123!"));
    }

    @Test
    void soportaAliasDeRutaYCamposPluralesYEndpointPerfil() throws Exception {
        String payloadPlural = """
                {
                  "correo": "laura@correo.com",
                  "contrasena": "Reservas#2024",
                  "nombres": "Laura",
                  "apellidos": "Gómez",
                  "telefono": "3001234567",
                  "rol": "Cliente"
                }
                """;

        RegistroResponse response = RegistroResponse.builder()
                .idUsuario(10)
                .nombre("Laura")
                .apellido("Gómez")
                .correo("laura@correo.com")
                .rolActivo("Cliente")
                .token("jwt-token")
                .build();

        when(registroService.registrar(org.mockito.ArgumentMatchers.argThat(req ->
                "Laura".equals(req.getNombre()) && "Gómez".equals(req.getApellido())
        ))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadPlural))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(10))
                .andExpect(jsonPath("$.rolActivo").value("Cliente"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());

        UsuarioResponse perfilResponse = UsuarioResponse.builder()
                .idUsuario(10)
                .nombre("Laura")
                .apellido("Gómez")
                .correo("laura@correo.com")
                .build();

        // H-03: el perfil se obtiene desde el facade, no desde el Principal
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(10);
        when(usuarioService.obtenerUsuarioPorId(10)).thenReturn(perfilResponse);

        mockMvc.perform(get("/api/v1/usuarios/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(10))
                .andExpect(jsonPath("$.correo").value("laura@correo.com"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    void debeRetornar409_cuandoRegistroTieneCorreoDuplicado() throws Exception {
        RegistroUsuarioRequest request = RegistroUsuarioRequest.builder()
                .correo("maria@udea.edu.co")
                .contrasena("Password123!")
                .rol("Cliente")
                .build();
        when(registroService.registrar(any(RegistroUsuarioRequest.class)))
                .thenThrow(new RecursoDuplicadoException("Ya existe una cuenta con este correo."));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta con este correo."));
    }

    @Test
    void debeRetornar400_cuandoRegistroIncumpleReglaDeNegocio() throws Exception {
        when(registroService.registrar(any(RegistroUsuarioRequest.class)))
                .thenThrow(new ReglaNegocioException("Debes indicar el rol con el que te registras."));

        mockMvc.perform(post("/api/v1/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"maria@udea.edu.co\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar el rol con el que te registras."));
    }

    @Test
    void debeRetornar401_cuandoNoHayUsuarioAutenticadoEnPerfil() throws Exception {
        // H-03: Sin autenticación válida el facade retorna null → 401
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);

        mockMvc.perform(get("/api/v1/usuarios/perfil"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void debeRetornar200_cuandoSeConsultaUsuarioPorId() throws Exception {
        UsuarioResponse response = UsuarioResponse.builder()
                .idUsuario(5)
                .nombre("Carlos")
                .correo("carlos@udea.edu.co")
                .roles(Set.of("Proveedor"))
                .build();
        when(usuarioService.obtenerUsuarioPorId(5)).thenReturn(response);

        mockMvc.perform(get("/api/v1/usuarios/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(5))
                .andExpect(jsonPath("$.roles[0]").value("Proveedor"));
    }

    @Test
    void debeRetornar404_cuandoUsuarioConsultadoNoExiste() throws Exception {
        when(usuarioService.obtenerUsuarioPorId(404))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con ID: 404"));

        mockMvc.perform(get("/api/v1/usuarios/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado con ID: 404"));
    }

    @Test
    void debeRetornar403_cuandoServicioDeniegaLaConsulta() throws Exception {
        when(usuarioService.obtenerUsuarioPorId(7))
                .thenThrow(new AccessDeniedException("No tienes permisos para consultar la información de otros propietarios."));

        mockMvc.perform(get("/api/v1/usuarios/7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void debeRetornar400_cuandoIdNoEsNumerico() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/abc"))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).obtenerUsuarioPorId(anyInt());
    }

    @Test
    void debeRetornar200_cuandoListaUsuariosPorRol() throws Exception {
        UsuarioResponse proveedor = UsuarioResponse.builder().idUsuario(4).correo("prov@udea.edu.co").build();
        when(usuarioService.listarPorRol("Proveedor")).thenReturn(List.of(proveedor));

        mockMvc.perform(get("/api/v1/usuarios/rol/Proveedor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idUsuario").value(4));
    }

    @Test
    void debeRetornar200ConListaVacia_cuandoNingunUsuarioTieneElRol() throws Exception {
        when(usuarioService.listarPorRol("Cliente")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/usuarios/rol/Cliente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nombreRol\":\"\"}", "{\"nombreRol\":\"   \"}"})
    void debeRetornar400_cuandoNombreRolAAsignarEsVacio(String payload) throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El nombre del rol no puede estar vacío."));

        verifyNoInteractions(usuarioService);
    }

    @Test
    void debeRetornar400_cuandoUsuarioYaTieneElRolAsignado() throws Exception {
        doThrow(new ReglaNegocioException("El usuario ya tiene asignado el rol Cliente."))
                .when(usuarioService).asignarRolAUsuario(eq(1), any(RolAsignacionRequest.class));

        mockMvc.perform(post("/api/v1/usuarios/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreRol\":\"Cliente\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El usuario ya tiene asignado el rol Cliente."));
    }

    @Test
    void debeRetornar409_cuandoDocumentoDelPerfilYaExiste() throws Exception {
        doThrow(new RecursoDuplicadoException("Ya existe un proveedor registrado con este documento."))
                .when(usuarioService).asignarRolAUsuario(eq(1), any(RolAsignacionRequest.class));

        mockMvc.perform(post("/api/v1/usuarios/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreRol\":\"Proveedor\",\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un proveedor registrado con este documento."));
    }

    @Test
    void debeRetornar200_cuandoRemueveRol() throws Exception {
        doNothing().when(usuarioService).removerRolDeUsuario(1, "Proveedor");

        mockMvc.perform(delete("/api/v1/usuarios/1/roles/Proveedor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Rol removido correctamente."));

        verify(usuarioService).removerRolDeUsuario(1, "Proveedor");
    }

    @Test
    void debeRetornar400_cuandoSeRemueveElUnicoRol() throws Exception {
        doThrow(new ReglaNegocioException("No se puede eliminar el único rol del usuario."))
                .when(usuarioService).removerRolDeUsuario(1, "Cliente");

        mockMvc.perform(delete("/api/v1/usuarios/1/roles/Cliente"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar el único rol del usuario."));
    }

    @Test
    void debeRetornar404_cuandoSeRemueveRolDeUsuarioInexistente() throws Exception {
        doThrow(new RecursoNoEncontradoException("Usuario no encontrado con ID: 404"))
                .when(usuarioService).removerRolDeUsuario(404, "Cliente");

        mockMvc.perform(delete("/api/v1/usuarios/404/roles/Cliente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado con ID: 404"));
    }
}
