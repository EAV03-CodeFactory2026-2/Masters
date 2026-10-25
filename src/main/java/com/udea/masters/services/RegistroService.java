package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RegistroResponse;
import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.Rol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.RolRepository;
import com.udea.masters.repositories.UsuarioRepository;
import com.udea.masters.security.IJwtService;
import com.udea.masters.services.strategy.IRegistroRolStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RegistroService implements IRegistroService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,64}$");

    private final List<IRegistroRolStrategy> strategies;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;

    @Override
    @Transactional
    public RegistroResponse registrar(RegistroUsuarioRequest request) {
        String rolSolicitado = Optional.ofNullable(request)
                .map(RegistroUsuarioRequest::getRol)
                .orElse("");
        if (!StringUtils.hasText(rolSolicitado)) {
            throw new ReglaNegocioException("Debes indicar el rol con el que te registras.");
        }

        // H-05: Diferenciar entre rol vacío (validado arriba) y rol no soportado
        IRegistroRolStrategy strategy = strategies.stream()
                .filter(s -> s.soporta(rolSolicitado))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("El rol indicado no es válido."));

        if (!StringUtils.hasText(request.getCorreo())) {
            throw new ReglaNegocioException("El correo es obligatorio");
        }

        if (usuarioRepository.existsByCorreoIgnoreCase(request.getCorreo().trim())) {
            throw new RecursoDuplicadoException("Ya existe una cuenta con este correo. Inicia sesión y agrega el rol desde Mi cuenta.");
        }

        if (!PASSWORD_PATTERN.matcher(Objects.requireNonNullElse(request.getContrasena(), "")).matches()) {
            throw new ReglaNegocioException("La contraseña debe tener entre 8 y 64 caracteres, una mayúscula, una minúscula, un número y un carácter especial.");
        }

        strategy.validar(request);

        Rol rol = rolRepository.findByNombreRol(strategy.getNombreRol())
                .orElseGet(() -> rolRepository.save(Rol.builder().nombreRol(strategy.getNombreRol()).build()));

        Usuario usuario = Usuario.builder()
                .nombre(Objects.requireNonNullElse(request.getNombre(), "").trim())
                .apellido(Objects.requireNonNullElse(request.getApellido(), "").trim())
                .correo(request.getCorreo().trim().toLowerCase())
                .telefono(Objects.requireNonNullElse(request.getTelefono(), "").trim())
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .estado("Activo")
                .roles(new HashSet<>(Set.of(rol)))
                .build();

        usuario = usuarioRepository.save(usuario);

        strategy.procesarRegistro(request, usuario);

        String token = jwtService.generateToken(
                usuario.getIdUsuario(),
                usuario.getCorreo(),
                List.of(strategy.getNombreRol()),
                strategy.getNombreRol()
        );

        return RegistroResponse.builder()
                .token(token)
                .tipoToken("Bearer")
                .idUsuario(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .correo(usuario.getCorreo())
                .rolActivo(strategy.getNombreRol())
                .build();
    }
}
