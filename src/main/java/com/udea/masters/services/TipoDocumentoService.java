package com.udea.masters.services;

import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.repositories.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TipoDocumentoService implements ITipoDocumentoService {

    private final TipoDocumentoRepository tipoDocumentoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TipoDocumento> listarTiposDocumento() {
        return tipoDocumentoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoDocumento> obtenerPorCodigo(String codigo) {
        return tipoDocumentoRepository.findByCodigoIgnoreCase(codigo);
    }
}
