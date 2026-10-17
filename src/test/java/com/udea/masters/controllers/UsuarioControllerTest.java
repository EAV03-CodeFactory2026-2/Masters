package com.udea.masters.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.masters.controllers.DTOs.RegistroResponse;
import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.services.IRegistroService;
import com.udea.masters.services.IUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
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

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UsuarioController controller = new UsuarioController(usuarioService, registroService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
                .andExpect(jsonPath("$.correo").value("maria@udea.edu.co"));
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
                .andExpect(jsonPath("$[0].correo").value("maria@udea.edu.co"));
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
}

