package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.DirectorioProveedoresResponse;
import com.udea.masters.controllers.DTOs.ProveedorDirectorioResponse;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilProveedorRepository;
import com.udea.masters.security.IAuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProveedorService implements IProveedorService {

    public static final int TAMANO_PAGINA = 10;
    public static final int MIN_CARACTERES = 3;
    public static final int MAX_CARACTERES = 100;

    public static final String MSG_MIN_CARACTERES = "Escribe al menos 3 caracteres para buscar.";
    public static final String MSG_MAX_CARACTERES = "El criterio de búsqueda no puede superar los 100 caracteres.";
    public static final String MSG_PAGINA_INVALIDA = "La página debe ser un número mayor o igual a 1.";
    public static final String MSG_SIN_RESULTADOS = "No encontramos proveedores con ese criterio.";
    public static final String MSG_CON_RESULTADOS = "Proveedores encontrados.";

    private final PerfilProveedorRepository perfilProveedorRepository;
    private final IAuthenticationFacade authenticationFacade;

    @Override
    @Transactional(readOnly = true)
    public DirectorioProveedoresResponse consultarDirectorio(String criterio, Integer pagina) {
        String criterioLimpio = validarCriterio(criterio);
        int numeroPagina = validarPagina(pagina);

        String criterioMinusculas = criterioLimpio.toLowerCase(Locale.ROOT);
        Page<PerfilProveedor> resultado = perfilProveedorRepository.buscarEnDirectorio(
                "%" + escaparLike(criterioMinusculas) + "%",
                criterioMinusculas,
                TipoRol.PROVEEDOR.getNombre(),
                PageRequest.of(numeroPagina - 1, TAMANO_PAGINA));

        Integer usuarioActualId = authenticationFacade.getAuthenticatedUserId();
        return DirectorioProveedoresResponse.builder()
                .mensaje(resultado.getTotalElements() == 0 ? MSG_SIN_RESULTADOS : MSG_CON_RESULTADOS)
                .proveedores(resultado.getContent().stream()
                        .map(p -> mapearADirectorioResponse(p, usuarioActualId))
                        .toList())
                .pagina(numeroPagina)
                .tamanoPagina(TAMANO_PAGINA)
                .totalResultados(resultado.getTotalElements())
                .totalPaginas(resultado.getTotalPages())
                .hayMasPaginas(resultado.hasNext())
                .build();
    }

    private String validarCriterio(String criterio) {
        String limpio = criterio == null ? "" : criterio.trim().replaceAll("\\s+", " ");
        if (limpio.length() < MIN_CARACTERES) {
            throw new ReglaNegocioException(MSG_MIN_CARACTERES);
        }
        if (limpio.length() > MAX_CARACTERES) {
            throw new ReglaNegocioException(MSG_MAX_CARACTERES);
        }
        return limpio;
    }

    private int validarPagina(Integer pagina) {
        if (pagina == null) {
            return 1;
        }
        if (pagina < 1) {
            throw new ReglaNegocioException(MSG_PAGINA_INVALIDA);
        }
        return pagina;
    }

    /** Evita que %, _ o ! escritos por el usuario funcionen como comodines del LIKE. */
    private String escaparLike(String texto) {
        return texto.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private ProveedorDirectorioResponse mapearADirectorioResponse(PerfilProveedor perfil, Integer usuarioActualId) {
        return ProveedorDirectorioResponse.builder()
                .idUsuario(perfil.getUsuarioId())
                .nombre(perfil.getUsuario().getNombre())
                .apellido(perfil.getUsuario().getApellido())
                .correo(perfil.getUsuario().getCorreo())
                .telefono(perfil.getUsuario().getTelefono())
                .tipoDocumento(perfil.getTipoDocumento().getCodigo())
                .tipoDocumentoNombre(perfil.getTipoDocumento().getNombre())
                .numeroDocumento(perfil.getNumeroDocumento())
                .especialidad(perfil.getEspecialidad())
                .descripcionProfesional(perfil.getDescripcionProfesional())
                .esCuentaActual(Objects.equals(perfil.getUsuarioId(), usuarioActualId))
                .build();
    }
}
