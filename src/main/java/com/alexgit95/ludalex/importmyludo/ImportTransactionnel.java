package com.alexgit95.ludalex.importmyludo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.JeuMyLudo;
import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.PartieMyLudo;
import com.alexgit95.ludalex.importmyludo.PlanImport.Action;
import com.alexgit95.ludalex.importmyludo.PlanImport.JeuPlan;
import com.alexgit95.ludalex.importmyludo.PlanImport.JoueurPlan;
import com.alexgit95.ludalex.importmyludo.PlanImport.ParticipantPlan;
import com.alexgit95.ludalex.importmyludo.PlanImport.PartiePlan;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Equipe;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;

import jakarta.persistence.EntityManager;

/**
 * Écrit l'import en une seule transaction : le plan est recalculé sur l'état réel de la base juste avant l'écriture,
 * puis jeux, joueurs, parties, équipes et participations sont ajoutés par lots. Rien n'existant n'est modifié,
 * hormis le prix et la date d'achat vides d'un jeu.
 */
@Component
public class ImportTransactionnel {

    private static final int LOT = 500;

    private final EntityManager em;
    private final AnalyseurImport analyseur;

    public ImportTransactionnel(EntityManager em, AnalyseurImport analyseur) {
        this.em = em;
        this.analyseur = analyseur;
    }

    @Transactional
    public RapportImport executer(List<JeuMyLudo> collection, List<PartieMyLudo> parties,
            Map<String, Long> associations, Progression progression) {
        PlanImport plan = analyseur.analyser(collection, parties, associations, null);
        if (progression != null) {
            progression.demarrer(Progression.Phase.IMPORT, plan.jeux().size() + plan.parties().size());
        }
        Map<String, Long> jeuIds = ecrireJeux(plan, progression);
        Map<String, Long> joueurIds = new HashMap<>();
        long[] compteursJoueurs = ecrireJoueurs(plan, joueurIds);
        ecrireParties(plan, jeuIds, joueurIds, progression);
        em.flush();
        em.clear();
        return new RapportImport(plan.jeuxA(Action.CREER), plan.jeuxA(Action.COMPLETER), plan.jeuxA(Action.INCHANGE),
                compteursJoueurs[0], compteursJoueurs[1], plan.parties().size(), plan.dejaPresentes(),
                plan.totalIgnorees(), plan.ignoreesParRaison(), plan.detailsIgnorees(), plan.avertissements(),
                plan.totalAvertissements());
    }

    private Map<String, Long> ecrireJeux(PlanImport plan, Progression progression) {
        Map<String, Long> ids = new HashMap<>();
        for (JeuPlan jeu : plan.jeux()) {
            if (jeu.action() == Action.CREER) {
                Jeu cree = new Jeu(jeu.nom());
                cree.setPrix(jeu.prix());
                cree.setDateAchat(jeu.dateAchat());
                cree.setCooperatif(jeu.cooperatif());
                em.persist(cree);
                ids.put(jeu.cle(), cree.getId());
            } else {
                if (jeu.action() == Action.COMPLETER) {
                    Jeu existant = em.find(Jeu.class, jeu.existantId());
                    if (existant.getPrix() == null && jeu.prix() != null) {
                        existant.setPrix(jeu.prix());
                    }
                    if (existant.getDateAchat() == null && jeu.dateAchat() != null) {
                        existant.setDateAchat(jeu.dateAchat());
                    }
                }
                ids.put(jeu.cle(), jeu.existantId());
            }
            if (progression != null) {
                progression.avancer(jeu.nom());
            }
        }
        return ids;
    }

    /** @return {joueurs créés, joueurs associés à une fiche existante} parmi ceux utilisés par les parties ajoutées */
    private long[] ecrireJoueurs(PlanImport plan, Map<String, Long> ids) {
        Set<String> utilises = new HashSet<>();
        for (PartiePlan partie : plan.parties()) {
            for (ParticipantPlan participant : partie.participants()) {
                utilises.add(participant.joueurCle());
            }
        }
        long crees = 0;
        long associes = 0;
        for (JoueurPlan joueur : plan.joueurs()) {
            if (!utilises.contains(joueur.cle())) {
                continue;
            }
            if (joueur.effectif() != null) {
                ids.put(joueur.cle(), joueur.effectif());
                associes++;
                continue;
            }
            Long homonymes = em.createQuery("select count(j) from Joueur j where j.nomNormalise = :nom "
                    + "and j.anonymise = false", Long.class)
                    .setParameter("nom", joueur.nom().toLowerCase(Locale.ROOT)).getSingleResult();
            if (homonymes > 0) {
                throw ImportMyLudoException.simple("Un joueur nommé « " + joueur.nom() + " » existe déjà : associez "
                        + "ce nom à la fiche existante au lieu d'en créer une nouvelle");
            }
            Joueur cree = new Joueur(joueur.nom());
            em.persist(cree);
            ids.put(joueur.cle(), cree.getId());
            crees++;
        }
        return new long[] { crees, associes };
    }

    private void ecrireParties(PlanImport plan, Map<String, Long> jeuIds, Map<String, Long> joueurIds,
            Progression progression) {
        int ecrites = 0;
        for (PartiePlan plans : plan.parties()) {
            Partie partie = new Partie(em.getReference(Jeu.class, jeuIds.get(plans.jeuCle())), plans.date());
            partie.setLieu(plans.lieu());
            partie.setResultatCooperatif(plans.resultat());
            em.persist(partie);
            Map<String, Equipe> equipes = new LinkedHashMap<>();
            for (ParticipantPlan participant : plans.participants()) {
                if (participant.equipe() != null && !equipes.containsKey(participant.equipe())) {
                    Equipe equipe = new Equipe(partie, participant.equipe());
                    em.persist(equipe);
                    equipes.put(participant.equipe(), equipe);
                }
            }
            for (ParticipantPlan participant : plans.participants()) {
                Participation participation = new Participation(partie,
                        em.getReference(Joueur.class, joueurIds.get(participant.joueurCle())));
                participation.setScore(participant.score());
                participation.setGagnant(participant.gagnant());
                if (participant.equipe() != null) {
                    participation.setEquipe(equipes.get(participant.equipe()));
                }
                em.persist(participation);
            }
            if (progression != null) {
                progression.avancer(plans.jeuCle() + ", " + plans.date());
            }
            if (++ecrites % LOT == 0) {
                em.flush();
                em.clear();
            }
        }
    }
}
