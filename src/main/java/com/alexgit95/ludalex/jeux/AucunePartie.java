package com.alexgit95.ludalex.jeux;

import org.springframework.stereotype.Component;

/** Provisoire : à retirer quand le change « parties » fournira l'implémentation réelle. */
@Component
class AucunePartie implements PartiesJeu {

    @Override
    public boolean aDesParties(Long jeuId) {
        return false;
    }
}
