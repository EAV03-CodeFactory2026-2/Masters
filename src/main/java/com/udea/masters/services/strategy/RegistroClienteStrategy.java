package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.PerfilCliente;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RegistroClienteStrategy implements IRegistroRolStrategy {

    private final PerfilClienteRepository perfilClienteRepository;

    @Override
    public boolean soporta(String rol) {
        return "cliente".equalsIgnoreCase(Objects.requireNonNullElse(rol, "").trim());
    }

    @Override
    public String getNombreRol() {
        return "Cliente";
    }

    @Override
    public void validar(RegistroUsuarioRequest request) {
        if (!StringUtils.hasText(request.getNombre())) {
            throw new ReglaNegocioException("Nombres es obligatorio");
        }
        if (!StringUtils.hasText(request.getApellido())) {
            throw new ReglaNegocioException("Apellidos es obligatorio");
        }
        if (!StringUtils.hasText(request.getTelefono())) {
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
