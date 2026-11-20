package com.udea.masters.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.masters.controllers.DTOs.CrearFranjaRequest;
import com.udea.masters.controllers.DTOs.FranjaServicioResponse;
import com.udea.masters.exceptions.AccesoDenegadoException;
import com.udea.masters.exceptions.GlobalExceptionHandler;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.services.FranjaServicioService;
import com.udea.masters.services.IFranjaServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FranjaServicioControllerTest {

    @Mock
    private IFranjaServicioService franjaServicioService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        FranjaServicioController controller = new FranjaServicioController(franjaServicioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void debeRetornar201ConFranjaCreada_cuandoProveedorCreaFranjaValida() throws Exception {
        // HU-0046/CA-01
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .servicioId(10)
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        FranjaServicioResponse respuesta = FranjaServicioResponse.builder()
                .mensaje(FranjaServicioService.MSG_FRANJA_CREADA)
                .idFranja(100)
                .servicioId(10)
                .nombreServicio("Corte clásico")
                .duracionMinutos(30)
                .proveedorId(7)
                .nombreProveedor("Carlos Ruiz")
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .horaFin("09:30")
                .creadaPorId(7)
                .nombreCreadaPor("Carlos Ruiz")
                .build();
        when(franjaServicioService.crearFranja(eq(10), any(CrearFranjaRequest.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/franjas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value(FranjaServicioService.MSG_FRANJA_CREADA))
                .andExpect(jsonPath("$.idFranja").value(100))
                .andExpect(jsonPath("$.servicioId").value(10))
                .andExpect(jsonPath("$.proveedorId").value(7))
                .andExpect(jsonPath("$.diaSemana").value("LUNES"))
                .andExpect(jsonPath("$.horaInicio").value("09:00"))
                .andExpect(jsonPath("$.horaFin").value("09:30"))
                .andExpect(jsonPath("$.nombreCreadaPor").value("Carlos Ruiz"));
    }

    @Test
    void debeRetornar201ConAutorRegistrado_cuandoPropietarioCreaFranjaConAliasJson() throws Exception {
        // HU-0046/CA-02
        String jsonConAlias = """
                {
                  "servicioId": 10,
                  "proveedorId": 7,
                  "dia_semana": "LUNES",
                  "hora_inicio": "10:30"
                }
                """;
        FranjaServicioResponse respuesta = FranjaServicioResponse.builder()
                .mensaje(FranjaServicioService.MSG_FRANJA_CREADA)
                .idFranja(101)
                .servicioId(10)
                .proveedorId(7)
                .nombreProveedor("Carlos Ruiz")
                .diaSemana("LUNES")
                .horaInicio("10:30")
                .horaFin("11:00")
                .creadaPorId(5)
                .nombreCreadaPor("Ana Gómez")
                .build();
        when(franjaServicioService.crearFranja(eq(10), any(CrearFranjaRequest.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/franjas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConAlias))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaInicio").value("10:30"))
                .andExpect(jsonPath("$.horaFin").value("11:00"))
                .andExpect(jsonPath("$.creadaPorId").value(5))
                .andExpect(jsonPath("$.nombreCreadaPor").value("Ana Gómez"));
    }

    @Test
    void debeRetornar400_cuandoFranjaNoCabeEnHorarioDelProveedor() throws Exception {
        // HU-0046/CA-03
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .servicioId(10)
                .diaSemana("LUNES")
                .horaInicio("11:30")
                .build();
        when(franjaServicioService.crearFranja(eq(10), any(CrearFranjaRequest.class)))
                .thenThrow(new ReglaNegocioException(FranjaServicioService.MSG_HORARIO_NO_CABE));

        mockMvc.perform(post("/api/v1/franjas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(FranjaServicioService.MSG_HORARIO_NO_CABE))
                .andExpect(jsonPath("$.error").value(FranjaServicioService.MSG_HORARIO_NO_CABE));
    }

    @Test
    void debeRetornar400_cuandoFranjaSeSuperponeConOtraDefinida() throws Exception {
        // HU-0046/CA-04
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .servicioId(10)
                .diaSemana("LUNES")
                .horaInicio("09:15")
                .build();
        when(franjaServicioService.crearFranja(eq(10), any(CrearFranjaRequest.class)))
                .thenThrow(new ReglaNegocioException(FranjaServicioService.MSG_FRANJA_SUPERPUESTA));

        mockMvc.perform(post("/api/v1/franjas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(FranjaServicioService.MSG_FRANJA_SUPERPUESTA));
    }

    @Test
    void debeRetornar403_cuandoUsuarioNoTienePermisoParaDefinirFranja() throws Exception {
        // HU-0046/CA-05
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .servicioId(10)
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(franjaServicioService.crearFranja(eq(10), any(CrearFranjaRequest.class)))
                .thenThrow(new AccesoDenegadoException(FranjaServicioService.MSG_SIN_PERMISO));

        mockMvc.perform(post("/api/v1/franjas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(FranjaServicioService.MSG_SIN_PERMISO));
    }

    @Test
    void debeRetornar404_cuandoServicioOProveedorNoExisteAlCrearFranja() throws Exception {
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .servicioId(999)
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(franjaServicioService.crearFranja(eq(999), any(CrearFranjaRequest.class)))
                .thenThrow(new RecursoNoEncontradoException(FranjaServicioService.MSG_SERVICIO_NO_ENCONTRADO));

        mockMvc.perform(post("/api/v1/franjas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(FranjaServicioService.MSG_SERVICIO_NO_ENCONTRADO));
    }

    @Test
    void debeRetornar200ConFranjas_cuandoConsultaConFiltrosDeProveedorYDia() throws Exception {
        FranjaServicioResponse franja = FranjaServicioResponse.builder()
                .idFranja(100)
                .servicioId(10)
                .proveedorId(7)
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .horaFin("09:30")
                .build();
        when(franjaServicioService.consultarFranjas(10, 7, "LUNES")).thenReturn(List.of(franja));

        mockMvc.perform(get("/api/v1/servicios/10/franjas")
                        .param("proveedorId", "7")
                        .param("diaSemana", "LUNES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idFranja").value(100))
                .andExpect(jsonPath("$[0].horaInicio").value("09:00"))
                .andExpect(jsonPath("$[0].horaFin").value("09:30"));
    }

    @Test
    void debeRetornar200ConFranjas_cuandoConsultaSoloPorServicio() throws Exception {
        when(franjaServicioService.consultarFranjas(10, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/servicios/10/franjas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void debeRetornar404_cuandoServicioNoExisteAlConsultarFranjas() throws Exception {
        when(franjaServicioService.consultarFranjas(999, null, null))
                .thenThrow(new RecursoNoEncontradoException(FranjaServicioService.MSG_SERVICIO_NO_ENCONTRADO));

        mockMvc.perform(get("/api/v1/servicios/999/franjas"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(FranjaServicioService.MSG_SERVICIO_NO_ENCONTRADO));
    }

    @Test
    void debeRetornar400_cuandoDiaSemanaEsInvalidoAlConsultarFranjas() throws Exception {
        when(franjaServicioService.consultarFranjas(10, null, "FESTIVO"))
                .thenThrow(new ReglaNegocioException("El día 'FESTIVO' no es válido."));

        mockMvc.perform(get("/api/v1/servicios/10/franjas")
                        .param("diaSemana", "FESTIVO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El día 'FESTIVO' no es válido."));
    }

    @Test
    void debeRetornar204_cuandoEliminaFranjaExistente() throws Exception {
        // HU-0046/CA-06
        doNothing().when(franjaServicioService).eliminarFranja(10, 100);

        mockMvc.perform(delete("/api/v1/servicios/10/franjas/100"))
                .andExpect(status().isNoContent());

        verify(franjaServicioService).eliminarFranja(10, 100);
    }

    @Test
    void debeRetornar403_cuandoNoTienePermisoParaEliminarFranja() throws Exception {
        // HU-0046/CA-06
        doThrow(new AccesoDenegadoException(FranjaServicioService.MSG_SIN_PERMISO_MODIFICAR))
                .when(franjaServicioService).eliminarFranja(10, 100);

        mockMvc.perform(delete("/api/v1/servicios/10/franjas/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(FranjaServicioService.MSG_SIN_PERMISO_MODIFICAR));
    }

    @Test
    void debeRetornar404_cuandoFranjaNoExisteAlEliminar() throws Exception {
        // HU-0046/CA-06
        doThrow(new RecursoNoEncontradoException(FranjaServicioService.MSG_FRANJA_NO_ENCONTRADA))
                .when(franjaServicioService).eliminarFranja(10, 999);

        mockMvc.perform(delete("/api/v1/servicios/10/franjas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value(FranjaServicioService.MSG_FRANJA_NO_ENCONTRADA));
    }

    @Test
    void debeRetornar400_cuandoFranjaNoPerteneceAlServicioAlEliminar() throws Exception {
        // HU-0046/CA-06
        doThrow(new ReglaNegocioException("La franja no pertenece al servicio indicado."))
                .when(franjaServicioService).eliminarFranja(10, 100);

        mockMvc.perform(delete("/api/v1/servicios/10/franjas/100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La franja no pertenece al servicio indicado."));
    }
}
