package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.BloqueHorarioDTO;
import com.udea.masters.controllers.DTOs.DiaHorarioRequest;
import com.udea.masters.controllers.DTOs.HorarioSemanalRequest;
import com.udea.masters.entities.BloqueHorario;
import com.udea.masters.entities.DiaSemana;
import com.udea.masters.exceptions.HorarioInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HorarioValidatorTest {

    private final HorarioValidator validator = new HorarioValidator();

    private static BloqueHorarioDTO bloque(String inicio, String fin) {
        return new BloqueHorarioDTO(inicio, fin);
    }

    private static DiaHorarioRequest dia(String dia, Boolean activo, BloqueHorarioDTO... bloques) {
        return new DiaHorarioRequest(dia, activo, Arrays.asList(bloques));
    }

    private static HorarioSemanalRequest horario(DiaHorarioRequest... dias) {
        return new HorarioSemanalRequest(Arrays.asList(dias));
    }

    static Stream<Arguments> horariosSinDiasActivos() {
        return Stream.of(
                Arguments.of((HorarioSemanalRequest) null),
                Arguments.of(new HorarioSemanalRequest(null)),
                Arguments.of(new HorarioSemanalRequest(List.of())),
                Arguments.of(horario(dia("LUNES", false, bloque("08:00", "12:00")))),
                Arguments.of(horario(dia("MARTES", null))),
                Arguments.of(horario(new DiaHorarioRequest("MIERCOLES", null, null))),
                Arguments.of(horario((DiaHorarioRequest) null))
        );
    }

    @ParameterizedTest
    @MethodSource("horariosSinDiasActivos")
    void debeLanzarHorarioInvalido_cuandoNoHayDiasActivos(HorarioSemanalRequest request) {
        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(HorarioValidator.MSG_SIN_DIAS_ACTIVOS, ex.getMessage());
        assertNull(ex.getDia());
        assertNull(ex.getBloque());
    }

    @Test
    void debeIgnorarDiasNulos_cuandoLaListaContieneElementosNull() {
        HorarioSemanalRequest request = horario(null, dia("LUNES", true, bloque("08:00", "12:00")), null);

        List<BloqueHorario> resultado = validator.validar(request);

        assertEquals(1, resultado.size());
        assertEquals(DiaSemana.LUNES, resultado.get(0).getDiaSemana());
    }

    @Test
    void debeConsiderarActivo_cuandoActivoEsNullYTraeBloques() {
        HorarioSemanalRequest request = horario(dia("JUEVES", null, bloque("09:00", "10:00")));

        List<BloqueHorario> resultado = validator.validar(request);

        assertEquals(1, resultado.size());
        assertEquals(DiaSemana.JUEVES, resultado.get(0).getDiaSemana());
    }

    @Test
    void debeLanzarHorarioInvalido_cuandoDiaRepetido() {
        HorarioSemanalRequest request = horario(
                dia("LUNES", true, bloque("08:00", "12:00")),
                dia("lunes", false));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(String.format(HorarioValidator.MSG_DIA_REPETIDO, "lunes"), ex.getMessage());
        assertEquals("LUNES", ex.getDia());
        assertNull(ex.getBloque());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FUNDAY", "", "   ", "LUN", "MONDAY"})
    void debeLanzarHorarioInvalido_cuandoDiaDesconocido(String nombreDia) {
        HorarioSemanalRequest request = horario(dia(nombreDia, true, bloque("08:00", "12:00")));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(String.format(HorarioValidator.MSG_DIA_INVALIDO, nombreDia), ex.getMessage());
        assertNull(ex.getDia());
        assertNull(ex.getBloque());
    }

    @Test
    void debeLanzarHorarioInvalido_cuandoDiaEsNull() {
        HorarioSemanalRequest request = horario(dia(null, true, bloque("08:00", "12:00")));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(HorarioValidator.MSG_DIA_OBLIGATORIO, ex.getMessage());
        assertNull(ex.getDia());
    }

    @ParameterizedTest
    @CsvSource({
            "miércoles, MIERCOLES",
            "' Sábado ', SABADO",
            "lunes, LUNES",
            "DoMiNgO, DOMINGO",
            "MIÉRCOLES, MIERCOLES"
    })
    void debeNormalizarDia_cuandoTraeTildesMinusculasOEspacios(String entrada, DiaSemana esperado) {
        HorarioSemanalRequest request = horario(dia(entrada, true, bloque("08:00", "12:00")));

        List<BloqueHorario> resultado = validator.validar(request);

        assertEquals(esperado, resultado.get(0).getDiaSemana());
    }

    static Stream<Arguments> diasActivosSinBloques() {
        return Stream.of(
                Arguments.of(new DiaHorarioRequest("VIERNES", true, null)),
                Arguments.of(new DiaHorarioRequest("VIERNES", true, List.of()))
        );
    }

    @ParameterizedTest
    @MethodSource("diasActivosSinBloques")
    void debeLanzarHorarioInvalido_cuandoDiaActivoSinBloques(DiaHorarioRequest diaRequest) {
        HorarioSemanalRequest request = horario(diaRequest);

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(String.format(HorarioValidator.MSG_DIA_SIN_BLOQUES, "viernes"), ex.getMessage());
        assertEquals("VIERNES", ex.getDia());
        assertNull(ex.getBloque());
    }

    @Test
    void debeLanzarHorarioInvalido_cuandoDiaSuperaMaximoDeBloques() {
        HorarioSemanalRequest request = horario(dia("MARTES", true,
                bloque("06:00", "07:00"),
                bloque("08:00", "09:00"),
                bloque("10:00", "11:00"),
                bloque("12:00", "13:00"),
                bloque("14:00", "15:00")));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(HorarioValidator.MSG_MAX_BLOQUES, ex.getMessage());
        assertEquals("MARTES", ex.getDia());
        assertNull(ex.getBloque());
    }

    @Test
    void debeAceptarCuatroBloques_cuandoEsElMaximoPermitido() {
        HorarioSemanalRequest request = horario(dia("MARTES", true,
                bloque("06:00", "07:00"),
                bloque("08:00", "09:00"),
                bloque("10:00", "11:00"),
                bloque("12:00", "13:00")));

        List<BloqueHorario> resultado = validator.validar(request);

        assertEquals(HorarioValidator.MAX_BLOQUES_POR_DIA, resultado.size());
    }

    @Test
    void debeLanzarHorarioInvalido_cuandoBloqueEsNull() {
        List<BloqueHorarioDTO> bloques = new ArrayList<>();
        bloques.add(bloque("08:00", "09:00"));
        bloques.add(null);
        HorarioSemanalRequest request = horario(new DiaHorarioRequest("SABADO", true, bloques));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(HorarioValidator.MSG_FORMATO_HORA, ex.getMessage());
        assertEquals("SABADO", ex.getDia());
        assertEquals("", ex.getBloque());
    }

    static Stream<Arguments> horasConFormatoInvalido() {
        return Stream.of(
                Arguments.of(null, "10:00"),
                Arguments.of("08:00", null),
                Arguments.of("8:00", "10:00"),
                Arguments.of("08:00", "24:00"),
                Arguments.of("08:60", "10:00"),
                Arguments.of("08-00", "10:00"),
                Arguments.of("0800", "10:00"),
                Arguments.of("08:00:00", "10:00"),
                Arguments.of("", "10:00"),
                Arguments.of("08:00", "8 pm")
        );
    }

    @ParameterizedTest
    @MethodSource("horasConFormatoInvalido")
    void debeLanzarHorarioInvalido_cuandoHoraConFormatoInvalido(String inicio, String fin) {
        HorarioSemanalRequest request = horario(dia("LUNES", true, bloque(inicio, fin)));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(HorarioValidator.MSG_FORMATO_HORA, ex.getMessage());
        assertEquals("LUNES", ex.getDia());
        assertEquals(inicio + "-" + fin, ex.getBloque());
    }

    @ParameterizedTest
    @CsvSource({
            "08:10, 10:00",
            "08:00, 10:05",
            "08:59, 10:00",
            "08:00, 23:59"
    })
    void debeLanzarHorarioInvalido_cuandoMinutosNoSonMultiploDeQuince(String inicio, String fin) {
        HorarioSemanalRequest request = horario(dia("LUNES", true, bloque(inicio, fin)));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(HorarioValidator.MSG_INTERVALO, ex.getMessage());
        assertEquals("LUNES", ex.getDia());
        assertEquals(inicio + "-" + fin, ex.getBloque());
    }

    @ParameterizedTest
    @CsvSource({
            "10:00, 10:00",
            "10:00, 09:45",
            "23:45, 00:00"
    })
    void debeLanzarHorarioInvalido_cuandoFinNoEsPosteriorAInicio(String inicio, String fin) {
        HorarioSemanalRequest request = horario(dia("LUNES", true, bloque(inicio, fin)));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(HorarioValidator.MSG_FIN_ANTES_DE_INICIO, ex.getMessage());
        assertEquals("LUNES", ex.getDia());
        assertEquals(inicio + "-" + fin, ex.getBloque());
    }

    static Stream<Arguments> bloquesSuperpuestos() {
        return Stream.of(
                Arguments.of(List.of(bloque("08:00", "12:00"), bloque("11:00", "13:00")), "11:00-13:00"),
                Arguments.of(List.of(bloque("11:00", "13:00"), bloque("08:00", "12:00")), "11:00-13:00"),
                Arguments.of(List.of(bloque("08:00", "18:00"), bloque("09:00", "10:00")), "09:00-10:00"),
                Arguments.of(List.of(bloque("08:00", "09:00"), bloque("08:00", "09:00")), "08:00-09:00")
        );
    }

    @ParameterizedTest
    @MethodSource("bloquesSuperpuestos")
    void debeLanzarHorarioInvalido_cuandoBloquesSeSuperponen(List<BloqueHorarioDTO> bloques, String bloqueEsperado) {
        HorarioSemanalRequest request = horario(new DiaHorarioRequest("miércoles", true, bloques));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> validator.validar(request));

        assertEquals(String.format(HorarioValidator.MSG_SUPERPUESTOS, "miércoles"), ex.getMessage());
        assertEquals("MIERCOLES", ex.getDia());
        assertEquals(bloqueEsperado, ex.getBloque());
    }

    @Test
    void debePermitirBloquesContiguos_cuandoFinCoincideConInicio() {
        HorarioSemanalRequest request = horario(dia("LUNES", true,
                bloque("12:00", "14:00"),
                bloque("08:00", "12:00")));

        List<BloqueHorario> resultado = validator.validar(request);

        assertEquals(2, resultado.size());
    }

    @Test
    void debeRetornarBloquesOrdenadosPorDia_cuandoHorarioEsValido() {
        HorarioSemanalRequest request = horario(
                dia("DOMINGO", true, bloque("09:00", "13:00")),
                dia("SABADO", false, bloque("10:00", "11:00")),
                dia("lunes", null, bloque(" 14:00 ", "18:30"), bloque("07:45", "12:15")),
                dia("Miércoles", true, bloque("00:00", "23:45")));

        List<BloqueHorario> resultado = validator.validar(request);

        assertEquals(4, resultado.size());
        assertBloque(resultado.get(0), DiaSemana.LUNES, "14:00", "18:30");
        assertBloque(resultado.get(1), DiaSemana.LUNES, "07:45", "12:15");
        assertBloque(resultado.get(2), DiaSemana.MIERCOLES, "00:00", "23:45");
        assertBloque(resultado.get(3), DiaSemana.DOMINGO, "09:00", "13:00");
        assertTrue(resultado.stream().noneMatch(b -> b.getDiaSemana() == DiaSemana.SABADO));
        assertTrue(resultado.stream().allMatch(b -> b.getHorario() == null && b.getIdBloqueHorario() == null));
    }

    private static void assertBloque(BloqueHorario bloque, DiaSemana dia, String inicio, String fin) {
        assertEquals(dia, bloque.getDiaSemana());
        assertEquals(LocalTime.parse(inicio), bloque.getHoraInicio());
        assertEquals(LocalTime.parse(fin), bloque.getHoraFin());
    }
}
