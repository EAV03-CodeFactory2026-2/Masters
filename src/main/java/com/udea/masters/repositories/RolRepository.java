package com.udea.masters.repositories;

import com.udea.masters.entities.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombreRol(String nombreRol);
    Optional<Rol> findByNombreRolIgnoreCase(String nombreRol);
}
