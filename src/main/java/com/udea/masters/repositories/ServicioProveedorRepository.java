package com.udea.masters.repositories;

import com.udea.masters.entities.ServicioProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioProveedorRepository extends JpaRepository<ServicioProveedor, Integer> {

    boolean existsByServicioIdAndProveedorId(
            Integer servicioId,
            Integer proveedorId
    );

    List<ServicioProveedor> findByServicioId(Integer servicioId);

    List<ServicioProveedor> findByProveedorId(Integer proveedorId);
}