package com.alexgit95.ludalex.importmyludo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.alexgit95.ludalex.parties.ResultatCooperatif;

/** Ce que l'import ferait : calculé sans rien écrire, puis recalculé à l'import sur l'état réel de la base. */
public record PlanImport(
        List<JeuPlan> jeux,
        List<JoueurPlan> joueurs,
        List<PartiePlan> parties,
        int dejaPresentes,
        int totalIgnorees,
        Map<String, Integer> ignoreesParRaison,
        List<String> detailsIgnorees,
        List<String> avertissements,
        int totalAvertissements) {

    public enum Action {
        CREER, COMPLETER, INCHANGE
    }

    public record JeuPlan(String cle, String nom, Action action, Long existantId, BigDecimal prix,
            LocalDate dateAchat, boolean cooperatif) {
    }

    /** {@code propose} : fiche existante de même nom ; {@code effectif} : choix retenu (vide = création). */
    public record JoueurPlan(int index, String cle, String nom, Long propose, Long effectif, int participations) {
    }

    public record ParticipantPlan(String joueurCle, Integer score, boolean gagnant, String equipe) {
    }

    public record PartiePlan(String jeuCle, LocalDate date, String lieu, ResultatCooperatif resultat,
            List<ParticipantPlan> participants) {
    }

    public long jeuxA(Action action) {
        return jeux.stream().filter(jeu -> jeu.action() == action).count();
    }

    public long jeuxACreer() {
        return jeuxA(Action.CREER);
    }

    public long jeuxACompleter() {
        return jeuxA(Action.COMPLETER);
    }

    public long jeuxInchanges() {
        return jeuxA(Action.INCHANGE);
    }

    public long joueursACreer() {
        return joueurs.stream().filter(joueur -> joueur.effectif() == null).count();
    }
}
