package com.udea.masters.repositories;

import com.udea.masters.entities.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Lectura de negocios (tabla administrada por Business) para validar al propietario.
 */
@Repository
public interface NegocioRepository extends JpaRepository<Negocio, Integer> {
}
