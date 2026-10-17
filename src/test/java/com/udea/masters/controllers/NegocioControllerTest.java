package com.udea.masters.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.masters.controllers.DTOs.NegocioCreateRequest;
import com.udea.masters.entities.Negocio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.services.INegocioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NegocioControllerTest {

    @Mock
    private INegocioService negocioService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Principal principal = () -> "10";

    @BeforeEach
    void setUp() {
        NegocioController controller = new NegocioController(negocioService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    private NegocioCreateRequest requestValido() {
        return NegocioCreateRequest.builder()
                .nombreNegocio("Peluquería Central")
                .direccionNegocio("Calle 10 # 20-30")
                .telefono("3001234567")
                .identificacionFiscal("NIT-123456")
                .monedaCodigoIso("COP")
                .build();
    }

    @Test
    void crearNegocioRetorna201() throws Exception {
        Negocio negocio = Negocio.builder().idNegocio(1).nombreNegocio("Peluquería Central").build();
        when(negocioService.crearNegocio(any(NegocioCreateRequest.class), eq(10))).thenReturn(negocio);

        mockMvc.perform(post("/api/v1/negocios")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idNegocio").value(1))
                .andExpect(jsonPath("$.mensaje").value("Negocio creado y activado exitosamente."));
    }

    @Test
    void crearNegocioRetorna409CuandoIdentificacionFiscalDuplicada() throws Exception {
        when(negocioService.crearNegocio(any(NegocioCreateRequest.class), eq(10)))
                .thenThrow(new RecursoDuplicadoException("La identificación fiscal ya está en uso en la plataforma."));

        mockMvc.perform(post("/api/v1/negocios")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("La identificación fiscal ya está en uso en la plataforma."));
    }

    @Test
    void listarMisNegociosRetorna200() throws Exception {
        Negocio negocio = Negocio.builder().idNegocio(1).nombreNegocio("Peluquería Central").build();
        when(negocioService.listarMisNegocios(10)).thenReturn(List.of(negocio));

        mockMvc.perform(get("/api/v1/negocios")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idNegocio").value(1));
    }
}
