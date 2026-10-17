package com.udea.masters.controllers;

import com.udea.masters.entities.ModalidadServicio;
import com.udea.masters.services.IModalidadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ModalidadControllerTest {

    @Mock
    private IModalidadService modalidadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ModalidadController controller = new ModalidadController(modalidadService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listarModalidadesRetorna200() throws Exception {
        ModalidadServicio presencial = ModalidadServicio.builder().idModalidad(1).nombreModalidad("Presencial").build();
        when(modalidadService.listarModalidades()).thenReturn(List.of(presencial));

        mockMvc.perform(get("/api/v1/modalidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idModalidad").value(1))
                .andExpect(jsonPath("$[0].nombreModalidad").value("Presencial"));
    }
}

