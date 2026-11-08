package com.udea.masters.controllers.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.udea.masters.entities.FranjaServicio;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FranjaServicioResponse {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Schema(description = "Mensaje informativo de la operación", example = "Franja creada correctamente.")
    private String mensaje;

    @Schema(description = "Identificador único de la franja", example = "100")
    private Integer idFranja;

    @Schema(description = "Identificador del servicio", example = "1")
    private Integer servicioId;

    @Schema(description = "Nombre del servicio", example = "Corte clásico")
    private String nombreServicio;

    @Schema(description = "Duración del servicio en minutos", example = "30")
    private Integer duracionMinutos;

    @Schema(description = "Identificador del proveedor asignado", example = "12")
    private Integer proveedorId;

    @Schema(description = "Nombre completo del proveedor", example = "Carlos Ruiz")
    private String nombreProveedor;

    @Schema(description = "Día de la semana", example = "LUNES")
    private String diaSemana;

    @Schema(description = "Hora de inicio en formato HH:mm", example = "09:00")
    private String horaInicio;

    @Schema(description = "Hora de fin calculada en formato HH:mm", example = "09:30")
    private String horaFin;

    @Schema(description = "Identificador del usuario que creó la franja", example = "5")
    private Integer creadaPorId;

    @Schema(description = "Nombre del usuario que creó la franja", example = "Ana Gómez")
    private String nombreCreadaPor;

    @Schema(description = "Fecha y hora de creación de la franja")
    private OffsetDateTime fechaCreacion;

    public static FranjaServicioResponse desde(FranjaServicio franja, String mensaje) {
        if (franja == null) {
            return null;
        }

        String horaInicioStr = franja.getHoraInicio() != null ? franja.getHoraInicio().format(HORA_FORMATTER) : null;
        String horaFinStr = franja.getHoraFin() != null ? franja.getHoraFin().format(HORA_FORMATTER) : null;

        String nombreProveedor = null;
        if (franja.getProveedor() != null && franja.getProveedor().getUsuario() != null) {
            nombreProveedor = franja.getProveedor().getUsuario().getNombre() + " "
                    + franja.getProveedor().getUsuario().getApellido();
        }

        String nombreCreadaPor = null;
        if (franja.getCreadaPor() != null) {
            nombreCreadaPor = franja.getCreadaPor().getNombre() + " "
                    + franja.getCreadaPor().getApellido();
        }

        return FranjaServicioResponse.builder()
                .mensaje(mensaje)
                .idFranja(franja.getIdFranja())
                .servicioId(franja.getServicio() != null ? franja.getServicio().getIdServicio() : null)
                .nombreServicio(franja.getServicio() != null ? franja.getServicio().getNombre() : null)
                .duracionMinutos(franja.getServicio() != null ? franja.getServicio().getDuracionMinutos() : null)
                .proveedorId(franja.getProveedor() != null ? franja.getProveedor().getUsuarioId() : null)
                .nombreProveedor(nombreProveedor)
                .diaSemana(franja.getDiaSemana() != null ? franja.getDiaSemana().name() : null)
                .horaInicio(horaInicioStr)
                .horaFin(horaFinStr)
                .creadaPorId(franja.getCreadaPor() != null ? franja.getCreadaPor().getIdUsuario() : null)
                .nombreCreadaPor(nombreCreadaPor)
                .fechaCreacion(franja.getFechaCreacion())
                .build();
    }
}
