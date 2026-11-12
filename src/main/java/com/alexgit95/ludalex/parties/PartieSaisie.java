package com.alexgit95.ludalex.parties;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Saisie d'une partie terminée. Les équipes sont décrites par un numéro (1 à {@link PartieService#EQUIPES_MAX}) :
 * {@code equipeDe} associe un joueur à son équipe, {@code equipeNoms} nomme les équipes.
 */
public record PartieSaisie(
        Long jeuId,
        LocalDate date,
        String lieu,
        List<Long> joueurIds,
        Map<Long, Integer> scores,
        List<Long> gagnantIds,
        Map<Long, Integer> equipeDe,
        Map<Integer, String> equipeNoms,
        List<Integer> equipesGagnantes,
        ResultatCooperatif resultat) {
}
