package com.udea.masters.repositories;

import com.udea.masters.entities.HorarioProveedor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HorarioProveedorRepository extends JpaRepository<HorarioProveedor, Integer> {

    @EntityGraph(attributePaths = "bloques")
    Optional<HorarioProveedor> findByProveedorUsuarioId(Integer proveedorId);
}
