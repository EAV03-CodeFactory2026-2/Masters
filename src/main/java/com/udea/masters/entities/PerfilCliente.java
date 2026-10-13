package com.udea.masters.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfil_cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilCliente {

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
