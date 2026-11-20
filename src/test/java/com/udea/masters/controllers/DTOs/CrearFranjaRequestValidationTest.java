package com.udea.masters.controllers.DTOs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrearFranjaRequestValidationTest {

    private static Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void debePasarValidacionEstandar_cuandoRequestEstaCompleto() {
        // HU-0046/CA-01
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .servicioId(1)
                .proveedorId(12)
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();

        Set<ConstraintViolation<CrearFranjaRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void debePasarValidacionBeanValidation_cuandoCamposOpcionalesEnRutaORolSonNulos() {
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();

        Set<ConstraintViolation<CrearFranjaRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void debeDeserializarAliasCortos_cuandoRecibeDiaEInicioEnJson() throws Exception {
        String json = """
                {
                  "servicioId": 3,
                  "proveedorId": 7,
                  "dia": "MARTES",
                  "inicio": "14:00"
                }
                """;

        CrearFranjaRequest request = objectMapper.readValue(json, CrearFranjaRequest.class);

        assertEquals(3, request.getServicioId());
        assertEquals(7, request.getProveedorId());
        assertEquals("MARTES", request.getDiaSemana());
        assertEquals("14:00", request.getHoraInicio());
    }

    @Test
    void debeDeserializarAliasSnakeCase_cuandoRecibeDiaSemanaYHoraInicioEnJson() throws Exception {
        String json = """
                {
                  "servicioId": 4,
                  "dia_semana": "VIERNES",
                  "hora_inicio": "16:30"
                }
                """;

        CrearFranjaRequest request = objectMapper.readValue(json, CrearFranjaRequest.class);

        assertEquals(4, request.getServicioId());
        assertEquals("VIERNES", request.getDiaSemana());
        assertEquals("16:30", request.getHoraInicio());
    }
}
