package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.entities.PerfilPropietario;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilPropietarioRepository;
import com.udea.masters.repositories.PerfilProveedorRepository;
import com.udea.masters.repositories.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RolPerfilProveedorStrategy implements IRolPerfilStrategy {

    private final PerfilProveedorRepository perfilProveedorRepository;
    private final PerfilPropietarioRepository perfilPropietarioRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    @Override
    public boolean soporta(String nombreRol) {
        return TipoRol.PROVEEDOR.getNombre().equalsIgnoreCase(nombreRol);
    }

    @Override
    public String getNombreRol() {
        return TipoRol.PROVEEDOR.getNombre();
    }

    @Override
    public void asignarPerfil(Usuario usuario, RolAsignacionRequest request) {
        String tipoDocCodigo = request.getTipoDocumento();
        String numeroDoc = request.getNumeroDocumento();

        // Si no se proporcionaron datos de documento, reutilizar los del perfil Propietario si ya existe
        if ((tipoDocCodigo == null || tipoDocCodigo.trim().isEmpty()) || (numeroDoc == null || numeroDoc.trim().isEmpty())) {
            Optional<PerfilPropietario> perfilPropietarioOpt = perfilPropietarioRepository.findById(usuario.getIdUsuario());
            if (perfilPropietarioOpt.isPresent()) {
                PerfilPropietario prop = perfilPropietarioOpt.get();
                if ((tipoDocCodigo == null || tipoDocCodigo.trim().isEmpty()) && prop.getTipoDocumento() != null) {
                    tipoDocCodigo = prop.getTipoDocumento().getCodigo();
                }
                if (numeroDoc == null || numeroDoc.trim().isEmpty()) {
                    numeroDoc = prop.getNumeroDocumento();
                }
            }
        }

        if (tipoDocCodigo == null || tipoDocCodigo.trim().isEmpty()) {
            throw new ReglaNegocioException("Tipo de documento es obligatorio");
        }
        if (numeroDoc == null || numeroDoc.trim().isEmpty()) {
            throw new ReglaNegocioException("Número de documento es obligatorio");
        }

        String finalTipoDocCodigo = tipoDocCodigo.trim();
        TipoDocumento tipoDoc = tipoDocumentoRepository.findByCodigoIgnoreCase(finalTipoDocCodigo)
                .orElseThrow(() -> new ReglaNegocioException("Tipo de documento no válido: " + finalTipoDocCodigo));

        if (perfilProveedorRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(tipoDoc, numeroDoc.trim())) {
            throw new RecursoDuplicadoException("Ya existe un proveedor registrado con este documento.");
        }

        if (request.getEspecialidad() == null || request.getEspecialidad().trim().length() < 2 || request.getEspecialidad().trim().length() > 100) {
            throw new ReglaNegocioException("La especialidad debe tener entre 2 y 100 caracteres.");
        }

        if (request.getDescripcionProfesional() != null && request.getDescripcionProfesional().trim().length() > 500) {
            throw new ReglaNegocioException("La descripción profesional no puede superar los 500 caracteres.");
        }

        PerfilProveedor perfil = PerfilProveedor.builder()
                .usuario(usuario)
                .tipoDocumento(tipoDoc)
                .numeroDocumento(numeroDoc.trim())
                .especialidad(request.getEspecialidad().trim())
                .descripcionProfesional(request.getDescripcionProfesional() != null ? request.getDescripcionProfesional().trim() : null)
                .build();
        perfilProveedorRepository.save(perfil);
    }

    @Override
    public void removerPerfil(Integer idUsuario) {
        perfilProveedorRepository.deleteById(idUsuario);
    }
}
