package com.alexgit95.ludalex.statistiques;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.jeux.ChampionsDesJeux;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurRepository;
import com.alexgit95.ludalex.parties.LignePartie;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieRepository;
import com.alexgit95.ludalex.parties.StatutPartie;

/**
 * Statistiques par jeu, calculées par agrégation en base : jeux coopératifs et compétitifs regroupés,
 * aucun minimum de parties, parties terminées uniquement.
 */
@Service
@Transactional(readOnly = true)
public class StatistiquesService implements ChampionsDesJeux {

    public static final int DERNIERES_PARTIES = 10;

    private static final Comparator<LigneClassement> ORDRE = Comparator
            .comparingLong(LigneClassement::victoires).reversed()
            .thenComparing(Comparator.comparingInt(LigneClassement::tauxPourcent).reversed())
            .thenComparing(ligne -> ligne.joueur().getNom().toLowerCase(Locale.ROOT))
            .thenComparing(ligne -> ligne.joueur().getId());

    private final StatistiqueRepository statistiques;
    private final JoueurRepository joueurs;
    private final PartieRepository parties;

    public StatistiquesService(StatistiqueRepository statistiques, JoueurRepository joueurs, PartieRepository parties) {
        this.statistiques = statistiques;
        this.joueurs = joueurs;
        this.parties = parties;
    }

    public StatistiquesJeu pourJeu(Long jeuId) {
        List<ResultatJoueur> resultats = statistiques.classement(jeuId);
        Map<Long, Joueur> parId = chargerJoueurs(resultats.stream().map(ResultatJoueur::joueurId).toList());
        List<LigneClassement> classement = resultats.stream()
                .filter(resultat -> parId.containsKey(resultat.joueurId()))
                .map(resultat -> new LigneClassement(parId.get(resultat.joueurId()), resultat.parties(),
                        resultat.victoires()))
                .sorted(ORDRE)
                .toList();
        List<Joueur> champions = champions(classement.stream()
                .filter(ligne -> !ligne.joueur().isAnonymise()).toList());
        return new StatistiquesJeu(classement, champions, dernieresParties(jeuId));
    }

    @Override
    public Map<Long, List<Joueur>> pour(Collection<Long> jeuIds) {
        if (jeuIds == null || jeuIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<VictoiresJeuJoueur>> parJeu = statistiques.victoires(jeuIds).stream()
                .collect(Collectors.groupingBy(VictoiresJeuJoueur::jeuId));
        Map<Long, Set<Long>> championsParJeu = new LinkedHashMap<>();
        Set<Long> tous = new HashSet<>();
        parJeu.forEach((jeuId, lignes) -> {
            long meilleur = lignes.stream().mapToLong(VictoiresJeuJoueur::victoires).max().orElse(0);
            if (meilleur > 0) {
                Set<Long> ids = lignes.stream().filter(ligne -> ligne.victoires() == meilleur)
                        .map(VictoiresJeuJoueur::joueurId).collect(Collectors.toSet());
                championsParJeu.put(jeuId, ids);
                tous.addAll(ids);
            }
        });
        Map<Long, Joueur> parId = chargerJoueurs(tous);
        Map<Long, List<Joueur>> resultat = new LinkedHashMap<>();
        championsParJeu.forEach((jeuId, ids) -> resultat.put(jeuId, ids.stream().map(parId::get)
                .filter(joueur -> joueur != null)
                .sorted(Comparator.comparing((Joueur joueur) -> joueur.getNom().toLowerCase(Locale.ROOT)))
                .toList()));
        return resultat;
    }

    /** Joueurs ayant le plus de victoires (au moins une) ; tous les ex æquo sont champions. */
    static List<Joueur> champions(List<LigneClassement> eligibles) {
        long meilleur = eligibles.stream().mapToLong(LigneClassement::victoires).max().orElse(0);
        if (meilleur <= 0) {
            return List.of();
        }
        return eligibles.stream().filter(ligne -> ligne.victoires() == meilleur).map(LigneClassement::joueur).toList();
    }

    private List<LignePartie> dernieresParties(Long jeuId) {
        List<Partie> recentes = parties.findByJeuIdAndStatutOrderByDatePartieDescIdDesc(jeuId, StatutPartie.TERMINEE,
                PageRequest.of(0, DERNIERES_PARTIES));
        if (recentes.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Participation>> participations = parties
                .participationsDesParties(recentes.stream().map(Partie::getId).toList()).stream()
                .collect(Collectors.groupingBy(participation -> participation.getPartie().getId()));
        return recentes.stream()
                .map(partie -> new LignePartie(partie, participations.getOrDefault(partie.getId(), List.of())))
                .toList();
    }

    private Map<Long, Joueur> chargerJoueurs(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return joueurs.findAllById(ids).stream().collect(Collectors.toMap(Joueur::getId, Function.identity()));
    }
}
