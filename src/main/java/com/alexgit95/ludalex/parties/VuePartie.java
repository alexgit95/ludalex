package com.alexgit95.ludalex.parties;

import java.util.List;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;

/** Partie en direct prête à afficher : participants, équipes et joueur suivant déjà chargés. */
public record VuePartie(Partie partie, Jeu jeu, List<Participation> participations, List<Equipe> equipes,
        Joueur joueurSuivant) {

    public boolean cooperatif() {
        return jeu.isCooperatif();
    }
}
