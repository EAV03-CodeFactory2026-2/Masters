package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfil_propietario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilPropietario {

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;
}
