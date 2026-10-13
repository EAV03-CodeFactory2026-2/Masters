package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = "roles")
@ToString(exclude = "roles")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(nullable = false, length = 20)
    private String telefono;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false, length = 255)
    private String contrasena;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String estado = "Activo";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "asignacion_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = OffsetDateTime.now();
        }
    }
}
