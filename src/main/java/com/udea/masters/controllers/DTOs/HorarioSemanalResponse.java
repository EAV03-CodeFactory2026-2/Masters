package com.udea.masters.controllers.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.udea.masters.entities.BloqueHorario;
import com.udea.masters.entities.DiaSemana;
import com.udea.masters.entities.HorarioProveedor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Horario semanal: siempre trae los 7 días, de lunes a domingo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioSemanalResponse {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String mensaje;

    /** Nula si el proveedor nunca ha guardado un horario. */
    private OffsetDateTime fechaActualizacion;

    private List<DiaHorarioResponse> dias;

    public static HorarioSemanalResponse desde(HorarioProveedor horario, String mensaje) {
        List<BloqueHorario> bloques = Optional.ofNullable(horario).map(HorarioProveedor::getBloques).orElse(List.of());
        List<DiaHorarioResponse> dias = Arrays.stream(DiaSemana.values())
                .map(dia -> diaDesde(dia, bloques))
                .toList();
        return HorarioSemanalResponse.builder()
                .mensaje(mensaje)
                .fechaActualizacion(Optional.ofNullable(horario).map(HorarioProveedor::getFechaActualizacion).orElse(null))
                .dias(dias)
                .build();
    }

    private static DiaHorarioResponse diaDesde(DiaSemana dia, List<BloqueHorario> bloques) {
        List<BloqueHorarioDTO> bloquesDelDia = bloques.stream()
                .filter(b -> b.getDiaSemana() == dia)
                .sorted(Comparator.comparing(BloqueHorario::getHoraInicio))
                .map(b -> new BloqueHorarioDTO(b.getHoraInicio().format(HORA), b.getHoraFin().format(HORA)))
                .toList();
        String nombre = dia.getNombre();
        return DiaHorarioResponse.builder()
                .dia(dia.name())
                .nombre(Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1))
                .activo(!bloquesDelDia.isEmpty())
                .bloques(bloquesDelDia)
                .build();
    }
}
