package com.alexgit95.ludalex.affichage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.LignePartie;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieRepository;
import com.alexgit95.ludalex.parties.StatutPartie;
import com.alexgit95.ludalex.statistiques.StatistiqueRepository;

@Service
@Transactional(readOnly = true)
public class HistoriqueAffichage {

    public record Rang(int position, long victoires, List<Joueur> joueurs, long autres) {
    }

    public record Donnees(List<LignePartie> jeu, List<LignePartie> global, List<Rang> podium) {
    }

    private final PartieRepository parties;
    private final StatistiqueRepository statistiques;

    public HistoriqueAffichage(PartieRepository parties, StatistiqueRepository statistiques) {
        this.parties = parties;
        this.statistiques = statistiques;
    }

    public Donnees pourJeu(Long jeuId) {
        return charger(jeuId);
    }

    public Donnees global() {
        return charger(null);
    }

    private Donnees charger(Long jeuId) {
        var limite = PageRequest.of(0, 5);
        List<Partie> jeu = jeuId == null ? List.of()
                : parties.findByJeuIdAndStatutOrderByDatePartieDescIdDesc(jeuId, StatutPartie.TERMINEE, limite);
        List<Partie> global = parties.findByStatutOrderByDatePartieDescIdDesc(StatutPartie.TERMINEE, limite);
        List<Long> ids = Stream.concat(jeu.stream(), global.stream()).map(Partie::getId).distinct().toList();
        Map<Long, List<Participation>> participations = ids.isEmpty() ? Map.of()
                : parties.participationsDesParties(ids).stream()
                        .collect(Collectors.groupingBy(participation -> participation.getPartie().getId()));
        return new Donnees(lignes(jeu, participations), lignes(global, participations),
            jeuId == null ? List.of() : podium(jeuId));
    }

    private static List<LignePartie> lignes(List<Partie> parties, Map<Long, List<Participation>> participations) {
        return parties.stream().map(partie -> new LignePartie(partie,
                participations.getOrDefault(partie.getId(), List.of()))).toList();
    }

    private List<Rang> podium(Long jeuId) {
        List<Long> scores = statistiques.scoresPodium(jeuId, PageRequest.of(0, 3));
        if (scores.isEmpty()) {
            return List.of();
        }
        List<Rang> rangs = new ArrayList<>();
        for (int index = 0; index < scores.size(); index++) {
            long score = scores.get(index);
            List<Joueur> exAequo = statistiques.joueursPodium(jeuId, score, PageRequest.of(0, 3));
            long autres = Math.max(0, statistiques.nombreJoueursPodium(jeuId, score) - exAequo.size());
            rangs.add(new Rang(index + 1, score, exAequo, autres));
        }
        return List.copyOf(rangs);
    }
}