package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RecursoCreateRequest;
import com.udea.masters.entities.TipoRecurso;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.util.TextoNormalizador;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

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

    private static final BigInteger CANTIDAD_MINIMA = BigInteger.ONE;
    private static final BigInteger CANTIDAD_MAXIMA = BigInteger.valueOf(999);
    private static final int NOMBRE_MAX = 100;
    private static final int DESCRIPCION_MAX = 500;

    public record DatosRecurso(String nombre, String nombreNormalizado, TipoRecurso tipo,
                               Integer cantidad, String descripcion) {
    }

    public DatosRecurso validar(RecursoCreateRequest request) {
        if (request == null) {
            throw new ReglaNegocioException(MSG_NOMBRE_OBLIGATORIO);
        }
        String nombre = validarNombre(request.getNombre());
        return new DatosRecurso(
                nombre,
                TextoNormalizador.normalizar(nombre),
                validarTipo(request.getTipo()),
                validarCantidad(request.getCantidad()),
                validarDescripcion(request.getDescripcion()));
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException(MSG_NOMBRE_OBLIGATORIO);
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() > NOMBRE_MAX) {
            throw new ReglaNegocioException(MSG_NOMBRE_LARGO);
        }
        return limpio;
    }

    private TipoRecurso validarTipo(String tipo) {
        return TipoRecurso.desdeNombre(TextoNormalizador.normalizar(tipo))
                .orElseThrow(() -> new ReglaNegocioException(MSG_TIPO_INVALIDO));
    }

    /**
     * Acepta un entero JSON (3) o un texto con solo dígitos ("3").
     * Rechaza decimales (2.5), texto no numérico ("abc"), vacío o nulo, y valores fuera de 1..999.
     */
    private Integer validarCantidad(Object cantidad) {
        BigInteger valor = switch (cantidad) {
            case Integer i -> BigInteger.valueOf(i);
            case Long l -> BigInteger.valueOf(l);
            case Short s -> BigInteger.valueOf(s);
            case BigInteger b -> b;
            case String s when s.trim().matches("[+-]?\\d+") -> new BigInteger(s.trim());
            case null, default -> throw new ReglaNegocioException(MSG_CANTIDAD_INVALIDA);
        };
        if (valor.compareTo(CANTIDAD_MINIMA) < 0 || valor.compareTo(CANTIDAD_MAXIMA) > 0) {
            throw new ReglaNegocioException(MSG_CANTIDAD_INVALIDA);
        }
        return valor.intValue();
    }

    private String validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        String limpia = descripcion.trim();
        if (limpia.length() > DESCRIPCION_MAX) {
            throw new ReglaNegocioException(MSG_DESCRIPCION_LARGA);
        }
        return limpia;
    }
}
