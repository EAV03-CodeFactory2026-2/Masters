package com.udea.masters.services;

import com.udea.masters.entities.ModalidadServicio;
import com.udea.masters.repositories.ModalidadServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModalidadService implements IModalidadService {

    private final ModalidadServicioRepository modalidadRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ModalidadServicio> listarModalidades() {
        return modalidadRepository.findAll();
    }
}
