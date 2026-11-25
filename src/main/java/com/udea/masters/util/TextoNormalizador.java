package com.udea.masters.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

public final class TextoNormalizador {

    /**
     * Normaliza un texto para comparaciones: quita tildes y diacríticos, pasa a minúsculas,
     * recorta y colapsa espacios. Ej.: "  Cámilla  DE masajes " -> "camilla de masajes".
     */
    public static String normalizar(String texto) {
        return Optional.ofNullable(texto)
                .map(valor -> Normalizer.normalize(valor, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .trim()
                        .replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT))
                .orElse(null);
    }
}
