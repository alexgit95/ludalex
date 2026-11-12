package com.alexgit95.ludalex.jeux;

/** Vignette de la liste : le jeu et son emprunteur courant (vide si non prêté). */
public record VignetteJeu(Jeu jeu, String emprunteur) {
}
