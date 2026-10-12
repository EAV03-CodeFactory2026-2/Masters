package com.udea.masters.repositories;

import com.udea.masters.entities.ModalidadServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModalidadServicioRepository extends JpaRepository<ModalidadServicio, Integer> {
    Optional<ModalidadServicio> findByNombreModalidad(String nombreModalidad);
}
