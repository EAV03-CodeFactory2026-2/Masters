package com.udea.masters.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private MethodArgumentNotValidException validationException;

    @Mock
    private BindingResult bindingResult;

    private void assertRespuesta(ResponseEntity<Map<String, Object>> respuesta, HttpStatus status, String mensaje) {
        assertEquals(status, respuesta.getStatusCode());
        Map<String, Object> body = respuesta.getBody();
        assertNotNull(body);
        assertEquals(status.value(), body.get("status"));
        assertEquals(mensaje, body.get("error"));
        assertEquals(mensaje, body.get("mensaje"));
        assertInstanceOf(LocalDateTime.class, body.get("timestamp"));
    }

    @Test
    void debeRetornar409_cuandoRecursoDuplicado() {
        RecursoDuplicadoException ex = new RecursoDuplicadoException("Ya existe un recurso con este nombre.");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleRecursoDuplicado(ex);

        assertRespuesta(respuesta, HttpStatus.CONFLICT, "Ya existe un recurso con este nombre.");
    }

    @Test
    void debeRetornar404_cuandoRecursoNoEncontrado() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("Negocio no encontrado.");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleRecursoNoEncontrado(ex);

        assertRespuesta(respuesta, HttpStatus.NOT_FOUND, "Negocio no encontrado.");
    }

    @Test
    void debeRetornar400_cuandoReglaNegocioIncumplida() {
        ReglaNegocioException ex = new ReglaNegocioException("La cantidad debe ser un número entero entre 1 y 999.");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleReglaNegocio(ex);

        assertRespuesta(respuesta, HttpStatus.BAD_REQUEST, "La cantidad debe ser un número entero entre 1 y 999.");
    }

    @Test
    void debeRetornar400ConDiaYBloque_cuandoHorarioInvalidoTieneAmbos() {
        HorarioInvalidoException ex = new HorarioInvalidoException("Bloques solapados.", "Lunes", "08:00-10:00");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleHorarioInvalido(ex);

        assertRespuesta(respuesta, HttpStatus.BAD_REQUEST, "Bloques solapados.");
        assertEquals("Lunes", respuesta.getBody().get("dia"));
        assertEquals("08:00-10:00", respuesta.getBody().get("bloque"));
    }

    @Test
    void debeRetornar400SinDiaNiBloque_cuandoHorarioInvalidoSinDetalle() {
        HorarioInvalidoException ex = new HorarioInvalidoException("Horario vacío.");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleHorarioInvalido(ex);

        assertRespuesta(respuesta, HttpStatus.BAD_REQUEST, "Horario vacío.");
        assertFalse(respuesta.getBody().containsKey("dia"));
        assertFalse(respuesta.getBody().containsKey("bloque"));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "Martes, null",
            "null, 14:00-16:00"
    }, nullValues = "null")
    void debeIncluirSoloDetallePresente_cuandoHorarioInvalidoTieneUnDato(String dia, String bloque) {
        HorarioInvalidoException ex = new HorarioInvalidoException("Bloque inválido.", dia, bloque);

        ResponseEntity<Map<String, Object>> respuesta = handler.handleHorarioInvalido(ex);

        assertRespuesta(respuesta, HttpStatus.BAD_REQUEST, "Bloque inválido.");
        assertEquals(dia != null, respuesta.getBody().containsKey("dia"));
        assertEquals(bloque != null, respuesta.getBody().containsKey("bloque"));
        assertEquals(dia, respuesta.getBody().get("dia"));
        assertEquals(bloque, respuesta.getBody().get("bloque"));
    }

    @Test
    void debeRetornar400_cuandoIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("El usuario no tiene el rol de Propietario.");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleIllegalArgument(ex);

        assertRespuesta(respuesta, HttpStatus.BAD_REQUEST, "El usuario no tiene el rol de Propietario.");
    }

    @Test
    void debeRetornar400ConMensajeDelCampo_cuandoValidacionTieneFieldError() {
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "nombre", "El nombre no puede estar vacío."),
                new FieldError("request", "precio", "El precio es obligatorio.")));

        ResponseEntity<Map<String, Object>> respuesta = handler.handleValidationExceptions(validationException);

        assertRespuesta(respuesta, HttpStatus.BAD_REQUEST, "El nombre no puede estar vacío.");
    }

    @Test
    void debeRetornar400ConMensajeGenerico_cuandoValidacionSinFieldError() {
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> respuesta = handler.handleValidationExceptions(validationException);

        assertRespuesta(respuesta, HttpStatus.BAD_REQUEST, "Datos de solicitud inválidos");
    }

    @Test
    void debeRetornar403_cuandoAccesoDenegado() {
        AccesoDenegadoException ex = new AccesoDenegadoException("Solo un Propietario puede gestionar los recursos del negocio.");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleAccesoDenegado(ex);

        assertRespuesta(respuesta, HttpStatus.FORBIDDEN, "Solo un Propietario puede gestionar los recursos del negocio.");
    }

    @Test
    void debeRetornar403ConMensajeFijo_cuandoSpringSecurityDeniegaAcceso() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleAccessDenied(ex);

        assertRespuesta(respuesta, HttpStatus.FORBIDDEN,
                "Acceso denegado: no tienes permisos para realizar esta operación.");
    }
}
