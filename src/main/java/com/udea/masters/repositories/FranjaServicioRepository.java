package com.udea.masters.repositories;

import com.udea.masters.entities.DiaSemana;
import com.udea.masters.entities.FranjaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FranjaServicioRepository extends JpaRepository<FranjaServicio, Integer> {

    @Query("SELECT f FROM FranjaServicio f "
            + "JOIN FETCH f.servicio s "
            + "JOIN FETCH f.proveedor p "
            + "JOIN FETCH p.usuario u "
            + "JOIN FETCH f.creadaPor c "
            + "WHERE f.servicio.idServicio = :servicioId "
            + "AND f.proveedor.usuarioId = :proveedorId "
            + "AND f.diaSemana = :diaSemana "
            + "ORDER BY f.horaInicio ASC")
    List<FranjaServicio> findByServicioAndProveedorAndDiaSemana(
            @Param("servicioId") Integer servicioId,
            @Param("proveedorId") Integer proveedorId,
            @Param("diaSemana") DiaSemana diaSemana);

    @Query("SELECT f FROM FranjaServicio f "
            + "JOIN FETCH f.servicio s "
            + "JOIN FETCH f.proveedor p "
            + "JOIN FETCH p.usuario u "
            + "JOIN FETCH f.creadaPor c "
            + "WHERE f.servicio.idServicio = :servicioId "
            + "AND f.proveedor.usuarioId = :proveedorId "
            + "ORDER BY f.diaSemana, f.horaInicio ASC")
    List<FranjaServicio> findByServicioAndProveedor(
            @Param("servicioId") Integer servicioId,
            @Param("proveedorId") Integer proveedorId);

    @Query("SELECT f FROM FranjaServicio f "
            + "JOIN FETCH f.servicio s "
            + "JOIN FETCH f.proveedor p "
            + "JOIN FETCH p.usuario u "
            + "JOIN FETCH f.creadaPor c "
            + "WHERE f.servicio.idServicio = :servicioId "
            + "ORDER BY f.diaSemana, f.horaInicio ASC")
    List<FranjaServicio> findByServicio(@Param("servicioId") Integer servicioId);

    @Query("SELECT f FROM FranjaServicio f "
            + "JOIN FETCH f.servicio s "
            + "JOIN FETCH f.proveedor p "
            + "JOIN FETCH p.usuario u "
            + "JOIN FETCH f.creadaPor c "
            + "WHERE f.servicio.idServicio = :servicioId "
            + "AND f.diaSemana = :diaSemana "
            + "ORDER BY f.horaInicio ASC")
    List<FranjaServicio> findByServicioAndDiaSemana(
            @Param("servicioId") Integer servicioId,
            @Param("diaSemana") DiaSemana diaSemana);
}
