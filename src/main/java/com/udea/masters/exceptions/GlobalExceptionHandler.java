package com.udea.masters.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<Class<? extends Exception>, HttpStatus> ESTADOS = Map.of(
            NoAutenticadoException.class, HttpStatus.UNAUTHORIZED,
            RecursoDuplicadoException.class, HttpStatus.CONFLICT,
            RecursoNoEncontradoException.class, HttpStatus.NOT_FOUND,
            ReglaNegocioException.class, HttpStatus.BAD_REQUEST,
            AccesoDenegadoException.class, HttpStatus.FORBIDDEN,
            IllegalArgumentException.class, HttpStatus.BAD_REQUEST
    );

    @ExceptionHandler({
            NoAutenticadoException.class,
            RecursoDuplicadoException.class,
            RecursoNoEncontradoException.class,
            ReglaNegocioException.class,
            AccesoDenegadoException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleDomainException(Exception ex) {
        return buildResponse(ESTADOS.get(ex.getClass()), ex.getMessage());
    }

    @ExceptionHandler(HorarioInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleHorarioInvalido(HorarioInvalidoException ex) {
        ResponseEntity<Map<String, Object>> respuesta = buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        Optional.ofNullable(ex.getDia())
                .ifPresent(dia -> respuesta.getBody().put("dia", dia));
        Optional.ofNullable(ex.getBloque())
                .ifPresent(bloque -> respuesta.getBody().put("bloque", bloque));
        return respuesta;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acceso denegado: no tienes permisos para realizar esta operación.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Datos de solicitud inválidos");

        return buildResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", message);
        body.put("mensaje", message);
        return ResponseEntity.status(status).body(body);
    }
}
