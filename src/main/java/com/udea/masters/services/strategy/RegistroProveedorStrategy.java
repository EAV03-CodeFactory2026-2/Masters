package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilProveedorRepository;
import com.udea.masters.repositories.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RegistroProveedorStrategy implements IRegistroRolStrategy {

    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PerfilProveedorRepository perfilProveedorRepository;

    @Override
    public boolean soporta(String rol) {
        return "proveedor".equalsIgnoreCase(Objects.requireNonNullElse(rol, "").trim());
    }

    @Override
    public String getNombreRol() {
        return "Proveedor";
    }

    @Override
    public void validar(RegistroUsuarioRequest request) {
        if (!StringUtils.hasText(request.getNombre())) {
            throw new ReglaNegocioException("Nombres es obligatorio");
        }
        if (!StringUtils.hasText(request.getApellido())) {
            throw new ReglaNegocioException("Apellidos es obligatorio");
        }
        if (!Objects.requireNonNullElse(request.getTelefono(), "").trim().matches("^\\d{7,15}$")) {
            throw new ReglaNegocioException("Ingresa un teléfono válido de 7 a 15 dígitos.");
        }

        boolean faltaTipoDoc = !StringUtils.hasText(request.getTipoDocumento());
        boolean faltaNumDoc = !StringUtils.hasText(request.getNumeroDocumento());
        boolean faltaEspecialidad = !StringUtils.hasText(request.getEspecialidad());

        if (faltaTipoDoc && faltaNumDoc && faltaEspecialidad) {
            throw new ReglaNegocioException("Los campos Tipo de documento, Número de documento y Especialidad son obligatorios para el rol Proveedor.");
        }
        if (faltaTipoDoc) {
            throw new ReglaNegocioException("Tipo de documento es obligatorio");
        }
        if (faltaNumDoc) {
            throw new ReglaNegocioException("Número de documento es obligatorio");
        }

        TipoDocumento tipoDoc = tipoDocumentoRepository.findByCodigoIgnoreCase(request.getTipoDocumento().trim())
                .orElseThrow(() -> new ReglaNegocioException("Tipo de documento no válido: " + request.getTipoDocumento()));

        if (perfilProveedorRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(tipoDoc, request.getNumeroDocumento().trim())) {
            throw new RecursoDuplicadoException("Ya existe un proveedor registrado con este documento.");
        }

        int largoEsp = Objects.requireNonNullElse(request.getEspecialidad(), "").trim().length();
        if (largoEsp < 2 || largoEsp > 100) {
            throw new ReglaNegocioException("La especialidad debe tener entre 2 y 100 caracteres.");
        }

        if (Objects.requireNonNullElse(request.getDescripcionProfesional(), "").trim().length() > 500) {
            throw new ReglaNegocioException("La descripción profesional no puede superar los 500 caracteres.");
        }
    }

    @Override
    public void procesarRegistro(RegistroUsuarioRequest request, Usuario usuario) {
        TipoDocumento tipoDoc = tipoDocumentoRepository.findByCodigoIgnoreCase(request.getTipoDocumento().trim())
                .orElseThrow(() -> new ReglaNegocioException("Tipo de documento no válido"));

        PerfilProveedor perfil = PerfilProveedor.builder()
                .usuario(usuario)
                .tipoDocumento(tipoDoc)
                .numeroDocumento(request.getNumeroDocumento().trim())
                .especialidad(request.getEspecialidad().trim())
                .descripcionProfesional(request.getDescripcionProfesional() != null ? request.getDescripcionProfesional().trim() : null)
                .build();

        perfilProveedorRepository.save(perfil);
    }
}
