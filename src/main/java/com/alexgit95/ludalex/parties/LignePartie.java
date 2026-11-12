package com.alexgit95.ludalex.parties;

import java.util.List;

/** Une ligne de la liste : la partie et ses participations (joueur et équipe déjà chargés). */
public record LignePartie(Partie partie, List<Participation> participations) {
}
