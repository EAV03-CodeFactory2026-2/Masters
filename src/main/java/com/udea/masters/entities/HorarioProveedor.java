package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Horario de atención semanal recurrente de un proveedor (uno por proveedor).
 * Los días sin bloques se consideran inactivos.
 */
@Entity
@Table(name = "horario_proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Integer idHorario;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false, unique = true)
    private PerfilProveedor proveedor;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @OneToMany(mappedBy = "horario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BloqueHorario> bloques = new ArrayList<>();

    /** Reemplaza todos los bloques; los anteriores se eliminan por orphanRemoval. */
    public void reemplazarBloques(List<BloqueHorario> nuevos, OffsetDateTime fecha) {
        bloques.clear();
        nuevos.forEach(b -> b.setHorario(this));
        bloques.addAll(nuevos);
        fechaActualizacion = fecha;
    }
}
