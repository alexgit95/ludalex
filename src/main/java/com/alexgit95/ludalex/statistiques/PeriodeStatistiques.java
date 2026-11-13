package com.alexgit95.ludalex.statistiques;

import java.time.LocalDate;

/** Période d'analyse du tableau de bord ; 12 mois par défaut. */
public enum PeriodeStatistiques {

    JOURS_30("30j", "30 derniers jours"),
    MOIS_12("12m", "12 derniers mois"),
    ANNEE("annee", "Année civile"),
    TOUT("tout", "Tout l'historique");

    /**
     * Fenêtre de dates incluses ; {@code debut} est vide pour tout l'historique.
     * {@code precedente} est la période de comparaison, vide si elle n'a pas de sens.
     */
    public record Fenetre(LocalDate debut, LocalDate fin, Fenetre precedente) {
    }

    private final String code;
    private final String libelle;

    PeriodeStatistiques(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    public String code() {
        return code;
    }

    public String libelle() {
        return libelle;
    }

    /** Période désignée par le paramètre, ou 12 mois si le code est absent ou inconnu. */
    public static PeriodeStatistiques depuisCode(String code) {
        for (PeriodeStatistiques periode : values()) {
            if (periode.code.equals(code)) {
                return periode;
            }
        }
        return MOIS_12;
    }

    public Fenetre fenetre(LocalDate aujourdhui) {
        return switch (this) {
            case JOURS_30 -> new Fenetre(aujourdhui.minusDays(29), aujourdhui,
                    new Fenetre(aujourdhui.minusDays(59), aujourdhui.minusDays(30), null));
            case MOIS_12 -> {
                LocalDate debut = aujourdhui.minusMonths(12).plusDays(1);
                yield new Fenetre(debut, aujourdhui, new Fenetre(debut.minusMonths(12), debut.minusDays(1), null));
            }
            case ANNEE -> {
                LocalDate debut = LocalDate.of(aujourdhui.getYear(), 1, 1);
                yield new Fenetre(debut, aujourdhui,
                        new Fenetre(debut.minusYears(1), aujourdhui.minusYears(1), null));
            }
            case TOUT -> new Fenetre(null, aujourdhui, null);
        };
    }
}
