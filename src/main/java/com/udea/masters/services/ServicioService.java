package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.ServicioCreateRequest;
import com.udea.masters.entities.Negocio;
import com.udea.masters.entities.Servicio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.repositories.NegocioRepository;
import com.udea.masters.repositories.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioService implements IServicioService {

    private final ServicioRepository servicioRepository;
    private final NegocioRepository negocioRepository;

    @Override
    @Transactional
    public Servicio crearServicio(ServicioCreateRequest request, Integer usuarioIdAutenticado) {
        return crearServicio(request, usuarioIdAutenticado, tieneRolPropietarioEnContexto());
    }

    @Override
    @Transactional
    public Servicio crearServicio(ServicioCreateRequest request, Integer usuarioIdAutenticado, boolean esPropietario) {
        if (!esPropietario) {
            throw new IllegalArgumentException("El usuario no tiene el rol de Propietario.");
        }

        Negocio negocio = negocioRepository.findById(request.getNegocioId())
                .orElseThrow(() -> new IllegalArgumentException("Negocio no encontrado."));

        if (!negocio.getPropietarioId().equals(usuarioIdAutenticado)) {
            throw new IllegalArgumentException("No tienes permisos para agregar servicios a este negocio.");
        }

        if (servicioRepository.existsByNegocioIdNegocioAndNombreIgnoreCase(request.getNegocioId(), request.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe un servicio con ese nombre en este negocio.");
        }

        Servicio servicio = Servicio.builder()
                .negocio(negocio)
                .modalidadId(request.getModalidadId())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .duracionMinutos(request.getDuracionMinutos())
                .precio(request.getPrecio())
                .estado("No Asignado")
                .build();

        return servicioRepository.save(servicio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Servicio> listarServiciosPorNegocio(Integer negocioId) {
        return servicioRepository.findByNegocioIdNegocio(negocioId);
    }

    private boolean tieneRolPropietarioEnContexto() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PROPIETARIO")
                        || a.getAuthority().equalsIgnoreCase("PROPIETARIO"));
    }
}
