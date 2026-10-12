package com.udea.masters.controllers;

import com.udea.masters.entities.Moneda;
import com.udea.masters.repositories.MonedaRepository;
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
class MonedaControllerTest {

    @Mock
    private MonedaRepository monedaRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MonedaController controller = new MonedaController(monedaRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listarMonedasRetorna200() throws Exception {
        Moneda cop = Moneda.builder().monedaCodigoIso("COP").nombreMoneda("Peso Colombiano").build();
        when(monedaRepository.findAll()).thenReturn(List.of(cop));

        mockMvc.perform(get("/api/v1/monedas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monedaCodigoIso").value("COP"))
                .andExpect(jsonPath("$[0].nombreMoneda").value("Peso Colombiano"));
    }
}
