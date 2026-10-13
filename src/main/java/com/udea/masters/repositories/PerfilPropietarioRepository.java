package com.udea.masters.repositories;

import com.udea.masters.entities.PerfilPropietario;
import com.udea.masters.entities.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilPropietarioRepository extends JpaRepository<PerfilPropietario, Integer> {
    boolean existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(TipoDocumento tipoDocumento, String numeroDocumento);
    Optional<PerfilPropietario> findByTipoDocumentoAndNumeroDocumentoIgnoreCase(TipoDocumento tipoDocumento, String numeroDocumento);
}
