package com.alexgit95.ludalex.statistiques;

import java.util.List;

import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.LignePartie;

/** Statistiques d'un jeu : classement, champion(s) et dix dernières parties terminées. */
public record StatistiquesJeu(List<LigneClassement> classement, List<Joueur> champions,
        List<LignePartie> dernieresParties) {

    public boolean vide() {
        return classement.isEmpty();
    }
}
