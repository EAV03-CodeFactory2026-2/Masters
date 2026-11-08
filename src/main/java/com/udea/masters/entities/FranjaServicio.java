package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "franja_servicio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_franja_servicio_slot",
                columnNames = {"servicio_id", "proveedor_id", "dia_semana", "hora_inicio"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FranjaServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_franja")
    private Integer idFranja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private PerfilProveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DiaSemana diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creada_por", nullable = false)
    private Usuario creadaPor;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = OffsetDateTime.now();
        }
    }

    public LocalTime getHoraFin() {
        if (horaInicio == null || servicio == null || servicio.getDuracionMinutos() == null) {
            return null;
        }
        return horaInicio.plusMinutes(servicio.getDuracionMinutos());
    }
}
