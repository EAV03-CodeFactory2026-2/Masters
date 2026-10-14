package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.PerfilPropietario;
import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilPropietarioRepository;
import com.udea.masters.repositories.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegistroPropietarioStrategy implements IRegistroRolStrategy {

    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PerfilPropietarioRepository perfilPropietarioRepository;

    @Override
    public boolean soporta(String rol) {
        return rol != null && "propietario".equalsIgnoreCase(rol.trim());
    }

    @Override
    public String getNombreRol() {
        return "Propietario";
    }

    @Override
    public void validar(RegistroUsuarioRequest request) {
        boolean faltaNombre = request.getNombre() == null || request.getNombre().trim().isEmpty();
        boolean faltaDocumento = request.getNumeroDocumento() == null || request.getNumeroDocumento().trim().isEmpty();

        if (faltaNombre && faltaDocumento) {
            throw new ReglaNegocioException("Nombres y Número de documento son obligatorios");
        }
        if (faltaNombre) {
            throw new ReglaNegocioException("Nombres es obligatorio");
        }
        if (faltaDocumento) {
            throw new ReglaNegocioException("Número de documento es obligatorio");
        }

        if (!request.getNombre().trim().matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]{2,100}$")) {
            throw new ReglaNegocioException("Los nombres solo deben contener letras y tener entre 2 y 100 caracteres.");
        }

        if (request.getApellido() == null || request.getApellido().trim().isEmpty()) {
            throw new ReglaNegocioException("Apellidos es obligatorio");
        }

        if (request.getTipoDocumento() == null || request.getTipoDocumento().trim().isEmpty()) {
            throw new ReglaNegocioException("Tipo de documento es obligatorio");
        }

        TipoDocumento tipoDoc = tipoDocumentoRepository.findByCodigoIgnoreCase(request.getTipoDocumento().trim())
                .orElseThrow(() -> new ReglaNegocioException("Tipo de documento no válido: " + request.getTipoDocumento()));

        if (request.getTelefono() == null || !request.getTelefono().trim().matches("^\\d{7,15}$")) {
            throw new ReglaNegocioException("Ingresa un teléfono válido de 7 a 15 dígitos.");
        }

        if (perfilPropietarioRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(tipoDoc, request.getNumeroDocumento().trim())) {
            throw new RecursoDuplicadoException("Ya existe un propietario registrado con este documento.");
        }
    }

    @Override
    public void procesarRegistro(RegistroUsuarioRequest request, Usuario usuario) {
        TipoDocumento tipoDoc = tipoDocumentoRepository.findByCodigoIgnoreCase(request.getTipoDocumento().trim())
                .orElseThrow(() -> new ReglaNegocioException("Tipo de documento no válido"));

        PerfilPropietario perfil = PerfilPropietario.builder()
                .usuario(usuario)
                .tipoDocumento(tipoDoc)
                .numeroDocumento(request.getNumeroDocumento().trim())
                .build();

        perfilPropietarioRepository.save(perfil);
    }
}
