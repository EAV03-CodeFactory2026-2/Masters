package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.BloqueHorarioDTO;
import com.udea.masters.controllers.DTOs.DiaHorarioRequest;
import com.udea.masters.controllers.DTOs.HorarioSemanalRequest;
import com.udea.masters.entities.BloqueHorario;
import com.udea.masters.entities.DiaSemana;
import com.udea.masters.exceptions.HorarioInvalidoException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Reglas del horario semanal. Devuelve los bloques ya validados, listos para guardar.
 */
@Component
public class HorarioValidator {

    public static final int MAX_BLOQUES_POR_DIA = 4;
    public static final int INTERVALO_MINUTOS = 15;

    public static final String MSG_SIN_DIAS_ACTIVOS = "Debes definir al menos un día de atención.";
    public static final String MSG_MAX_BLOQUES = "Cada día admite un máximo de 4 bloques.";
    public static final String MSG_FIN_ANTES_DE_INICIO = "La hora de fin debe ser posterior a la hora de inicio.";
    public static final String MSG_INTERVALO = "Las horas deben estar en intervalos de 15 minutos.";
    public static final String MSG_FORMATO_HORA = "Las horas deben tener el formato HH:mm de 24 horas (ej. 08:00).";
    public static final String MSG_SUPERPUESTOS = "Los bloques del %s se superponen.";
    public static final String MSG_DIA_INVALIDO = "El día '%s' no es válido. Usa LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO o DOMINGO.";
    public static final String MSG_DIA_REPETIDO = "El %s está repetido en el horario.";
    public static final String MSG_DIA_SIN_BLOQUES = "El %s está activo pero no tiene bloques.";

    private static final Pattern FORMATO_HORA = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    public List<BloqueHorario> validar(HorarioSemanalRequest request) {
        Map<DiaSemana, DiaHorarioRequest> diasActivos = diasActivosEnOrden(request);
        if (diasActivos.isEmpty()) {
            throw new HorarioInvalidoException(MSG_SIN_DIAS_ACTIVOS);
        }

        List<BloqueHorario> resultado = new ArrayList<>();
        diasActivos.forEach((dia, diaRequest) -> resultado.addAll(validarDia(dia, diaRequest.getBloques())));
        return resultado;
    }

    /** Días activos ordenados de lunes a domingo; los inactivos se descartan con sus bloques. */
    private Map<DiaSemana, DiaHorarioRequest> diasActivosEnOrden(HorarioSemanalRequest request) {
        Map<DiaSemana, DiaHorarioRequest> activos = new EnumMap<>(DiaSemana.class);
        List<DiaHorarioRequest> dias = Optional.ofNullable(request)
                .map(HorarioSemanalRequest::getDias)
                .orElse(List.of());

        Set<DiaSemana> vistos = EnumSet.noneOf(DiaSemana.class);
        for (DiaHorarioRequest diaRequest : dias.stream().filter(Objects::nonNull).toList()) {
            DiaSemana dia = parsearDia(diaRequest.getDia());
            if (!vistos.add(dia)) {
                throw new HorarioInvalidoException(String.format(MSG_DIA_REPETIDO, dia.getNombre()), dia.name(), null);
            }
            if (estaActivo(diaRequest)) {
                activos.put(dia, diaRequest);
            }
        }
        return activos;
    }

    private boolean estaActivo(DiaHorarioRequest diaRequest) {
        return Objects.requireNonNullElse(diaRequest.getActivo(), !CollectionUtils.isEmpty(diaRequest.getBloques()));
    }

    private List<BloqueHorario> validarDia(DiaSemana dia, List<BloqueHorarioDTO> bloques) {
        if (CollectionUtils.isEmpty(bloques)) {
            throw new HorarioInvalidoException(String.format(MSG_DIA_SIN_BLOQUES, dia.getNombre()), dia.name(), null);
        }
        if (bloques.size() > MAX_BLOQUES_POR_DIA) {
            throw new HorarioInvalidoException(MSG_MAX_BLOQUES, dia.name(), null);
        }

        List<BloqueHorario> validados = bloques.stream()
                .map(bloque -> validarBloque(dia, bloque))
                .toList();
        validarQueNoSeSuperpongan(dia, validados);
        return validados;
    }

    private BloqueHorario validarBloque(DiaSemana dia, BloqueHorarioDTO bloque) {
        if (bloque == null) {
            throw new HorarioInvalidoException(MSG_FORMATO_HORA, dia.name(), "");
        }
        String texto = bloque.comoTexto();
        LocalTime inicio = parsearHora(bloque.getHoraInicio(), dia, texto);
        LocalTime fin = parsearHora(bloque.getHoraFin(), dia, texto);
        if (!fin.isAfter(inicio)) {
            throw new HorarioInvalidoException(MSG_FIN_ANTES_DE_INICIO, dia.name(), texto);
        }
        return BloqueHorario.builder().diaSemana(dia).horaInicio(inicio).horaFin(fin).build();
    }

    private LocalTime parsearHora(String hora, DiaSemana dia, String bloque) {
        String limpia = Objects.requireNonNullElse(hora, "").trim();
        if (!FORMATO_HORA.matcher(limpia).matches()) {
            throw new HorarioInvalidoException(MSG_FORMATO_HORA, dia.name(), bloque);
        }
        LocalTime valor = LocalTime.parse(limpia);
        if (valor.getMinute() % INTERVALO_MINUTOS != 0) {
            throw new HorarioInvalidoException(MSG_INTERVALO, dia.name(), bloque);
        }
        return valor;
    }

    /** Dos bloques se superponen si uno empieza antes de que termine el otro; los contiguos (12:00 y 12:00) sí se permiten. */
    private void validarQueNoSeSuperpongan(DiaSemana dia, List<BloqueHorario> bloques) {
        List<BloqueHorario> ordenados = bloques.stream()
                .sorted(Comparator.comparing(BloqueHorario::getHoraInicio))
                .toList();
        for (int i = 1; i < ordenados.size(); i++) {
            BloqueHorario anterior = ordenados.get(i - 1);
            BloqueHorario actual = ordenados.get(i);
            if (actual.getHoraInicio().isBefore(anterior.getHoraFin())) {
                throw new HorarioInvalidoException(String.format(MSG_SUPERPUESTOS, dia.getNombre()), dia.name(),
                        actual.getHoraInicio() + "-" + actual.getHoraFin());
            }
        }
    }

    private DiaSemana parsearDia(String dia) {
        String normalizado = Normalizer.normalize(Objects.requireNonNullElse(dia, "").trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return Arrays.stream(DiaSemana.values())
                .filter(valor -> valor.name().equalsIgnoreCase(normalizado))
                .findFirst()
                .orElseThrow(() -> new HorarioInvalidoException(String.format(MSG_DIA_INVALIDO, dia), null, null));
    }
}
