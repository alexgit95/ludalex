package com.alexgit95.ludalex.importmyludo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Modèle intermédiaire des exports MyLudo : seuls les champs utiles à l'import sont conservés. */
final class ModeleMyLudo {

    private ModeleMyLudo() {
    }

    /** {@code prix} vide si inconnu ou nul ; {@code mecaniqueCooperative} : mécanisme exact « Coopération ». */
    record JeuMyLudo(int entree, String titre, BigDecimal prix, LocalDate dateAcquisition,
            boolean mecaniqueCooperative) {
    }

    record JoueurMyLudo(String nom, Integer score, boolean victoire, String equipe) {
    }

    record PartieMyLudo(int entree, String titre, LocalDate date, String lieu, List<JoueurMyLudo> joueurs,
            boolean coop, boolean coopVictoire, boolean exclue) {

        String contexte() {
            return titre + ", " + date;
        }
    }
}
