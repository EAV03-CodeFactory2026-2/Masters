package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Immutable
@Table(name = "servicio_proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioProveedor {
    @Id
    @Column(name = "id_servicio_proveedor")
    private Integer idServicioProveedor;

    @Column(name = "servicio_id")
    private Integer servicioId;

    @Column(name = "proveedor_id")
    private Integer proveedorId;
}
