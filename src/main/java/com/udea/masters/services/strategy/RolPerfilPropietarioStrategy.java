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
        // H-04: aplicar trim() para normalizar el nombre del rol igual que en RegistroXxxStrategy
        return TipoRol.PROPIETARIO.getNombre().equalsIgnoreCase(
                Optional.ofNullable(nombreRol).map(String::trim).orElse(""));
    }

    @Override
    public String getNombreRol() {
        return TipoRol.PROPIETARIO.getNombre();
    }

    @Override
    public void asignarPerfil(Usuario usuario, RolAsignacionRequest request) {
        String tipoDocCodigo = request.getTipoDocumento();
        String numeroDoc = request.getNumeroDocumento();

        // H-02: Reutilizar el documento del otro perfil SOLO si ambos campos vienen vacíos.
        // Si llega exactamente uno de los dos campos, exigir el otro en lugar de mezclarlo.
        boolean faltaTipo = !StringUtils.hasText(tipoDocCodigo);
        boolean faltaNumero = !StringUtils.hasText(numeroDoc);

        if (faltaTipo && faltaNumero) {
            // Ninguno llegó: intentar reutilizar el perfil Proveedor existente
            Optional<PerfilProveedor> prov = perfilProveedorRepository.findById(usuario.getIdUsuario());
            tipoDocCodigo = prov.map(PerfilProveedor::getTipoDocumento).map(TipoDocumento::getCodigo).orElse(null);
            numeroDoc = prov.map(PerfilProveedor::getNumeroDocumento).orElse(null);
        } else if (faltaTipo) {
            // Solo llegó el número pero falta el tipo
            throw new ReglaNegocioException("Tipo de documento es obligatorio");
        } else if (faltaNumero) {
            // Solo llegó el tipo pero falta el número
            throw new ReglaNegocioException("Número de documento es obligatorio");
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
