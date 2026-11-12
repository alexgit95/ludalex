package com.alexgit95.ludalex.joueurs;

/** Vérifie si un joueur apparaît dans des parties (anonymisation plutôt que suppression). */
public interface ParticipationsJoueur {

    boolean aDesParticipations(Long joueurId);
}
