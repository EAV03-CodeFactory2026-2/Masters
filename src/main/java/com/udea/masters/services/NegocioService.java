package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.NegocioCreateRequest;
import com.udea.masters.entities.Negocio;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.repositories.NegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NegocioService implements INegocioService {

    private final NegocioRepository negocioRepository;

    @Override
    @Transactional
    public Negocio crearNegocio(NegocioCreateRequest request, Integer usuarioIdAutenticado) {
        return crearNegocio(request, usuarioIdAutenticado, tieneRolPropietarioEnContexto());
    }

    @Override
    @Transactional
    public Negocio crearNegocio(NegocioCreateRequest request, Integer usuarioIdAutenticado, boolean esPropietario) {
        if (negocioRepository.existsByIdentificacionFiscalIgnoreCase(request.getIdentificacionFiscal())) {
            throw new RecursoDuplicadoException("La identificación fiscal ya está en uso en la plataforma.");
        }

        if (!esPropietario) {
            throw new IllegalArgumentException("El usuario especificado no tiene el rol de Propietario.");
        }

        Negocio negocio = Negocio.builder()
                .propietarioId(usuarioIdAutenticado)
                .nombreNegocio(request.getNombreNegocio())
                .direccionNegocio(request.getDireccionNegocio())
                .identificacionFiscal(request.getIdentificacionFiscal())
                .telefono(request.getTelefono())
                .monedaCodigoIso(request.getMonedaCodigoIso())
                .estado("Activo")
                .build();

        return negocioRepository.save(negocio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Negocio> listarMisNegocios(Integer usuarioIdAutenticado) {
        return listarMisNegocios(usuarioIdAutenticado, tieneRolPropietarioEnContexto());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Negocio> listarMisNegocios(Integer usuarioIdAutenticado, boolean esPropietario) {
        if (!esPropietario) {
            throw new IllegalArgumentException("El usuario no tiene el rol de Propietario.");
        }

        return negocioRepository.findByPropietarioId(usuarioIdAutenticado);
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
