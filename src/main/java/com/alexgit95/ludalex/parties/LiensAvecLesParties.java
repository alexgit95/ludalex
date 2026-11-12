package com.alexgit95.ludalex.parties;

import org.springframework.stereotype.Component;

import com.alexgit95.ludalex.jeux.PartiesJeu;
import com.alexgit95.ludalex.joueurs.ParticipationsJoueur;

/** Branche les contrôles des modules « joueurs » et « jeux » sur les parties réelles. */
@Component
class LiensAvecLesParties implements ParticipationsJoueur, PartiesJeu {

    private final ParticipationRepository participations;
    private final PartieRepository parties;

    LiensAvecLesParties(ParticipationRepository participations, PartieRepository parties) {
        this.participations = participations;
        this.parties = parties;
    }

    @Override
    public boolean aDesParticipations(Long joueurId) {
        return participations.existsByJoueurId(joueurId);
    }

    @Override
    public boolean aDesParties(Long jeuId) {
        return parties.existsByJeuId(jeuId);
    }
}
