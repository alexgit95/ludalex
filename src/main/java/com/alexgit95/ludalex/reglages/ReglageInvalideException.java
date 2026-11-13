package com.alexgit95.ludalex.reglages;

import java.util.Map;

/** Valeurs refusées, indexées par la clé du réglage concerné. */
public class ReglageInvalideException extends RuntimeException {

    private final transient Map<ReglageCle, String> erreurs;

    public ReglageInvalideException(Map<ReglageCle, String> erreurs) {
        super("Réglages invalides");
        this.erreurs = Map.copyOf(erreurs);
    }

    public Map<ReglageCle, String> erreurs() {
        return erreurs;
    }
}
