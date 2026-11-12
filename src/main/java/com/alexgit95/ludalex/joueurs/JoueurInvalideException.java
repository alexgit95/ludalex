package com.alexgit95.ludalex.joueurs;

/** Saisie invalide ; {@code champ} désigne la zone du formulaire concernée (nom, avatar, victoire). */
public class JoueurInvalideException extends RuntimeException {

    private final String champ;

    public JoueurInvalideException(String champ, String message) {
        super(message);
        this.champ = champ;
    }

    public String getChamp() {
        return champ;
    }
}
