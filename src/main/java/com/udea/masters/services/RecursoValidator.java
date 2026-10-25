package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RecursoCreateRequest;
import com.udea.masters.entities.TipoRecurso;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.util.TextoNormalizador;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

/**
 * Reglas de los datos de un recurso físico. Devuelve los valores ya limpios, listos para guardar.
 */
@Component
public class RecursoValidator {

    public static final String MSG_CANTIDAD_INVALIDA = "La cantidad debe ser un número entero entre 1 y 999.";
    public static final String MSG_NOMBRE_OBLIGATORIO = "El nombre del recurso es obligatorio.";
    public static final String MSG_NOMBRE_LARGO = "El nombre del recurso no puede exceder los 100 caracteres.";
    public static final String MSG_TIPO_INVALIDO = "El tipo del recurso debe ser Espacio o Equipo.";
    public static final String MSG_DESCRIPCION_LARGA = "La descripción no puede exceder los 500 caracteres.";

    private static final Set<Class<?>> TIPOS_ENTEROS = Set.of(
            Integer.class, Long.class, Short.class, BigInteger.class, String.class);
    private static final int NOMBRE_MAX = 100;
    private static final int DESCRIPCION_MAX = 500;

    // H-06: Expresión sin \+? para rechazar el signo '+'; acepta solo dígitos en el rango 1..999
    private static final String PATRON_CANTIDAD = "0*[1-9]\\d{0,2}";

    public record DatosRecurso(String nombre, String nombreNormalizado, TipoRecurso tipo,
                               Integer cantidad, String descripcion) {
    }

    public DatosRecurso validar(RecursoCreateRequest request) {
        String nombreRaw = Optional.ofNullable(request).map(RecursoCreateRequest::getNombre).orElse(null);
        if (!StringUtils.hasText(nombreRaw)) {
            throw new ReglaNegocioException(MSG_NOMBRE_OBLIGATORIO);
        }
        String nombre = nombreRaw.trim().replaceAll("\\s+", " ");
        if (nombre.length() > NOMBRE_MAX) {
            throw new ReglaNegocioException(MSG_NOMBRE_LARGO);
        }

        TipoRecurso tipo = TipoRecurso.desdeNombre(TextoNormalizador.normalizar(request.getTipo()))
                .orElseThrow(() -> new ReglaNegocioException(MSG_TIPO_INVALIDO));

        Object cantidadObj = request.getCantidad();
        String cantidadTexto = TIPOS_ENTEROS.contains(Optional.ofNullable(cantidadObj).map(Object::getClass).orElse(Void.class))
                ? cantidadObj.toString().trim()
                : "";
        if (!cantidadTexto.matches(PATRON_CANTIDAD)) {
            throw new ReglaNegocioException(MSG_CANTIDAD_INVALIDA);
        }

        String descripcion = Optional.ofNullable(request.getDescripcion())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(null);
        if (descripcion != null && descripcion.length() > DESCRIPCION_MAX) {
            throw new ReglaNegocioException(MSG_DESCRIPCION_LARGA);
        }

        return new DatosRecurso(
                nombre,
                TextoNormalizador.normalizar(nombre),
                tipo,
                Integer.valueOf(cantidadTexto),
                descripcion);
    }
}
