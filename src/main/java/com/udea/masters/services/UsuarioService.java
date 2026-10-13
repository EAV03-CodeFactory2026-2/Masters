package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.entities.*;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PerfilPropietarioRepository perfilPropietarioRepository;
    private final PerfilProveedorRepository perfilProveedorRepository;
    private final PerfilClienteRepository perfilClienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorId(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + idUsuario));

        Integer currentUserId = obtenerIdUsuarioAutenticado();
        if (!esUsuarioVisibleParaConsulta(usuario, currentUserId)) {
            throw new AccessDeniedException("No tienes permisos para consultar la información de otros propietarios.");
        }

        return mapToResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        Integer currentUserId = obtenerIdUsuarioAutenticado();

        return usuarioRepository.findAll().stream()
                .filter(u -> esUsuarioVisibleParaConsulta(u, currentUserId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarPorRol(String nombreRol) {
        Integer currentUserId = obtenerIdUsuarioAutenticado();

        return usuarioRepository.findByRolesNombreRol(nombreRol).stream()
                .filter(u -> {
                    if (TipoRol.PROPIETARIO.getNombre().equalsIgnoreCase(nombreRol)) {
                        return currentUserId != null && u.getIdUsuario() != null && u.getIdUsuario().equals(currentUserId);
                    }
                    return esUsuarioVisibleParaConsulta(u, currentUserId);
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void asignarRolAUsuario(Integer idUsuario, RolAsignacionRequest request) {
        if (request == null || request.getNombreRol() == null || request.getNombreRol().trim().isEmpty()) {
            throw new ReglaNegocioException("Debes indicar el rol a asignar.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + idUsuario));

        String nombreRol = request.getNombreRol().trim();
        Rol rol = rolRepository.findByNombreRolIgnoreCase(nombreRol)
                .orElseThrow(() -> new ReglaNegocioException("Rol no encontrado con nombre: " + nombreRol));

        boolean yaTieneRol = usuario.getRoles().stream()
                .anyMatch(r -> r.getNombreRol().equalsIgnoreCase(rol.getNombreRol()));
        if (yaTieneRol) {
            throw new ReglaNegocioException("El usuario ya tiene asignado el rol " + rol.getNombreRol() + ".");
        }

        if (TipoRol.PROPIETARIO.getNombre().equalsIgnoreCase(rol.getNombreRol())) {
            validarYCrearPerfilPropietario(usuario, request);
        } else if (TipoRol.PROVEEDOR.getNombre().equalsIgnoreCase(rol.getNombreRol())) {
            validarYCrearPerfilProveedor(usuario, request);
        } else if (TipoRol.CLIENTE.getNombre().equalsIgnoreCase(rol.getNombreRol())) {
            validarYCrearPerfilCliente(usuario);
        }

        usuario.getRoles().add(rol);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void asignarRolAUsuario(Integer idUsuario, String nombreRol) {
        asignarRolAUsuario(idUsuario, RolAsignacionRequest.builder().nombreRol(nombreRol).build());
    }

    @Override
    @Transactional
    public void removerRolDeUsuario(Integer idUsuario, String nombreRol) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + idUsuario));

        Rol rol = rolRepository.findByNombreRolIgnoreCase(nombreRol)
                .orElseThrow(() -> new ReglaNegocioException("Rol no encontrado con nombre: " + nombreRol));

        boolean tieneRol = usuario.getRoles().stream()
                .anyMatch(r -> r.getNombreRol().equalsIgnoreCase(rol.getNombreRol()));
        if (!tieneRol) {
            throw new ReglaNegocioException("El usuario no tiene asignado el rol " + rol.getNombreRol() + ".");
        }

        if (usuario.getRoles().size() <= 1) {
            throw new ReglaNegocioException("No se puede eliminar el único rol del usuario.");
        }

        if (TipoRol.PROPIETARIO.getNombre().equalsIgnoreCase(rol.getNombreRol())) {
            perfilPropietarioRepository.deleteById(idUsuario);
        } else if (TipoRol.PROVEEDOR.getNombre().equalsIgnoreCase(rol.getNombreRol())) {
            perfilProveedorRepository.deleteById(idUsuario);
        } else if (TipoRol.CLIENTE.getNombre().equalsIgnoreCase(rol.getNombreRol())) {
            perfilClienteRepository.deleteById(idUsuario);
        }

        usuario.getRoles().removeIf(r -> r.getNombreRol().equalsIgnoreCase(rol.getNombreRol()));
        usuarioRepository.save(usuario);
    }

    private void validarYCrearPerfilPropietario(Usuario usuario, RolAsignacionRequest request) {
        String tipoDocCodigo = request.getTipoDocumento();
        String numeroDoc = request.getNumeroDocumento();

        // Si no se proporcionaron datos de documento, reutilizar los del perfil Proveedor si ya existe
        if ((tipoDocCodigo == null || tipoDocCodigo.trim().isEmpty()) || (numeroDoc == null || numeroDoc.trim().isEmpty())) {
            Optional<PerfilProveedor> perfilProveedorOpt = perfilProveedorRepository.findById(usuario.getIdUsuario());
            if (perfilProveedorOpt.isPresent()) {
                PerfilProveedor prov = perfilProveedorOpt.get();
                if ((tipoDocCodigo == null || tipoDocCodigo.trim().isEmpty()) && prov.getTipoDocumento() != null) {
                    tipoDocCodigo = prov.getTipoDocumento().getCodigo();
                }
                if (numeroDoc == null || numeroDoc.trim().isEmpty()) {
                    numeroDoc = prov.getNumeroDocumento();
                }
            }
        }

        if (tipoDocCodigo == null || tipoDocCodigo.trim().isEmpty()) {
            throw new ReglaNegocioException("Tipo de documento es obligatorio");
        }
        if (numeroDoc == null || numeroDoc.trim().isEmpty()) {
            throw new ReglaNegocioException("\"Número de documento\" es obligatorio");
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

    private void validarYCrearPerfilProveedor(Usuario usuario, RolAsignacionRequest request) {
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

    private void validarYCrearPerfilCliente(Usuario usuario) {
        if (!perfilClienteRepository.existsById(usuario.getIdUsuario())) {
            PerfilCliente perfil = PerfilCliente.builder()
                    .usuario(usuario)
                    .build();
            perfilClienteRepository.save(perfil);
        }
    }

    private UsuarioResponse mapToResponse(Usuario usuario) {
        Set<String> nombresRoles = usuario.getRoles() != null
                ? usuario.getRoles().stream().map(Rol::getNombreRol).collect(Collectors.toSet())
                : Set.of();

        return UsuarioResponse.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .correo(usuario.getCorreo())
                .telefono(usuario.getTelefono())
                .estado(usuario.getEstado())
                .fechaRegistro(usuario.getFechaRegistro())
                .roles(nombresRoles)
                .build();
    }

    private Integer obtenerIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            try {
                return Integer.parseInt(auth.getPrincipal().toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private boolean esUsuarioVisibleParaConsulta(Usuario usuario, Integer currentUserId) {
        if (currentUserId == null) {
            return true;
        }
        if (usuario.getIdUsuario() != null && usuario.getIdUsuario().equals(currentUserId)) {
            return true;
        }
        boolean esClienteOProveedor = usuario.getRoles() != null && usuario.getRoles().stream()
                .anyMatch(r -> TipoRol.CLIENTE.getNombre().equalsIgnoreCase(r.getNombreRol())
                        || TipoRol.PROVEEDOR.getNombre().equalsIgnoreCase(r.getNombreRol()));
        return esClienteOProveedor;
    }
}
