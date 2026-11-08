package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.CrearFranjaRequest;
import com.udea.masters.controllers.DTOs.FranjaServicioResponse;
import com.udea.masters.entities.*;
import com.udea.masters.exceptions.AccesoDenegadoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.*;
import com.udea.masters.security.IAuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FranjaServicioService implements IFranjaServicioService {

    public static final String MSG_FRANJA_CREADA = "Franja creada correctamente.";
    public static final String MSG_HORARIO_NO_CABE = "La franja debe estar completa dentro de tu horario de atención.";
    public static final String MSG_FRANJA_SUPERPUESTA = "Esta franja se cruza con otra ya definida.";
    public static final String MSG_SIN_PERMISO = "No tienes permiso para definir franjas de este servicio.";
    public static final String MSG_SIN_PERMISO_MODIFICAR = "No tienes permiso para modificar este servicio.";
    public static final String MSG_SERVICIO_NO_ENCONTRADO = "Servicio no encontrado.";
    public static final String MSG_PROVEEDOR_NO_ENCONTRADO = "Proveedor no encontrado.";
    public static final String MSG_FRANJA_NO_ENCONTRADA = "Franja no encontrada.";
    public static final String MSG_DIA_OBLIGATORIO = "El día de la semana es obligatorio.";
    public static final String MSG_HORA_OBLIGATORIA = "La hora de inicio es obligatoria y debe tener formato HH:mm.";
    public static final String MSG_PROVEEDOR_OBLIGATORIO = "Debes indicar el proveedor asignado al servicio.";

    private static final Pattern FORMATO_HORA = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final FranjaServicioRepository franjaServicioRepository;
    private final ServicioRepository servicioRepository;
    private final PerfilProveedorRepository perfilProveedorRepository;
    private final HorarioProveedorRepository horarioProveedorRepository;
    private final ServicioProveedorRepository servicioProveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final IAuthenticationFacade authenticationFacade;
    private final Clock clock;

    @Override
    @Transactional
    public FranjaServicioResponse crearFranja(Integer servicioIdParam, CrearFranjaRequest request) {
        if (request == null) {
            throw new ReglaNegocioException("La solicitud no puede estar vacía.");
        }

        Integer servicioId = servicioIdParam != null ? servicioIdParam : request.getServicioId();
        if (servicioId == null) {
            throw new ReglaNegocioException("El identificador del servicio es obligatorio.");
        }

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_SERVICIO_NO_ENCONTRADO));

        Integer usuarioAutenticadoId = authenticationFacade.getAuthenticatedUserId();
        if (usuarioAutenticadoId == null) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO);
        }

        boolean esProveedor = authenticationFacade.tieneRol(TipoRol.PROVEEDOR.getNombre());
        boolean esPropietario = authenticationFacade.tieneRol(TipoRol.PROPIETARIO.getNombre());

        if (!esProveedor && !esPropietario) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO);
        }

        Integer proveedorId;
        if (esProveedor) {
            // Un proveedor define franjas para sí mismo
            if (request.getProveedorId() != null && !request.getProveedorId().equals(usuarioAutenticadoId)) {
                throw new AccesoDenegadoException(MSG_SIN_PERMISO);
            }
            proveedorId = usuarioAutenticadoId;
        } else {
            // El propietario define franjas para proveedores asignados a servicios de su negocio
            proveedorId = request.getProveedorId();
            if (proveedorId == null) {
                throw new ReglaNegocioException(MSG_PROVEEDOR_OBLIGATORIO);
            }
            if (!servicio.getNegocio().getPropietarioId().equals(usuarioAutenticadoId)) {
                throw new AccesoDenegadoException(MSG_SIN_PERMISO);
            }
        }

        boolean asignado = servicioProveedorRepository.existsByServicioIdAndProveedorId(servicioId, proveedorId);
        if (!asignado) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO);
        }

        DiaSemana diaSemana = normalizarDia(request.getDiaSemana());
        LocalTime horaInicio = parsearHora(request.getHoraInicio());
        LocalTime horaFin = horaInicio.plusMinutes(servicio.getDuracionMinutos());

        if (horaFin.isBefore(horaInicio)) {
            throw new ReglaNegocioException("La franja no puede exceder el final del día.");
        }

        validarHorarioAtencionProveedor(proveedorId, diaSemana, horaInicio, horaFin);

        validarNoSolapamientoFranjas(servicioId, proveedorId, diaSemana, horaInicio, horaFin);

        PerfilProveedor perfilProveedor = perfilProveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_PROVEEDOR_NO_ENCONTRADO));
        Usuario creadaPor = usuarioRepository.findById(usuarioAutenticadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        FranjaServicio nuevaFranja = FranjaServicio.builder()
                .servicio(servicio)
                .proveedor(perfilProveedor)
                .diaSemana(diaSemana)
                .horaInicio(horaInicio)
                .creadaPor(creadaPor)
                .fechaCreacion(OffsetDateTime.now(clock))
                .build();

        FranjaServicio guardada = franjaServicioRepository.save(nuevaFranja);
        return FranjaServicioResponse.desde(guardada, MSG_FRANJA_CREADA);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FranjaServicioResponse> consultarFranjas(Integer servicioId, Integer proveedorId, String diaSemanaStr) {
        if (servicioId == null) {
            throw new ReglaNegocioException("El identificador del servicio es obligatorio.");
        }
        if (!servicioRepository.existsById(servicioId)) {
            throw new RecursoNoEncontradoException(MSG_SERVICIO_NO_ENCONTRADO);
        }

        List<FranjaServicio> franjas;

        boolean tieneProveedor = proveedorId != null;
        boolean tieneDia = diaSemanaStr != null && !diaSemanaStr.trim().isEmpty();

        if (tieneProveedor && tieneDia) {
            DiaSemana dia = normalizarDia(diaSemanaStr);
            franjas = franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(servicioId, proveedorId, dia);

        } else if (tieneProveedor) {
            franjas = franjaServicioRepository.findByServicioAndProveedor(servicioId, proveedorId);

        } else if (tieneDia) {
            DiaSemana dia = normalizarDia(diaSemanaStr);
            franjas = franjaServicioRepository.findByServicioAndDiaSemana(servicioId, dia);

        } else {
            franjas = franjaServicioRepository.findByServicio(servicioId);
        }

        return franjas.stream()
                .map(f -> FranjaServicioResponse.desde(f, null))
                .toList();
    }

    @Override
    @Transactional
    public void eliminarFranja(Integer servicioId, Integer franjaId) {
        if (franjaId == null) {
            throw new ReglaNegocioException("El identificador de la franja es obligatorio.");
        }

        FranjaServicio franja = franjaServicioRepository.findById(franjaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_FRANJA_NO_ENCONTRADA));

        if (servicioId != null && !franja.getServicio().getIdServicio().equals(servicioId)) {
            throw new ReglaNegocioException("La franja no pertenece al servicio indicado.");
        }

        Integer usuarioAutenticadoId = authenticationFacade.getAuthenticatedUserId();
        if (usuarioAutenticadoId == null) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO_MODIFICAR);
        }

        boolean esProveedor = authenticationFacade.tieneRol(TipoRol.PROVEEDOR.getNombre());
        boolean esPropietario = authenticationFacade.tieneRol(TipoRol.PROPIETARIO.getNombre());

        boolean puedeEliminar = false;
        if (esProveedor && franja.getProveedor().getUsuarioId().equals(usuarioAutenticadoId)) {
            puedeEliminar = true;
        } else if (esPropietario && franja.getServicio().getNegocio().getPropietarioId().equals(usuarioAutenticadoId)) {
            puedeEliminar = true;
        }

        if (!puedeEliminar) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO_MODIFICAR);
        }

        franjaServicioRepository.delete(franja);
    }

    private void validarHorarioAtencionProveedor(Integer proveedorId, DiaSemana diaSemana,
                                                LocalTime horaInicio, LocalTime horaFin) {
        HorarioProveedor horario = horarioProveedorRepository.findByProveedorUsuarioId(proveedorId).orElse(null);
        if (horario == null || horario.getBloques() == null || horario.getBloques().isEmpty()) {
            throw new ReglaNegocioException(MSG_HORARIO_NO_CABE);
        }

        boolean cabeEnBloque = horario.getBloques().stream()
                .filter(b -> b.getDiaSemana() == diaSemana)
                .anyMatch(b -> !horaInicio.isBefore(b.getHoraInicio()) && !horaFin.isAfter(b.getHoraFin()));

        if (!cabeEnBloque) {
            throw new ReglaNegocioException(MSG_HORARIO_NO_CABE);
        }
    }

    private void validarNoSolapamientoFranjas(Integer servicioId, Integer proveedorId, DiaSemana diaSemana,
                                             LocalTime nuevaInicio, LocalTime nuevaFin) {
        List<FranjaServicio> existentes = franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(
                servicioId, proveedorId, diaSemana);

        boolean seCruza = existentes.stream().anyMatch(f -> {
            LocalTime fInicio = f.getHoraInicio();
            LocalTime fFin = f.getHoraFin();
            return nuevaInicio.isBefore(fFin) && fInicio.isBefore(nuevaFin);
        });

        if (seCruza) {
            throw new ReglaNegocioException(MSG_FRANJA_SUPERPUESTA);
        }
    }

    private DiaSemana normalizarDia(String diaStr) {
        if (diaStr == null || diaStr.trim().isEmpty()) {
            throw new ReglaNegocioException(MSG_DIA_OBLIGATORIO);
        }
        String limpio = Normalizer.normalize(diaStr.trim().toUpperCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        try {
            return DiaSemana.valueOf(limpio);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("El día '" + diaStr + "' no es válido.");
        }
    }

    private LocalTime parsearHora(String horaStr) {
        if (horaStr == null || !FORMATO_HORA.matcher(horaStr.trim()).matches()) {
            throw new ReglaNegocioException(MSG_HORA_OBLIGATORIA);
        }
        return LocalTime.parse(horaStr.trim());
    }
}
