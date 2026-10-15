package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.HorarioSemanalRequest;
import com.udea.masters.controllers.DTOs.HorarioSemanalResponse;
import com.udea.masters.entities.BloqueHorario;
import com.udea.masters.entities.HorarioProveedor;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.exceptions.AccesoDenegadoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.repositories.HorarioProveedorRepository;
import com.udea.masters.repositories.PerfilProveedorRepository;
import com.udea.masters.security.IAuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioService implements IHorarioService {

    public static final String MSG_SOLO_PROVEEDOR = "Solo un Proveedor puede gestionar su horario de atención.";
    public static final String MSG_SIN_PERFIL = "No encontramos el perfil de proveedor de tu cuenta.";
    public static final String MSG_GUARDADO = "Horario guardado correctamente.";

    private final HorarioProveedorRepository horarioRepository;
    private final PerfilProveedorRepository perfilProveedorRepository;
    private final HorarioValidator horarioValidator;
    private final IAuthenticationFacade authenticationFacade;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public HorarioSemanalResponse consultarMiHorario() {
        Integer proveedorId = validarProveedor();
        HorarioProveedor horario = horarioRepository.findByProveedorUsuarioId(proveedorId).orElse(null);
        return HorarioSemanalResponse.desde(horario, null);
    }

    /**
     * Reemplaza por completo el horario del proveedor. Si alguna regla falla, no se guarda nada.
     */
    @Override
    @Transactional
    public HorarioSemanalResponse guardarMiHorario(HorarioSemanalRequest request) {
        Integer proveedorId = validarProveedor();
        List<BloqueHorario> bloques = horarioValidator.validar(request);

        HorarioProveedor horario = horarioRepository.findByProveedorUsuarioId(proveedorId)
                .orElseGet(() -> HorarioProveedor.builder().proveedor(buscarPerfil(proveedorId)).build());
        horario.reemplazarBloques(bloques, OffsetDateTime.now(clock));

        return HorarioSemanalResponse.desde(horarioRepository.save(horario), MSG_GUARDADO);
    }

    /** Verifica que se opere con el rol Proveedor y que exista su perfil; devuelve su id. */
    private Integer validarProveedor() {
        if (!authenticationFacade.tieneRol(TipoRol.PROVEEDOR.getNombre())) {
            throw new AccesoDenegadoException(MSG_SOLO_PROVEEDOR);
        }
        Integer usuarioId = authenticationFacade.getAuthenticatedUserId();
        if (usuarioId == null || !perfilProveedorRepository.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException(MSG_SIN_PERFIL);
        }
        return usuarioId;
    }

    private PerfilProveedor buscarPerfil(Integer proveedorId) {
        return perfilProveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_SIN_PERFIL));
    }
}
