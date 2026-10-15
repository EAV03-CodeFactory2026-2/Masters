package com.udea.masters.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextoNormalizador {

    private TextoNormalizador() {
    }

    /**
     * Normaliza un texto para comparaciones: quita tildes y diacríticos, pasa a minúsculas,
     * recorta y colapsa espacios. Ej.: "  Cámilla  DE masajes " -> "camilla de masajes".
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String sinDiacriticos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinDiacriticos.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
