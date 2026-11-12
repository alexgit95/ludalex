package com.alexgit95.ludalex.importmyludo;

import java.util.ArrayList;
import java.util.List;

/**
 * Erreur de lecture ou d'import dont le message permet le diagnostic : fichier, entrée, jeu et date, champ, cause,
 * ligne et colonne pour un JSON mal formé. Aucun détail interne.
 */
public class ImportMyLudoException extends RuntimeException {

    private final String fichier;
    private final Integer entree;
    private final String contexte;
    private final String champ;
    private final String cause;
    private final Integer ligne;
    private final Integer colonne;

    public ImportMyLudoException(String fichier, Integer entree, String contexte, String champ, String cause) {
        this(fichier, entree, contexte, champ, cause, null, null);
    }

    public ImportMyLudoException(String fichier, Integer entree, String contexte, String champ, String cause,
            Integer ligne, Integer colonne) {
        super(message(fichier, entree, contexte, champ, cause, ligne, colonne));
        this.fichier = fichier;
        this.entree = entree;
        this.contexte = contexte;
        this.champ = champ;
        this.cause = cause;
        this.ligne = ligne;
        this.colonne = colonne;
    }

    public static ImportMyLudoException simple(String message) {
        return new ImportMyLudoException(null, null, null, null, message);
    }

    /** Éléments du diagnostic, un par ligne d'affichage. */
    public List<String> details() {
        List<String> details = new ArrayList<>();
        if (fichier != null) {
            details.add("Fichier : " + fichier);
        }
        if (entree != null) {
            details.add("Entrée n°" + entree);
        }
        if (contexte != null) {
            details.add("Concerne : " + contexte);
        }
        if (champ != null) {
            details.add("Champ : " + champ);
        }
        if (ligne != null) {
            details.add("Position : ligne " + ligne + ", colonne " + colonne);
        }
        details.add("Cause : " + cause);
        return details;
    }

    private static String message(String fichier, Integer entree, String contexte, String champ, String cause,
            Integer ligne, Integer colonne) {
        StringBuilder texte = new StringBuilder();
        if (fichier != null) {
            texte.append(fichier);
        }
        if (entree != null) {
            texte.append(texte.isEmpty() ? "" : ", ").append("entrée n°").append(entree);
        }
        if (contexte != null) {
            texte.append(" (").append(contexte).append(')');
        }
        if (champ != null) {
            texte.append(texte.isEmpty() ? "" : ", ").append("champ « ").append(champ).append(" »");
        }
        if (ligne != null) {
            texte.append(texte.isEmpty() ? "" : ", ").append("ligne ").append(ligne).append(", colonne ")
                    .append(colonne);
        }
        texte.append(texte.isEmpty() ? "" : " : ").append(cause);
        return texte.toString();
    }
}
