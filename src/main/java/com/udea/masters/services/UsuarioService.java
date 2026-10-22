package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.entities.Rol;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.RolRepository;
import com.udea.masters.repositories.UsuarioRepository;
import com.udea.masters.security.IAuthenticationFacade;
import com.udea.masters.services.strategy.IRolPerfilStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final List<IRolPerfilStrategy> rolPerfilStrategies;
    private final IAuthenticationFacade authenticationFacade;

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorId(Integer idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);

        Integer currentUserId = authenticationFacade.getAuthenticatedUserId();
        if (!esUsuarioVisibleParaConsulta(usuario, currentUserId)) {
            throw new AccessDeniedException("No tienes permisos para consultar la información de otros propietarios.");
        }

        return mapToResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        Integer currentUserId = authenticationFacade.getAuthenticatedUserId();

        return usuarioRepository.findAll().stream()
                .filter(u -> esUsuarioVisibleParaConsulta(u, currentUserId))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarPorRol(String nombreRol) {
        Integer currentUserId = authenticationFacade.getAuthenticatedUserId();
        boolean filtroPropietario = TipoRol.PROPIETARIO.getNombre().equalsIgnoreCase(nombreRol);

        return usuarioRepository.findByRolesNombreRol(nombreRol).stream()
                .filter(u -> filtroPropietario
                        ? currentUserId != null && currentUserId.equals(u.getIdUsuario())
                        : esUsuarioVisibleParaConsulta(u, currentUserId))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void asignarRolAUsuario(Integer idUsuario, RolAsignacionRequest request) {
        String nombreRol = Optional.ofNullable(request)
                .map(RolAsignacionRequest::getNombreRol)
                .orElse("");
        if (!StringUtils.hasText(nombreRol)) {
            throw new ReglaNegocioException("Debes indicar el rol a asignar.");
        }

        Usuario usuario = buscarUsuario(idUsuario);
        Rol rol = buscarRol(nombreRol.trim());

        boolean yaTieneRol = usuario.getRoles().stream()
                .map(Rol::getNombreRol)
                .anyMatch(rol.getNombreRol()::equalsIgnoreCase);
        if (yaTieneRol) {
            throw new ReglaNegocioException("El usuario ya tiene asignado el rol " + rol.getNombreRol() + ".");
        }

        rolPerfilStrategies.stream()
                .filter(s -> s.soporta(rol.getNombreRol()))
                .findFirst()
                .ifPresent(strategy -> strategy.asignarPerfil(usuario, request));

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
        Usuario usuario = buscarUsuario(idUsuario);
        Rol rol = buscarRol(nombreRol);

        boolean tieneRol = usuario.getRoles().stream()
                .map(Rol::getNombreRol)
                .anyMatch(rol.getNombreRol()::equalsIgnoreCase);
        if (!tieneRol) {
            throw new ReglaNegocioException("El usuario no tiene asignado el rol " + rol.getNombreRol() + ".");
        }

        if (usuario.getRoles().size() <= 1) {
            throw new ReglaNegocioException("No se puede eliminar el único rol del usuario.");
        }

        rolPerfilStrategies.stream()
                .filter(s -> s.soporta(rol.getNombreRol()))
                .findFirst()
                .ifPresent(strategy -> strategy.removerPerfil(idUsuario));

        usuario.getRoles().removeIf(r -> r.getNombreRol().equalsIgnoreCase(rol.getNombreRol()));
        usuarioRepository.save(usuario);
    }

    private Usuario buscarUsuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + idUsuario));
    }

    private Rol buscarRol(String nombreRol) {
        return rolRepository.findByNombreRolIgnoreCase(nombreRol)
                .orElseThrow(() -> new ReglaNegocioException("Rol no encontrado con nombre: " + nombreRol));
    }

    private UsuarioResponse mapToResponse(Usuario usuario) {
        Set<String> nombresRoles = Objects.requireNonNullElse(usuario.getRoles(), Set.<Rol>of()).stream()
                .map(Rol::getNombreRol)
                .collect(Collectors.toSet());

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

    private boolean esUsuarioVisibleParaConsulta(Usuario usuario, Integer currentUserId) {
        if (currentUserId == null || currentUserId.equals(usuario.getIdUsuario())) {
            return true;
        }
        return Objects.requireNonNullElse(usuario.getRoles(), Set.<Rol>of()).stream()
                .map(Rol::getNombreRol)
                .anyMatch(r -> TipoRol.CLIENTE.getNombre().equalsIgnoreCase(r)
                        || TipoRol.PROVEEDOR.getNombre().equalsIgnoreCase(r));
    }
}
