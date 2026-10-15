package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Franja de atención de un día dentro del horario semanal de un proveedor.
 */
@Entity
@Table(name = "bloque_horario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloque_horario")
    private Integer idBloqueHorario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "horario_id", nullable = false)
    private HorarioProveedor horario;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
