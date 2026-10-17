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

@Component
@RequiredArgsConstructor
public class RegistroProveedorStrategy implements IRegistroRolStrategy {

    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PerfilProveedorRepository perfilProveedorRepository;

    @Override
    public boolean soporta(String rol) {
        return rol != null && "proveedor".equalsIgnoreCase(rol.trim());
    }

    @Override
    public String getNombreRol() {
        return "Proveedor";
    }

    @Override
    public void validar(RegistroUsuarioRequest request) {
        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new ReglaNegocioException("Nombres es obligatorio");
        }
        if (request.getApellido() == null || request.getApellido().trim().isEmpty()) {
            throw new ReglaNegocioException("Apellidos es obligatorio");
        }
        if (request.getTelefono() == null || !request.getTelefono().trim().matches("^\\d{7,15}$")) {
            throw new ReglaNegocioException("Ingresa un teléfono válido de 7 a 15 dígitos.");
        }

        boolean faltaTipoDoc = request.getTipoDocumento() == null || request.getTipoDocumento().trim().isEmpty();
        boolean faltaNumDoc = request.getNumeroDocumento() == null || request.getNumeroDocumento().trim().isEmpty();
        boolean faltaEspecialidad = request.getEspecialidad() == null || request.getEspecialidad().trim().isEmpty();

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

        if (request.getEspecialidad() == null || request.getEspecialidad().trim().length() < 2 || request.getEspecialidad().trim().length() > 100) {
            throw new ReglaNegocioException("La especialidad debe tener entre 2 y 100 caracteres.");
        }

        if (request.getDescripcionProfesional() != null && request.getDescripcionProfesional().trim().length() > 500) {
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
