package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.RecursoCreateRequest;
import com.udea.masters.controllers.DTOs.RecursoRegistradoResponse;
import com.udea.masters.controllers.DTOs.RecursoResponse;
import com.udea.masters.controllers.DTOs.RecursosNegocioResponse;
import com.udea.masters.exceptions.AccesoDenegadoException;
import com.udea.masters.exceptions.GlobalExceptionHandler;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.services.IRecursoService;
import com.udea.masters.services.RecursoService;
import com.udea.masters.services.RecursoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecursoControllerTest {

    private static final String URL = "/api/v1/negocios/5/recursos";
    private static final String BODY = """
            {"nombre": "Camilla de masajes", "tipo": "Equipo", "cantidad": 3, "descripcion": "Plegable"}
            """;

    @Mock
    private IRecursoService recursoService;

    private RecursoController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new RecursoController(recursoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RecursoResponse recurso() {
        return RecursoResponse.builder()
                .idRecurso(77)
                .negocioId(5)
                .nombre("Camilla de masajes")
                .tipo("Equipo")
                .cantidad(3)
                .descripcion("Plegable")
                .estado("Activo")
                .build();
    }

    @Test
    void debeRetornar201_cuandoRecursoRegistrado() throws Exception {
        when(recursoService.registrarRecurso(eq(5), any(RecursoCreateRequest.class)))
                .thenReturn(new RecursoRegistradoResponse(RecursoService.MSG_REGISTRADO, recurso()));
        ArgumentCaptor<RecursoCreateRequest> captor = ArgumentCaptor.forClass(RecursoCreateRequest.class);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value(RecursoService.MSG_REGISTRADO))
                .andExpect(jsonPath("$.recurso.idRecurso").value(77))
                .andExpect(jsonPath("$.recurso.negocioId").value(5))
                .andExpect(jsonPath("$.recurso.tipo").value("Equipo"))
                .andExpect(jsonPath("$.recurso.estado").value("Activo"));

        verify(recursoService).registrarRecurso(eq(5), captor.capture());
        assertEquals("Camilla de masajes", captor.getValue().getNombre());
        assertEquals(3, captor.getValue().getCantidad());
    }

    @Test
    void debeDelegarRequestNulo_cuandoPeticionSinCuerpo() throws Exception {
        when(recursoService.registrarRecurso(eq(5), isNull()))
                .thenThrow(new ReglaNegocioException(RecursoValidator.MSG_NOMBRE_OBLIGATORIO));

        mockMvc.perform(post(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value(RecursoValidator.MSG_NOMBRE_OBLIGATORIO));
    }

    static Stream<Arguments> erroresDelServicio() {
        return Stream.of(
                Arguments.of(new ReglaNegocioException(RecursoValidator.MSG_TIPO_INVALIDO), 400),
                Arguments.of(new AccesoDenegadoException(RecursoService.MSG_SIN_PERMISO_NEGOCIO), 403),
                Arguments.of(new RecursoNoEncontradoException(RecursoService.MSG_NEGOCIO_NO_ENCONTRADO), 404),
                Arguments.of(new RecursoDuplicadoException(RecursoService.MSG_NOMBRE_DUPLICADO), 409)
        );
    }

    @ParameterizedTest
    @MethodSource("erroresDelServicio")
    void debeRetornarCodigoDeError_cuandoRegistroFalla(RuntimeException error, int codigo) throws Exception {
        when(recursoService.registrarRecurso(eq(5), any(RecursoCreateRequest.class))).thenThrow(error);

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().is(codigo))
                .andExpect(jsonPath("$.status").value(codigo))
                .andExpect(jsonPath("$.error").value(error.getMessage()))
                .andExpect(jsonPath("$.mensaje").value(error.getMessage()));
    }

    @Test
    void debeRetornar200ConRecursos_cuandoListaNegocio() throws Exception {
        when(recursoService.listarRecursos(5))
                .thenReturn(new RecursosNegocioResponse(RecursoService.MSG_LISTA, List.of(recurso())));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(RecursoService.MSG_LISTA))
                .andExpect(jsonPath("$.recursos[0].nombre").value("Camilla de masajes"))
                .andExpect(jsonPath("$.recursos[0].cantidad").value(3));
    }

    @Test
    void debeRetornar200ConListaVacia_cuandoNegocioSinRecursos() throws Exception {
        when(recursoService.listarRecursos(5))
                .thenReturn(new RecursosNegocioResponse(RecursoService.MSG_LISTA_VACIA, List.of()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(RecursoService.MSG_LISTA_VACIA))
                .andExpect(jsonPath("$.recursos").isEmpty());
    }

    static Stream<Arguments> erroresDeListado() {
        return Stream.of(
                Arguments.of(new AccesoDenegadoException(RecursoService.MSG_SOLO_PROPIETARIO), 403),
                Arguments.of(new RecursoNoEncontradoException(RecursoService.MSG_NEGOCIO_NO_ENCONTRADO), 404)
        );
    }

    @ParameterizedTest
    @MethodSource("erroresDeListado")
    void debeRetornarCodigoDeError_cuandoListadoFalla(RuntimeException error, int codigo) throws Exception {
        when(recursoService.listarRecursos(5)).thenThrow(error);

        mockMvc.perform(get(URL))
                .andExpect(status().is(codigo))
                .andExpect(jsonPath("$.mensaje").value(error.getMessage()));
    }

    @Test
    void debeRetornarRespuestaDelServicio_cuandoSeInvocaDirectamente() {
        RecursosNegocioResponse esperado = new RecursosNegocioResponse(RecursoService.MSG_LISTA, List.of(recurso()));
        when(recursoService.listarRecursos(5)).thenReturn(esperado);

        ResponseEntity<RecursosNegocioResponse> respuesta = controller.listarRecursos(5);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(esperado, respuesta.getBody());
    }
}
