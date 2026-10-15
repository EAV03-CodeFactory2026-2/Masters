package com.udea.masters.repositories;

import com.udea.masters.entities.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecursoRepository extends JpaRepository<Recurso, Integer> {
    List<Recurso> findByNegocioIdNegocioOrderByNombreAsc(Integer negocioId);
    boolean existsByNegocioIdNegocioAndNombreNormalizado(Integer negocioId, String nombreNormalizado);
}
