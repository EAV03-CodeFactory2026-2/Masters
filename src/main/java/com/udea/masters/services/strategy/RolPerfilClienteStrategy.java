package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.entities.PerfilCliente;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.repositories.PerfilClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RolPerfilClienteStrategy implements IRolPerfilStrategy {

    private final PerfilClienteRepository perfilClienteRepository;

    @Override
    public boolean soporta(String nombreRol) {
        return TipoRol.CLIENTE.getNombre().equalsIgnoreCase(nombreRol);
    }

    @Override
    public String getNombreRol() {
        return TipoRol.CLIENTE.getNombre();
    }

    @Override
    public void asignarPerfil(Usuario usuario, RolAsignacionRequest request) {
        if (!perfilClienteRepository.existsById(usuario.getIdUsuario())) {
            PerfilCliente perfil = PerfilCliente.builder()
                    .usuario(usuario)
                    .build();
            perfilClienteRepository.save(perfil);
        }
    }

    @Override
    public void removerPerfil(Integer idUsuario) {
        perfilClienteRepository.deleteById(idUsuario);
    }
}
