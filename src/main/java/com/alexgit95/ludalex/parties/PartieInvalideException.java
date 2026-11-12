package com.alexgit95.ludalex.parties;

/** Saisie invalide ; {@code champ} désigne la zone du formulaire (date, lieu, jeu, joueurs, gagnants, ...). */
public class PartieInvalideException extends RuntimeException {

    private final String champ;

    public PartieInvalideException(String champ, String message) {
        super(message);
        this.champ = champ;
    }

    public String getChamp() {
        return champ;
    }
}
