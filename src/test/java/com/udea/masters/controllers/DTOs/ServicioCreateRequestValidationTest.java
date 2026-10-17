package com.udea.masters.controllers.DTOs;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicioCreateRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private ServicioCreateRequest requestValido() {
        return ServicioCreateRequest.builder()
                .negocioId(1)
                .modalidadId(1)
                .nombre("Corte de Cabello")
                .descripcion("Corte clásico de caballero")
                .duracionMinutos(30)
                .precio(new BigDecimal("25000.00"))
                .build();
    }

    @Test
    void requestValidoPasaValidacion() {
        ServicioCreateRequest request = requestValido();
        Set<ConstraintViolation<ServicioCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void nombreVacioFallaValidacion() {
        ServicioCreateRequest request = requestValido();
        request.setNombre("");

        Set<ConstraintViolation<ServicioCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
    }

    @Test
    void duracionMenorAUnoFallaValidacion() {
        ServicioCreateRequest request = requestValido();
        request.setDuracionMinutos(0);

        Set<ConstraintViolation<ServicioCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("duracionMinutos")));
    }

    @Test
    void precioNegativoFallaValidacion() {
        ServicioCreateRequest request = requestValido();
        request.setPrecio(new BigDecimal("-100.00"));

        Set<ConstraintViolation<ServicioCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("precio")));
    }
}
