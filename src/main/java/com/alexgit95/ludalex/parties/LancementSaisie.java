package com.alexgit95.ludalex.parties;

import java.util.List;
import java.util.Map;

/** Lancement d'une partie en direct ; les équipes sont décrites comme dans {@link PartieSaisie}. */
public record LancementSaisie(Long jeuId, List<Long> joueurIds, Map<Long, Integer> equipeDe,
                Map<Integer, String> equipeNoms, String variante) {

        public LancementSaisie(Long jeuId, List<Long> joueurIds, Map<Long, Integer> equipeDe,
                        Map<Integer, String> equipeNoms) {
                this(jeuId, joueurIds, equipeDe, equipeNoms, null);
        }
}
