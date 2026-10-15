package com.udea.masters.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFacade implements IAuthenticationFacade {

    @Override
    public Integer getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            try {
                return Integer.parseInt(auth.getPrincipal().toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @Override
    public boolean tieneRol(String nombreRol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || nombreRol == null) {
            return false;
        }
        String autoridad = "ROLE_" + nombreRol.toUpperCase();
        return auth.getAuthorities().stream().anyMatch(a -> autoridad.equalsIgnoreCase(a.getAuthority()));
    }
}
