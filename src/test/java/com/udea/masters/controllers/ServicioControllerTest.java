package com.udea.masters.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.masters.controllers.DTOs.ServicioCreateRequest;
import com.udea.masters.entities.Servicio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.security.IAuthenticationFacade;
import com.udea.masters.services.IServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServicioControllerTest {

    @Mock
    private IServicioService servicioService;

    @Mock
    private IAuthenticationFacade authenticationFacade;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ServicioController controller = new ServicioController(servicioService, authenticationFacade);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    private ServicioCreateRequest requestValido() {
        return ServicioCreateRequest.builder()
                .negocioId(10)
                .modalidadId(1)
                .nombre("Corte de cabello")
                .descripcion("Corte clásico")
                .duracionMinutos(30)
                .precio(new BigDecimal("15.50"))
                .build();
    }

    @Test
    void crearServicioRetorna201() throws Exception {
        Servicio servicio = Servicio.builder()
                .idServicio(1)
                .nombre("Corte de cabello")
                .estado("No Asignado")
                .build();

        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(1);
        when(servicioService.crearServicio(any(ServicioCreateRequest.class), eq(1))).thenReturn(servicio);

        mockMvc.perform(post("/api/v1/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idServicio").value(1))
                .andExpect(jsonPath("$.estado").value("No Asignado"));
    }

    @Test
    void crearServicioRetorna409CuandoNombreDuplicado() throws Exception {
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(1);
        when(servicioService.crearServicio(any(ServicioCreateRequest.class), eq(1)))
                .thenThrow(new RecursoDuplicadoException("Ya existe un servicio con ese nombre en este negocio."));

        mockMvc.perform(post("/api/v1/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un servicio con ese nombre en este negocio."));
    }

    @Test
    void listarServiciosRetorna200() throws Exception {
        Servicio servicio = Servicio.builder()
                .idServicio(1)
                .nombre("Corte de cabello")
                .build();

        when(servicioService.listarServiciosPorNegocio(10)).thenReturn(List.of(servicio));

        mockMvc.perform(get("/api/v1/servicios/negocio/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Corte de cabello"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "El usuario no tiene el rol de Propietario.",
            "Negocio no encontrado.",
            "No tienes permisos para agregar servicios a este negocio."
    })
    void debeRetornar400_cuandoServicioRechazaCreacion(String mensaje) throws Exception {
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(1);
        when(servicioService.crearServicio(any(ServicioCreateRequest.class), eq(1)))
                .thenThrow(new IllegalArgumentException(mensaje));

        mockMvc.perform(post("/api/v1/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(mensaje));
    }

    @Test
    void debeRetornar401_cuandoNoHayUsuarioAutenticado() throws Exception {
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);

        mockMvc.perform(post("/api/v1/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No autenticado o token inválido."));
    }

    static Stream<ServicioCreateRequest> requestsInvalidos() {
        ServicioCreateRequest sinNegocio = ServicioCreateRequest.builder().modalidadId(1).nombre("Corte")
                .descripcion("Clásico").duracionMinutos(30).precio(new BigDecimal("10.00")).build();
        ServicioCreateRequest duracionCero = ServicioCreateRequest.builder().negocioId(10).modalidadId(1)
                .nombre("Corte").descripcion("Clásico").duracionMinutos(0).precio(new BigDecimal("10.00")).build();
        ServicioCreateRequest precioCero = ServicioCreateRequest.builder().negocioId(10).modalidadId(1)
                .nombre("Corte").descripcion("Clásico").duracionMinutos(30).precio(BigDecimal.ZERO).build();
        ServicioCreateRequest nombreVacio = ServicioCreateRequest.builder().negocioId(10).modalidadId(1)
                .nombre(" ").descripcion("Clásico").duracionMinutos(30).precio(new BigDecimal("10.00")).build();
        return Stream.of(sinNegocio, duracionCero, precioCero, nombreVacio);
    }

    @ParameterizedTest
    @MethodSource("requestsInvalidos")
    void debeRetornar400SinInvocarServicio_cuandoRequestInvalido(ServicioCreateRequest request) throws Exception {
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(servicioService);
    }

    @Test
    void debeRetornar200ConListaVacia_cuandoNegocioSinServicios() throws Exception {
        when(servicioService.listarServiciosPorNegocio(10)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/servicios/negocio/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
