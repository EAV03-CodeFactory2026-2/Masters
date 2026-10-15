package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RecursoCreateRequest;
import com.udea.masters.controllers.DTOs.RecursoRegistradoResponse;
import com.udea.masters.controllers.DTOs.RecursoResponse;
import com.udea.masters.controllers.DTOs.RecursosNegocioResponse;
import com.udea.masters.entities.Negocio;
import com.udea.masters.entities.Recurso;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.exceptions.AccesoDenegadoException;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.repositories.NegocioRepository;
import com.udea.masters.repositories.RecursoRepository;
import com.udea.masters.security.IAuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecursoService implements IRecursoService {

    public static final String MSG_SOLO_PROPIETARIO = "Solo un Propietario puede gestionar los recursos del negocio.";
    public static final String MSG_SIN_PERMISO_NEGOCIO = "No tienes permiso para gestionar los recursos de este negocio.";
    public static final String MSG_NEGOCIO_NO_ENCONTRADO = "Negocio no encontrado.";
    public static final String MSG_NOMBRE_DUPLICADO = "Ya existe un recurso con este nombre en el negocio.";
    public static final String MSG_REGISTRADO = "Recurso registrado correctamente.";
    public static final String MSG_LISTA_VACIA = "Este negocio aún no tiene recursos registrados.";
    public static final String MSG_LISTA = "Recursos consultados correctamente.";

    private final RecursoRepository recursoRepository;
    private final NegocioRepository negocioRepository;
    private final RecursoValidator recursoValidator;
    private final IAuthenticationFacade authenticationFacade;

    @Override
    @Transactional
    public RecursoRegistradoResponse registrarRecurso(Integer negocioId, RecursoCreateRequest request) {
        // Los permisos se validan antes que los datos: sin permiso siempre es 403.
        Negocio negocio = validarAccesoANegocio(negocioId);
        RecursoValidator.DatosRecurso datos = recursoValidator.validar(request);

        if (recursoRepository.existsByNegocioIdNegocioAndNombreNormalizado(negocioId, datos.nombreNormalizado())) {
            throw new RecursoDuplicadoException(MSG_NOMBRE_DUPLICADO);
        }

        Recurso recurso = Recurso.builder()
                .negocio(negocio)
                .nombre(datos.nombre())
                .nombreNormalizado(datos.nombreNormalizado())
                .tipo(datos.tipo())
                .cantidad(datos.cantidad())
                .descripcion(datos.descripcion())
                .estado(Recurso.ESTADO_ACTIVO)
                .build();

        try {
            recurso = recursoRepository.saveAndFlush(recurso);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo nombre: los frena la restricción única de la BD.
            throw new RecursoDuplicadoException(MSG_NOMBRE_DUPLICADO);
        }
        return new RecursoRegistradoResponse(MSG_REGISTRADO, RecursoResponse.desde(recurso));
    }

    @Override
    @Transactional(readOnly = true)
    public RecursosNegocioResponse listarRecursos(Integer negocioId) {
        validarAccesoANegocio(negocioId);
        List<RecursoResponse> recursos = recursoRepository.findByNegocioIdNegocioOrderByNombreAsc(negocioId).stream()
                .map(RecursoResponse::desde)
                .toList();
        return new RecursosNegocioResponse(recursos.isEmpty() ? MSG_LISTA_VACIA : MSG_LISTA, recursos);
    }

    private Negocio validarAccesoANegocio(Integer negocioId) {
        if (!authenticationFacade.tieneRol(TipoRol.PROPIETARIO.getNombre())) {
            throw new AccesoDenegadoException(MSG_SOLO_PROPIETARIO);
        }
        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_NEGOCIO_NO_ENCONTRADO));
        if (!negocio.getPropietarioId().equals(authenticationFacade.getAuthenticatedUserId())) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO_NEGOCIO);
        }
        return negocio;
    }
}
