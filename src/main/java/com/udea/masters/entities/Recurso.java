package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Recurso físico (espacio o equipo) de un negocio.
 * Usa la misma tabla y columnas que tenía en Business, para conservar los datos existentes.
 */
@Entity
@Table(
        name = "recurso",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recurso_negocio_nombre_normalizado",
                columnNames = {"negocio_id", "nombre_normalizado"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recurso {

    public static final String ESTADO_ACTIVO = "Activo";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recurso")
    private Integer idRecurso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 100)
    private String nombre;

    /** Nombre sin tildes, en minúsculas y con espacios colapsados; garantiza la unicidad por negocio. */
    @Column(name = "nombre_normalizado", nullable = false, length = 100)
    private String nombreNormalizado;

    @Convert(converter = TipoRecursoConverter.class)
    @Column(nullable = false, length = 20)
    private TipoRecurso tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = ESTADO_ACTIVO;
}
