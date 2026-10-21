package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RecursoCreateRequest;
import com.udea.masters.entities.TipoRecurso;
import com.udea.masters.exceptions.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RecursoValidatorTest {

    private final RecursoValidator validator = new RecursoValidator();

    private RecursoCreateRequest request(String nombre, String tipo, Object cantidad, String descripcion) {
        return RecursoCreateRequest.builder()
                .nombre(nombre)
                .tipo(tipo)
                .cantidad(cantidad)
                .descripcion(descripcion)
                .build();
    }

    @Test
    void debeRetornarDatosLimpios_cuandoRequestValido() {
        RecursoCreateRequest req = request("  Cámilla   DE  masajes ", " Equipo ", 3, "  Plegable  ");

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertEquals(new RecursoValidator.DatosRecurso(
                "Cámilla DE masajes", "camilla de masajes", TipoRecurso.EQUIPO, 3, "Plegable"), datos);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoRequestNulo() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(null));

        assertEquals(RecursoValidator.MSG_NOMBRE_OBLIGATORIO, ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void debeLanzarNombreObligatorio_cuandoNombreVacio(String nombre) {
        RecursoCreateRequest req = request(nombre, "Espacio", 1, null);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(req));

        assertEquals(RecursoValidator.MSG_NOMBRE_OBLIGATORIO, ex.getMessage());
    }

    static Stream<String> nombresLargos() {
        return Stream.of("a".repeat(101), "  " + "b".repeat(101) + "  ", "a ".repeat(60));
    }

    @ParameterizedTest
    @MethodSource("nombresLargos")
    void debeLanzarNombreLargo_cuandoNombreSupera100Caracteres(String nombre) {
        RecursoCreateRequest req = request(nombre, "Espacio", 1, null);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(req));

        assertEquals(RecursoValidator.MSG_NOMBRE_LARGO, ex.getMessage());
    }

    @Test
    void debeAceptarNombre_cuandoTiene100CaracteresTrasColapsarEspacios() {
        String nombre = "a".repeat(49) + "        " + "b".repeat(50);
        RecursoCreateRequest req = request(nombre, "Espacio", 1, null);

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertEquals(100, datos.nombre().length());
        assertEquals("a".repeat(49) + " " + "b".repeat(50), datos.nombre());
    }

    @ParameterizedTest
    @CsvSource({
            "Espacio, ESPACIO",
            "espacio, ESPACIO",
            "'  ESPÁCIO  ', ESPACIO",
            "Equipo, EQUIPO",
            "équipo, EQUIPO"
    })
    void debeResolverTipo_cuandoNombreCoincideSinDistinguirMayusculasNiTildes(String tipo, TipoRecurso esperado) {
        RecursoCreateRequest req = request("Sala", tipo, 1, null);

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertEquals(esperado, datos.tipo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Mueble", "Espacios", "Equipo Espacio"})
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
                Arguments.of("+8", 8),
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
    void debeRetornarDescripcionNula_cuandoDescripcionVacia(String descripcion) {
        RecursoCreateRequest req = request("Sala", "Espacio", 1, descripcion);

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertNull(datos.descripcion());
    }

    @Test
    void debeAceptarDescripcion_cuandoTiene500CaracteresTrasRecortar() {
        String descripcion = "  " + "d".repeat(500) + "  ";
        RecursoCreateRequest req = request("Sala", "Espacio", 1, descripcion);

        RecursoValidator.DatosRecurso datos = validator.validar(req);

        assertEquals("d".repeat(500), datos.descripcion());
    }

    @Test
    void debeLanzarDescripcionLarga_cuandoDescripcionSupera500Caracteres() {
        RecursoCreateRequest req = request("Sala", "Espacio", 1, "d".repeat(501));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(req));

        assertEquals(RecursoValidator.MSG_DESCRIPCION_LARGA, ex.getMessage());
    }

    @Test
    void debeValidarNombreAntesQueDemasCampos_cuandoTodosSonInvalidos() {
        RecursoCreateRequest req = request(" ", "Mueble", "abc", "d".repeat(501));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> validator.validar(req));

        assertEquals(RecursoValidator.MSG_NOMBRE_OBLIGATORIO, ex.getMessage());
    }
}
