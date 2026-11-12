package com.alexgit95.ludalex.jeux;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.alexgit95.ludalex.joueurs.Joueur;

/** Champions d'une page de jeux, obtenus en un nombre constant de requêtes (fourni par « statistiques »). */
public interface ChampionsDesJeux {

    /** Jeu vers ses champions ; les jeux sans champion sont absents. */
    Map<Long, List<Joueur>> pour(Collection<Long> jeuIds);
}
