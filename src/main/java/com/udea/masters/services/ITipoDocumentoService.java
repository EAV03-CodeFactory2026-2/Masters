package com.udea.masters.services;

import com.udea.masters.entities.TipoDocumento;
import java.util.List;
import java.util.Optional;

public interface ITipoDocumentoService {
    List<TipoDocumento> listarTiposDocumento();
    Optional<TipoDocumento> obtenerPorCodigo(String codigo);
}
