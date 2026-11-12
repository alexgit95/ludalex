package com.alexgit95.ludalex.jeux;

/** Vérifie si un jeu a des parties (la suppression d'un tel jeu est refusée). */
public interface PartiesJeu {

    boolean aDesParties(Long jeuId);
}
