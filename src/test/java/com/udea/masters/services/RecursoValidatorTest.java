package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RecursoCreateRequest;
import com.udea.masters.entities.TipoRecurso;
import com.udea.masters.exceptions.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RecursoValidatorTest {

    private RecursoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RecursoValidator();
    }

    private RecursoCreateRequest request(String nombre, String tipo, Object cantidad, String descripcion) {
        return RecursoCreateRequest.builder()
                .nombre(nombre)
                .tipo(tipo)
                .cantidad(cantidad)
                .descripcion(descripcion)
                .build();
    }

    @Test
    void debeValidarYLimpiarDatos_cuandoRequestEsValido() {
        RecursoCreateRequest req = request("  Camilla  de   masajes  ", "Equipo", 3, "  Tres camillas  ");

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertEquals("Camilla de masajes", datos.nombre());
        assertEquals("camilla de masajes", datos.nombreNormalizado());
        assertEquals(TipoRecurso.EQUIPO, datos.tipo());
        assertEquals(3, datos.cantidad());
        assertEquals("Tres camillas", datos.descripcion());
    }

    @Test
    void debeDejarDescripcionNull_cuandoEsNullOBlanco() {
        RecursoCreateRequest req1 = request("Sala", "Espacio", 1, null);
        RecursoCreateRequest req2 = request("Sala", "Espacio", 1, "   ");

        assertNull(validator.validar(req1).descripcion());
        assertNull(validator.validar(req2).descripcion());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void debeLanzarNombreObligatorio_cuandoNombreEsNuloOBlanco(String nombre) {
        RecursoCreateRequest req = request(nombre, "Espacio", 1, null);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(req));

        assertEquals(RecursoValidator.MSG_NOMBRE_OBLIGATORIO, ex.getMessage());
    }

    @Test
    void debeLanzarNombreLargo_cuandoNombreExcede100Caracteres() {
        RecursoCreateRequest req = request("a".repeat(101), "Espacio", 1, null);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(req));

        assertEquals(RecursoValidator.MSG_NOMBRE_LARGO, ex.getMessage());
    }

    @Test
    void debeAceptarNombreDe100Caracteres() {
        RecursoCreateRequest req = request("a".repeat(100), "Espacio", 1, null);

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertEquals(100, datos.nombre().length());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Espacio", "espacio", "ESPACIO", "Equipo", "equipo", "EQUIPO", "  espacio  "})
    void debeAceptarTiposValidos(String tipo) {
        RecursoCreateRequest req = request("Sala", tipo, 1, null);

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertNotNull(datos.tipo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Invalido", "Herramienta", "123", "   "})
    void debeLanzarTipoInvalido_cuandoTipoNoEsEspacioNiEquipo(String tipo) {
        RecursoCreateRequest req = request("Sala", tipo, 1, null);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(req));

        assertEquals(RecursoValidator.MSG_TIPO_INVALIDO, ex.getMessage());
    }

    static Stream<Arguments> cantidadesValidas() {
        return Stream.of(
                Arguments.of(1, 1),
                Arguments.of(999, 999),
                Arguments.of(5L, 5),
                Arguments.of((short) 7, 7),
                Arguments.of(BigInteger.valueOf(10), 10),
                Arguments.of("3", 3),
                Arguments.of(" 42 ", 42),
                Arguments.of("0999", 999)
        );
    }

    @ParameterizedTest
    @MethodSource("cantidadesValidas")
    void debeConvertirCantidad_cuandoEsEnteroEnRango(Object cantidad, Integer esperado) {
        RecursoCreateRequest req = request("Sala", "Espacio", cantidad, null);

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertEquals(esperado, datos.cantidad());
    }

    static Stream<Object> cantidadesInvalidas() {
        return Stream.of(
                null,
                0,
                -1,
                1000,
                0L,
                1000L,
                Long.MAX_VALUE,
                (short) 0,
                BigInteger.ZERO,
                new BigInteger("99999999999999999999"),
                2.5,
                2.5f,
                new BigDecimal("3"),
                "",
                "   ",
                "abc",
                "2.5",
                "3a",
                "-1",
                "+8",
                "0",
                "1000",
                true
        );
    }

    @ParameterizedTest
    @MethodSource("cantidadesInvalidas")
    void debeLanzarCantidadInvalida_cuandoCantidadNoEsEnteroEntre1Y999(Object cantidad) {
        RecursoCreateRequest req = request("Sala", "Espacio", cantidad, null);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(req));

        assertEquals(RecursoValidator.MSG_CANTIDAD_INVALIDA, ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void debeAceptarDescripcionVacia(String descripcion) {
        RecursoCreateRequest req = request("Sala", "Espacio", 1, descripcion);

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertNull(datos.descripcion());
    }

    @Test
    void debeAceptarDescripcionDe500Caracteres() {
        RecursoCreateRequest req = request("Sala", "Espacio", 1, "d".repeat(500));

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertEquals(500, datos.descripcion().length());
    }

    @Test
    void debeLanzarDescripcionLarga_cuandoExcede500Caracteres() {
        RecursoCreateRequest req = request("Sala", "Espacio", 1, "d".repeat(501));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(req));

        assertEquals(RecursoValidator.MSG_DESCRIPCION_LARGA, ex.getMessage());
    }

    @Test
    void debeLanzarNombreObligatorio_cuandoRequestEsNull() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(null));

        assertEquals(RecursoValidator.MSG_NOMBRE_OBLIGATORIO, ex.getMessage());
    }
}
