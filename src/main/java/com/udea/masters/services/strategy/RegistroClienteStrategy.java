package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.PerfilCliente;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistroClienteStrategy implements IRegistroRolStrategy {

    private final PerfilClienteRepository perfilClienteRepository;

    @Override
    public boolean soporta(String rol) {
        return rol != null && "cliente".equalsIgnoreCase(rol.trim());
    }

    @Override
    public String getNombreRol() {
        return "Cliente";
    }

    @Override
    public void validar(RegistroUsuarioRequest request) {
        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new ReglaNegocioException("Nombres es obligatorio");
        }
        if (request.getApellido() == null || request.getApellido().trim().isEmpty()) {
            throw new ReglaNegocioException("Apellidos es obligatorio");
        }
        if (request.getTelefono() == null || request.getTelefono().trim().isEmpty()) {
            throw new ReglaNegocioException("Teléfono de contacto es obligatorio");
        }
        if (!request.getTelefono().trim().matches("^\\d{7,15}$")) {
            throw new ReglaNegocioException("Ingresa un teléfono válido de 7 a 15 dígitos.");
        }
    }

    @Override
    public void procesarRegistro(RegistroUsuarioRequest request, Usuario usuario) {
        PerfilCliente perfil = PerfilCliente.builder()
                .usuario(usuario)
                .build();

        perfilClienteRepository.save(perfil);
    }
}
