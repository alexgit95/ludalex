package com.alexgit95.ludalex.statistiques;

import com.alexgit95.ludalex.joueurs.Joueur;

/** Ligne du classement d'un jeu ; le taux de victoire est arrondi à l'entier le plus proche. */
public record LigneClassement(Joueur joueur, long parties, long victoires) {

    public int tauxPourcent() {
        return parties == 0 ? 0 : (int) Math.round(100.0 * victoires / parties);
    }
}
