package com.alexgit95.ludalex.affichage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Equipe;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.VuePartie;

/**
 * Façon de présenter les joueurs d'une partie en cours : groupes à afficher côte à côte, séparés ou non par un « VS ».
 * Un groupe est une équipe (nom renseigné) ou un joueur seul / l'ensemble des joueurs (nom nul).
 */
public record PresentationJoueurs(Mode mode, List<Groupe> groupes) {

    public enum Mode {
        COOPERATIF, SOLO, EQUIPES, DUEL, MULTI
    }

    public record Groupe(String nom, List<Joueur> joueurs) {
    }

    /** Vrai si un cartouche « VS » sépare les groupes. */
    public boolean versus() {
        return mode == Mode.EQUIPES || mode == Mode.DUEL || mode == Mode.MULTI;
    }

    /** Vrai pour un jeu coopératif : un libellé « Ensemble » remplace le « VS ». */
    public boolean ensemble() {
        return mode == Mode.COOPERATIF;
    }

    public static PresentationJoueurs de(VuePartie vue) {
        List<Participation> participations = vue.participations();
        if (vue.cooperatif()) {
            return new PresentationJoueurs(Mode.COOPERATIF, List.of(tous(participations)));
        }
        if (participations.size() <= 1) {
            return new PresentationJoueurs(Mode.SOLO, List.of(tous(participations)));
        }
        if (participations.stream().anyMatch(p -> p.getEquipe() != null)) {
            return new PresentationJoueurs(Mode.EQUIPES, parEquipe(vue.equipes(), participations));
        }
        List<Groupe> seuls = participations.stream()
                .map(p -> new Groupe(null, List.of(p.getJoueur()))).toList();
        return new PresentationJoueurs(participations.size() == 2 ? Mode.DUEL : Mode.MULTI, seuls);
    }

    private static Groupe tous(List<Participation> participations) {
        return new Groupe(null, participations.stream().map(Participation::getJoueur).toList());
    }

    /** Équipes dans l'ordre de la partie, puis les joueurs sans équipe ; les groupes vides sont omis. */
    private static List<Groupe> parEquipe(List<Equipe> equipes, List<Participation> participations) {
        Map<Long, List<Joueur>> membres = new LinkedHashMap<>();
        for (Equipe equipe : equipes) {
            membres.put(equipe.getId(), new ArrayList<>());
        }
        List<Joueur> sansEquipe = new ArrayList<>();
        for (Participation participation : participations) {
            Equipe equipe = participation.getEquipe();
            List<Joueur> destination = equipe == null ? null : membres.get(equipe.getId());
            (destination == null ? sansEquipe : destination).add(participation.getJoueur());
        }
        List<Groupe> groupes = new ArrayList<>();
        for (Equipe equipe : equipes) {
            List<Joueur> joueurs = membres.get(equipe.getId());
            if (!joueurs.isEmpty()) {
                groupes.add(new Groupe(equipe.getNom(), joueurs));
            }
        }
        if (!sansEquipe.isEmpty()) {
            groupes.add(new Groupe(null, sansEquipe));
        }
        return groupes;
    }
}
