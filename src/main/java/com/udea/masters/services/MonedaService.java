package com.udea.masters.services;

import com.udea.masters.entities.Moneda;
import com.udea.masters.repositories.MonedaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonedaService implements IMonedaService {

    private final MonedaRepository monedaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Moneda> listarMonedas() {
        return monedaRepository.findAll();
    }
}
