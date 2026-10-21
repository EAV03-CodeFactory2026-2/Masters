package com.udea.masters.controllers;

import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.services.ITipoDocumentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TipoDocumentoControllerTest {

    @Mock
    private ITipoDocumentoService tipoDocumentoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TipoDocumentoController controller = new TipoDocumentoController(tipoDocumentoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void debeRetornar200ConCatalogo_cuandoExistenTiposDocumento() throws Exception {
        TipoDocumento cedula = TipoDocumento.builder().idTipoDocumento(1).codigo("CC").nombre("Cédula de ciudadanía").build();
        TipoDocumento pasaporte = TipoDocumento.builder().idTipoDocumento(2).codigo("PA").nombre("Pasaporte").build();
        when(tipoDocumentoService.listarTiposDocumento()).thenReturn(List.of(cedula, pasaporte));

        mockMvc.perform(get("/api/v1/tipos-documento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].idTipoDocumento").value(1))
                .andExpect(jsonPath("$[0].codigo").value("CC"))
                .andExpect(jsonPath("$[1].codigo").value("PA"))
                .andExpect(jsonPath("$[1].nombre").value("Pasaporte"));

        verify(tipoDocumentoService).listarTiposDocumento();
    }

    @Test
    void debeRetornar200ConListaVacia_cuandoNoHayTiposDocumento() throws Exception {
        when(tipoDocumentoService.listarTiposDocumento()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tipos-documento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
