package com.alexgit95.ludalex.jeux;

/** Saisie invalide ; {@code champ} désigne la zone du formulaire concernée. */
public class JeuInvalideException extends RuntimeException {

    private final String champ;

    public JeuInvalideException(String champ, String message) {
        super(message);
        this.champ = champ;
    }

    public String getChamp() {
        return champ;
    }
}
