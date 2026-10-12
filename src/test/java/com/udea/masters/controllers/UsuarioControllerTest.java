package com.udea.masters.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.controllers.DTOs.UsuarioCreateRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private IUsuarioService usuarioService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UsuarioController controller = new UsuarioController(usuarioService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void crearUsuarioRetorna201() throws Exception {
        UsuarioCreateRequest request = UsuarioCreateRequest.builder()
                .nombre("Maria")
                .apellido("Lopez")
                .correo("maria@udea.edu.co")
                .telefono("3001234567")
                .contrasena("password")
                .roles(Set.of("Proveedor"))
                .build();

        UsuarioResponse response = UsuarioResponse.builder()
                .idUsuario(1)
                .nombre("Maria")
                .apellido("Lopez")
                .correo("maria@udea.edu.co")
                .roles(Set.of("Proveedor"))
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/usuarios")
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

        doNothing().when(usuarioService).asignarRolAUsuario(1, "Propietario");

        mockMvc.perform(post("/api/v1/usuarios/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Rol asignado correctamente."));
    }
}
