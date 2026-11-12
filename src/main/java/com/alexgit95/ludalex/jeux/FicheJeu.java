package com.alexgit95.ludalex.jeux;

import java.util.List;

/** Fiche détail : prêt en cours (nul si le jeu n'est pas prêté) et historique complet. */
public record FicheJeu(Jeu jeu, Pret enCours, List<Pret> historique) {
}
