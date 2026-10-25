package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.entities.PerfilCliente;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.repositories.PerfilClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RolPerfilClienteStrategy implements IRolPerfilStrategy {

    private final PerfilClienteRepository perfilClienteRepository;

    @Override
    public boolean soporta(String nombreRol) {
        // H-04: aplicar trim() para normalizar el nombre del rol igual que en RegistroXxxStrategy
        return TipoRol.CLIENTE.getNombre().equalsIgnoreCase(
                Optional.ofNullable(nombreRol).map(String::trim).orElse(""));
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
