package com.udea.masters.repositories;

import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilProveedorRepository extends JpaRepository<PerfilProveedor, Integer> {
    boolean existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(TipoDocumento tipoDocumento, String numeroDocumento);
    Optional<PerfilProveedor> findByTipoDocumentoAndNumeroDocumentoIgnoreCase(TipoDocumento tipoDocumento, String numeroDocumento);

    @Query("SELECT p FROM PerfilProveedor p WHERE " +
           "(p.usuario.estado = 'Activo') AND (" +
           "LOWER(p.usuario.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.usuario.apellido) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(p.usuario.nombre, ' ', p.usuario.apellido)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.especialidad) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<PerfilProveedor> buscarEnDirectorio(@Param("query") String query);

    @Query("SELECT p FROM PerfilProveedor p WHERE p.usuario.estado = 'Activo'")
    List<PerfilProveedor> buscarTodosActivos();
}
