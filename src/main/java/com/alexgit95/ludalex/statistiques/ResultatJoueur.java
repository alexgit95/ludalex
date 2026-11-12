package com.alexgit95.ludalex.statistiques;

/** Parties jouées et victoires d'un joueur pour un jeu (parties terminées uniquement). */
public record ResultatJoueur(Long joueurId, Long parties, Long victoires) {
}
