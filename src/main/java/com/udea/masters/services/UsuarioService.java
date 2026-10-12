package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.UsuarioCreateRequest;
import com.udea.masters.controllers.DTOs.UsuarioResponse;
import com.udea.masters.entities.Rol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.repositories.RolRepository;
import com.udea.masters.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UsuarioResponse crearUsuario(UsuarioCreateRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo especificado.");
        }

        Set<Rol> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String nombreRol : request.getRoles()) {
                Rol rol = rolRepository.findByNombreRol(nombreRol)
                        .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + nombreRol));
                roles.add(rol);
            }
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .correo(request.getCorreo())
                .telefono(request.getTelefono())
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .estado("Activo")
                .roles(roles)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return mapToResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorId(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + idUsuario));
        return mapToResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarPorRol(String nombreRol) {
        return usuarioRepository.findByRolesNombreRol(nombreRol).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void asignarRolAUsuario(Integer idUsuario, String nombreRol) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + idUsuario));

        Rol rol = rolRepository.findByNombreRol(nombreRol)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado con nombre: " + nombreRol));

        usuario.getRoles().add(rol);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void removerRolDeUsuario(Integer idUsuario, String nombreRol) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + idUsuario));

        Rol rol = rolRepository.findByNombreRol(nombreRol)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado con nombre: " + nombreRol));

        usuario.getRoles().remove(rol);
        usuarioRepository.save(usuario);
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
}
