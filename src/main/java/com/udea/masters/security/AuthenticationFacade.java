package com.udea.masters.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticationFacade implements IAuthenticationFacade {

    @Override
    public Integer getAuthenticatedUserId() {
        try {
            return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                    .map(Authentication::getPrincipal)
                    .map(Object::toString)
                    .map(Integer::valueOf)
                    .orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public boolean tieneRol(String nombreRol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || nombreRol == null) {
            return false;
        }
        String autoridad = "ROLE_" + nombreRol.toUpperCase();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(autoridad::equalsIgnoreCase);
    }
}
