package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

/**
 * Vista de solo lectura de la tabla negocio (hoy la administra Business).
 * Masters solo la usa para saber a qué propietario pertenece un negocio.
 */
@Entity
@Immutable
@Table(name = "negocio")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Negocio {

    @Id
    @Column(name = "id_negocio")
    private Integer idNegocio;

    @Column(name = "propietario_id", nullable = false)
    private Integer propietarioId;

    @Column(name = "nombre_negocio", nullable = false, length = 250)
    private String nombreNegocio;
}
