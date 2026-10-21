package com.udea.masters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void debeRetornarEstadoUp_cuandoSeInvocaDirectamente() {
        ResponseEntity<Map<String, String>> respuesta = controller.health();

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(Map.of("status", "UP", "service", "Masters"), respuesta.getBody());
    }

    @Test
    void debeRetornar200_cuandoSeConsultaEndpointHealth() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Masters"));
    }
}
