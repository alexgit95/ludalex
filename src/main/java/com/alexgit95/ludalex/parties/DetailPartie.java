package com.alexgit95.ludalex.parties;

import java.util.List;

/** Partie complète pour le formulaire de modification. */
public record DetailPartie(Partie partie, List<Participation> participations, List<Equipe> equipes) {
}
