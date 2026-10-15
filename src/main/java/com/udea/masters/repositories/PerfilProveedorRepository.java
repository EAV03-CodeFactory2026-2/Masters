package com.udea.masters.repositories;

import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilProveedorRepository extends JpaRepository<PerfilProveedor, Integer> {
    boolean existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(TipoDocumento tipoDocumento, String numeroDocumento);
    Optional<PerfilProveedor> findByTipoDocumentoAndNumeroDocumentoIgnoreCase(TipoDocumento tipoDocumento, String numeroDocumento);

    String FILTRO_DIRECTORIO = "WHERE u.estado = 'Activo' "
            + "AND EXISTS (SELECT r FROM u.roles r WHERE r.nombreRol = :rol) "
            + "AND (LOWER(u.nombre) LIKE :patron ESCAPE '!' "
            + "OR LOWER(u.apellido) LIKE :patron ESCAPE '!' "
            + "OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE :patron ESCAPE '!' "
            + "OR LOWER(p.especialidad) LIKE :patron ESCAPE '!' "
            + "OR LOWER(p.numeroDocumento) = :documento) ";

    /**
     * Proveedores activos con el rol indicado que coinciden por nombre, apellido o especialidad (parcial)
     * o por número de documento (exacto). El patrón debe venir en minúsculas y escapado con '!'.
     */
    @Query(value = "SELECT p FROM PerfilProveedor p JOIN p.usuario u " + FILTRO_DIRECTORIO
            + "ORDER BY u.apellido, u.nombre, p.usuarioId",
            countQuery = "SELECT COUNT(p) FROM PerfilProveedor p JOIN p.usuario u " + FILTRO_DIRECTORIO)
    Page<PerfilProveedor> buscarEnDirectorio(@Param("patron") String patron,
                                             @Param("documento") String documento,
                                             @Param("rol") String rol,
                                             Pageable pageable);
}
