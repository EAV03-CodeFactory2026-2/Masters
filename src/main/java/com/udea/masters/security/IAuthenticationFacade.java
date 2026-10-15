package com.udea.masters.security;

public interface IAuthenticationFacade {
    Integer getAuthenticatedUserId();

    /** Indica si el usuario opera con el rol dado (el filtro JWT solo concede el rol activo del token). */
    boolean tieneRol(String nombreRol);
}
