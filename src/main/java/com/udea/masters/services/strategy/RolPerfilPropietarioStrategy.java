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
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RolPerfilPropietarioStrategy implements IRolPerfilStrategy {

    private final PerfilPropietarioRepository perfilPropietarioRepository;
    private final PerfilProveedorRepository perfilProveedorRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    @Override
    public boolean soporta(String nombreRol) {
        return TipoRol.PROPIETARIO.getNombre().equalsIgnoreCase(nombreRol);
    }

    @Override
    public String getNombreRol() {
        return TipoRol.PROPIETARIO.getNombre();
    }

    @Override
    public void asignarPerfil(Usuario usuario, RolAsignacionRequest request) {
        String tipoDocCodigo = request.getTipoDocumento();
        String numeroDoc = request.getNumeroDocumento();

        // Si no se proporcionaron datos de documento, reutilizar los del perfil Proveedor si ya existe
        if (!StringUtils.hasText(tipoDocCodigo) || !StringUtils.hasText(numeroDoc)) {
            PerfilProveedor prov = perfilProveedorRepository.findById(usuario.getIdUsuario()).orElse(null);
            if (prov != null) {
                tipoDocCodigo = StringUtils.hasText(tipoDocCodigo) ? tipoDocCodigo
                        : Optional.ofNullable(prov.getTipoDocumento()).map(TipoDocumento::getCodigo).orElse(null);
                numeroDoc = StringUtils.hasText(numeroDoc) ? numeroDoc : prov.getNumeroDocumento();
            }
        }

        if (!StringUtils.hasText(tipoDocCodigo)) {
            throw new ReglaNegocioException("Tipo de documento es obligatorio");
        }
        if (!StringUtils.hasText(numeroDoc)) {
            throw new ReglaNegocioException("Número de documento es obligatorio");
        }

        String finalTipoDocCodigo = tipoDocCodigo.trim();
        TipoDocumento tipoDoc = tipoDocumentoRepository.findByCodigoIgnoreCase(finalTipoDocCodigo)
                .orElseThrow(() -> new ReglaNegocioException("Tipo de documento no válido: " + finalTipoDocCodigo));

        if (perfilPropietarioRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(tipoDoc, numeroDoc.trim())) {
            throw new RecursoDuplicadoException("Ya existe un propietario registrado con este documento.");
        }

        PerfilPropietario perfil = PerfilPropietario.builder()
                .usuario(usuario)
                .tipoDocumento(tipoDoc)
                .numeroDocumento(numeroDoc.trim())
                .build();
        perfilPropietarioRepository.save(perfil);
    }

    @Override
    public void removerPerfil(Integer idUsuario) {
        perfilPropietarioRepository.deleteById(idUsuario);
    }
}
