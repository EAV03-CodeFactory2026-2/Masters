package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.ProveedorDirectorioResponse;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.repositories.PerfilProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProveedorService implements IProveedorService {

    private final PerfilProveedorRepository perfilProveedorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorDirectorioResponse> consultarDirectorio(String query) {
        List<PerfilProveedor> proveedores;

        if (query != null && !query.trim().isEmpty()) {
            proveedores = perfilProveedorRepository.buscarEnDirectorio(query.trim());
        } else {
            proveedores = perfilProveedorRepository.buscarTodosActivos();
        }

        return proveedores.stream()
                .map(this::mapearADirectorioResponse)
                .collect(Collectors.toList());
    }

    private ProveedorDirectorioResponse mapearADirectorioResponse(PerfilProveedor perfil) {
        return ProveedorDirectorioResponse.builder()
                .idUsuario(perfil.getUsuario() != null ? perfil.getUsuario().getIdUsuario() : perfil.getUsuarioId())
                .nombre(perfil.getUsuario() != null ? perfil.getUsuario().getNombre() : "")
                .apellido(perfil.getUsuario() != null ? perfil.getUsuario().getApellido() : "")
                .correo(perfil.getUsuario() != null ? perfil.getUsuario().getCorreo() : "")
                .telefono(perfil.getUsuario() != null ? perfil.getUsuario().getTelefono() : "")
                .tipoDocumento(perfil.getTipoDocumento() != null ? perfil.getTipoDocumento().getCodigo() : null)
                .numeroDocumento(perfil.getNumeroDocumento())
                .especialidad(perfil.getEspecialidad())
                .descripcionProfesional(perfil.getDescripcionProfesional())
                .build();
    }
}
