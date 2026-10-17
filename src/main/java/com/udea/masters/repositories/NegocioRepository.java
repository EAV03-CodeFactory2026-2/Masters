package com.udea.masters.repositories;

import com.udea.masters.entities.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NegocioRepository extends JpaRepository<Negocio, Integer> {
    List<Negocio> findByPropietarioId(Integer propietarioId);
    boolean existsByIdentificacionFiscalIgnoreCase(String identificacionFiscal);
}

