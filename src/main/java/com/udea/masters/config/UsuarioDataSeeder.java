package com.udea.masters.config;

import com.udea.masters.entities.Rol;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.entities.Usuario;
import com.udea.masters.repositories.RolRepository;
import com.udea.masters.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Order(2)
@RequiredArgsConstructor
public class UsuarioDataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@udea.edu.co}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByCorreo(adminEmail)) {
            Rol rolPropietario = rolRepository.findByNombreRol(TipoRol.PROPIETARIO.getNombre())
                    .orElseThrow(() -> new IllegalStateException("El rol Propietario debe existir previamente en BD."));

            Usuario admin = Usuario.builder()
                    .nombre("Administrador")
                    .apellido("Inicial")
                    .correo(adminEmail)
                    .telefono("0000000000")
                    .contrasena(passwordEncoder.encode(adminPassword))
                    .estado("Activo")
                    .roles(Set.of(rolPropietario))
                    .build();

            usuarioRepository.save(admin);
        }
    }
}
