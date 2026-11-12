package com.alexgit95.ludalex.joueurs;

import org.springframework.stereotype.Component;

/** Provisoire : à retirer quand le change « parties » fournira l'implémentation réelle. */
@Component
class AucuneParticipation implements ParticipationsJoueur {

    @Override
    public boolean aDesParticipations(Long joueurId) {
        return false;
    }
}
