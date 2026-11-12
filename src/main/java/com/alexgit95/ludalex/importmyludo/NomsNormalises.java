package com.alexgit95.ludalex.importmyludo;

import java.text.Normalizer;
import java.util.Locale;

/** Clé de comparaison de noms de jeux et de joueurs : casse, accents, espaces et apostrophes ignorés. */
final class NomsNormalises {

    private NomsNormalises() {
    }

    static String cle(String nom) {
        if (nom == null) {
            return "";
        }
        String sansAccents = Normalizer.normalize(nom, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sansAccents.replace('\u2019', '\'').replace('\u2018', '\'').replace('`', '\'')
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
