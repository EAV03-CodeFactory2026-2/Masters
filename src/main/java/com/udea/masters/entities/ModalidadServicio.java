package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "modalidad_servicio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModalidadServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modalidad")
    private Integer idModalidad;

    @Column(name = "nombre_modalidad", nullable = false, unique = true, length = 20)
    private String nombreModalidad;
}
