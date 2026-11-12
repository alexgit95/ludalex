package com.alexgit95.ludalex.parties;

import java.util.List;
import java.util.Map;

/** Fin d'une partie en direct : gagnants (joueurs ou équipes par identifiant), scores facultatifs, résultat coopératif. */
public record FinSaisie(List<Long> gagnantIds, List<Long> equipesGagnantes, Map<Long, Integer> scores,
        ResultatCooperatif resultat) {
}
