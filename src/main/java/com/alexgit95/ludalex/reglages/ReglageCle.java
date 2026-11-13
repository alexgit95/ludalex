package com.alexgit95.ludalex.reglages;

import java.util.Optional;

/** Catalogue des réglages connus : clé stockée, libellé, valeur par défaut et bornes (entiers positifs). */
public enum ReglageCle {

    JEU_DORMANT_MOIS("statistiques.jeu-dormant-mois", "Jeu dormant : mois sans partie", 6, 1, 120),
    CLASSEMENT_PARTIES_MIN("statistiques.classement-parties-min", "Classement : nombre minimal de parties", 5, 1, 1000),
    PRET_LONG_JOURS("statistiques.pret-long-jours", "Prêt long : nombre de jours", 30, 1, 3650);

    private final String cle;
    private final String libelle;
    private final int defaut;
    private final int min;
    private final int max;

    ReglageCle(String cle, String libelle, int defaut, int min, int max) {
        this.cle = cle;
        this.libelle = libelle;
        this.defaut = defaut;
        this.min = min;
        this.max = max;
    }

    public String cle() {
        return cle;
    }

    public String libelle() {
        return libelle;
    }

    public int defaut() {
        return defaut;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public static Optional<ReglageCle> depuisCle(String cle) {
        for (ReglageCle candidat : values()) {
            if (candidat.cle.equals(cle)) {
                return Optional.of(candidat);
            }
        }
        return Optional.empty();
    }

    /** Valeur entière dans les bornes, ou vide si le texte est absent, non numérique ou hors bornes. */
    public Optional<Integer> analyser(String valeur) {
        if (valeur == null || !valeur.strip().matches("[0-9]{1,9}")) {
            return Optional.empty();
        }
        int nombre = Integer.parseInt(valeur.strip());
        return nombre < min || nombre > max ? Optional.empty() : Optional.of(nombre);
    }
}
