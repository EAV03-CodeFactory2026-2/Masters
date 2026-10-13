package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfil_proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilProveedor {

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 100)
    private String especialidad;

    @Column(name = "descripcion_profesional", length = 500)
    private String descripcionProfesional;
}
