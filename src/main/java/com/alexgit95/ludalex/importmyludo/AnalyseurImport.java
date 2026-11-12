package com.alexgit95.ludalex.importmyludo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.JeuMyLudo;
import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.JoueurMyLudo;
import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.PartieMyLudo;
import com.alexgit95.ludalex.importmyludo.PlanImport.Action;
import com.alexgit95.ludalex.importmyludo.PlanImport.JeuPlan;
import com.alexgit95.ludalex.importmyludo.PlanImport.JoueurPlan;
import com.alexgit95.ludalex.importmyludo.PlanImport.ParticipantPlan;
import com.alexgit95.ludalex.importmyludo.PlanImport.PartiePlan;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Equipe;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieService;
import com.alexgit95.ludalex.parties.ResultatCooperatif;

import jakarta.persistence.EntityManager;

/**
 * Calcule le plan d'import à partir des exports et de l'état réel de la base, sans rien écrire : rapprochement
 * des jeux et des joueurs, conversion des parties, parties ignorées avec leur raison et parties déjà présentes.
 */
@Component
public class AnalyseurImport {

    private record JeuExistant(Long id, BigDecimal prix, LocalDate dateAchat, boolean cooperatif) {
    }

    private static final class JeuSource {
        String nom;
        BigDecimal prix;
        LocalDate dateAchat;
        boolean mecaniqueCooperative;
        boolean dansCollection;
        boolean cooperatifParties;
        boolean utilise;
    }

    private static final class JoueurAcc {
        int index;
        String nom;
        int participations;
    }

    private record Candidate(String jeuCle, LocalDate date, PartiePlan plan, String contexte) {
    }

    private record Groupe(Long jeuId, LocalDate date) {
    }

    private static final class Collecteur {
        private final int max;
        private int total;
        private final List<String> lignes = new ArrayList<>();
        private final Map<String, Integer> parRaison = new LinkedHashMap<>();

        Collecteur(int max) {
            this.max = max;
        }

        void ajouter(String raison, String ligne) {
            total++;
            parRaison.merge(raison, 1, Integer::sum);
            if (lignes.size() < max) {
                lignes.add(ligne);
            }
        }
    }

    private final EntityManager em;
    private final Clock horloge;
    private final ImportMyLudoProperties proprietes;

    public AnalyseurImport(EntityManager em, Clock horloge, ImportMyLudoProperties proprietes) {
        this.em = em;
        this.horloge = horloge;
        this.proprietes = proprietes;
    }

    /**
     * @param associations nom normalisé d'un joueur vers la fiche choisie (valeur vide : créer) ; un nom absent
     *                     de la table suit la proposition par défaut (fiche existante de même nom)
     * @param progression  suivi facultatif
     */
    @Transactional(readOnly = true)
    public PlanImport analyser(List<JeuMyLudo> collection, List<PartieMyLudo> parties,
            Map<String, Long> associations, Progression progression) {
        LocalDate aujourdhui = LocalDate.now(horloge);
        Collecteur ignorees = new Collecteur(proprietes.lignesMax());
        Collecteur avertissements = new Collecteur(proprietes.lignesMax());
        if (progression != null) {
            progression.demarrer(Progression.Phase.ANALYSE, parties.size());
        }

        Map<String, JeuExistant> jeuxExistants = jeuxExistants();
        Map<String, Long> joueursExistants = joueursExistants();

        Map<String, JeuSource> jeux = new LinkedHashMap<>();
        for (JeuMyLudo jeu : collection) {
            JeuSource source = jeux.computeIfAbsent(NomsNormalises.cle(jeu.titre()), cle -> new JeuSource());
            if (source.nom == null) {
                source.nom = jeu.titre();
                source.prix = jeu.prix();
                source.dateAchat = jeu.dateAcquisition();
                source.mecaniqueCooperative = jeu.mecaniqueCooperative();
                source.dansCollection = true;
            }
        }

        // Étape A : parties exploitables indépendamment du rapprochement des joueurs
        List<PartieMyLudo> retenues = new ArrayList<>();
        for (PartieMyLudo partie : parties) {
            if (progression != null) {
                progression.avancer(partie.contexte());
            }
            JeuSource source = jeux.computeIfAbsent(NomsNormalises.cle(partie.titre()), cle -> new JeuSource());
            if (source.nom == null) {
                source.nom = partie.titre();
            }
            source.cooperatifParties |= partie.coop();
            String raison = raisonIgnoree(partie, aujourdhui);
            if (raison != null) {
                ignorees.ajouter(raison, "Entrée n°" + partie.entree() + " (" + partie.contexte() + ") : " + raison);
            } else {
                retenues.add(partie);
            }
        }

        // Joueurs de l'export, un doublon de nom dans une partie devient « Nom 2 »
        Map<String, JoueurAcc> joueurs = new LinkedHashMap<>();
        Map<PartieMyLudo, List<String>> clesParPartie = new HashMap<>();
        for (PartieMyLudo partie : retenues) {
            List<String> cles = new ArrayList<>();
            Map<String, Integer> vus = new HashMap<>();
            for (JoueurMyLudo joueur : partie.joueurs()) {
                int rang = vus.merge(NomsNormalises.cle(joueur.nom()), 1, Integer::sum);
                String nom = rang == 1 ? joueur.nom()
                        : tronquer(joueur.nom(), Joueur.NOM_MAX - 8) + " " + rang;
                String cle = NomsNormalises.cle(nom);
                JoueurAcc acc = joueurs.computeIfAbsent(cle, k -> {
                    JoueurAcc nouveau = new JoueurAcc();
                    nouveau.index = joueurs.size();
                    nouveau.nom = nom;
                    return nouveau;
                });
                acc.participations++;
                cles.add(cle);
            }
            clesParPartie.put(partie, cles);
        }
        Map<String, Long> effectifs = new HashMap<>();
        for (String cle : joueurs.keySet()) {
            effectifs.put(cle, associations != null && associations.containsKey(cle) ? associations.get(cle)
                    : joueursExistants.get(cle));
        }

        // Étape B : conversion des parties
        List<Candidate> candidates = new ArrayList<>();
        for (PartieMyLudo partie : retenues) {
            String jeuCle = NomsNormalises.cle(partie.titre());
            JeuSource source = jeux.get(jeuCle);
            JeuExistant existant = jeuxExistants.get(jeuCle);
            boolean cooperatif = existant != null ? existant.cooperatif()
                    : source.cooperatifParties || source.mecaniqueCooperative;
            List<String> cles = clesParPartie.get(partie);
            Candidate candidate = convertir(partie, jeuCle, cooperatif, cles, effectifs, ignorees, avertissements);
            if (candidate != null) {
                source.utilise = true;
                candidates.add(candidate);
            }
        }

        // Étape C : parties déjà présentes (même jeu, même jour, joueur en commun)
        List<PartiePlan> aAjouter = new ArrayList<>();
        int dejaPresentes = 0;
        Map<Groupe, List<Integer>> groupes = new LinkedHashMap<>();
        Map<Groupe, Map<Long, Set<Long>>> enBase = partiesEnBase(candidates, jeuxExistants);
        for (int i = 0; i < candidates.size(); i++) {
            Candidate candidate = candidates.get(i);
            JeuExistant existant = jeuxExistants.get(candidate.jeuCle());
            Groupe groupe = new Groupe(existant == null ? null : existant.id(), candidate.date());
            groupes.computeIfAbsent(groupe, g -> new ArrayList<>()).add(i);
        }
        boolean[] absorbees = new boolean[candidates.size()];
        for (Map.Entry<Groupe, List<Integer>> entree : groupes.entrySet()) {
            Map<Long, Set<Long>> base = enBase.get(entree.getKey());
            if (entree.getKey().jeuId() == null || base == null) {
                continue;
            }
            List<Set<Long>> fichier = new ArrayList<>();
            for (int indice : entree.getValue()) {
                Set<Long> ids = new HashSet<>();
                for (ParticipantPlan participant : candidates.get(indice).plan().participants()) {
                    Long id = effectifs.get(participant.joueurCle());
                    if (id != null) {
                        ids.add(id);
                    }
                }
                fichier.add(ids);
            }
            boolean[] resultat = Appariement.apparier(fichier, new ArrayList<>(base.values()));
            for (int k = 0; k < resultat.length; k++) {
                absorbees[entree.getValue().get(k)] = resultat[k];
            }
        }
        for (int i = 0; i < candidates.size(); i++) {
            if (absorbees[i]) {
                dejaPresentes++;
            } else {
                aAjouter.add(candidates.get(i).plan());
            }
        }

        return new PlanImport(plansJeux(jeux, jeuxExistants, aujourdhui, avertissements),
                plansJoueurs(joueurs, joueursExistants, effectifs), aAjouter, dejaPresentes,
                ignorees.total, ignorees.parRaison, ignorees.lignes, avertissements.lignes, avertissements.total);
    }

    private String raisonIgnoree(PartieMyLudo partie, LocalDate aujourdhui) {
        if (partie.exclue()) {
            return "Partie exclue dans MyLudo";
        }
        if (partie.date().isAfter(aujourdhui)) {
            return "Date dans le futur";
        }
        if (partie.joueurs().isEmpty()) {
            return "Aucun joueur";
        }
        return null;
    }

    private Candidate convertir(PartieMyLudo partie, String jeuCle, boolean cooperatif, List<String> cles,
            Map<String, Long> effectifs, Collecteur ignorees, Collecteur avertissements) {
        String reference = "Entrée n°" + partie.entree() + " (" + partie.contexte() + ")";
        Set<Long> fiches = new HashSet<>();
        for (String cle : cles) {
            Long id = effectifs.get(cle);
            if (id != null && !fiches.add(id)) {
                ignorees.ajouter("Deux noms associés au même joueur",
                        reference + " : deux noms associés au même joueur");
                return null;
            }
        }
        boolean unGagnant = partie.joueurs().stream().anyMatch(JoueurMyLudo::victoire);
        ResultatCooperatif resultat = null;
        boolean victoireCooperative = false;
        if (cooperatif) {
            victoireCooperative = partie.coop() ? partie.coopVictoire() : unGagnant;
            resultat = victoireCooperative ? ResultatCooperatif.VICTOIRE : ResultatCooperatif.DEFAITE;
        } else if (!unGagnant) {
            ignorees.ajouter("Aucun gagnant dans un jeu non coopératif",
                    reference + " : aucun gagnant dans un jeu non coopératif");
            return null;
        }

        boolean scores = partie.joueurs().stream().anyMatch(j -> j.score() != null && j.score() != 0);
        Set<String> equipes = new HashSet<>();
        partie.joueurs().stream().map(JoueurMyLudo::equipe).filter(e -> e != null).forEach(equipes::add);
        boolean garderEquipes = !equipes.isEmpty() && equipes.size() <= PartieService.EQUIPES_MAX;
        if (equipes.size() > PartieService.EQUIPES_MAX) {
            avertissements.ajouter("Équipes", reference + " : plus de " + PartieService.EQUIPES_MAX
                    + " équipes, équipes ignorées");
        }

        List<ParticipantPlan> participants = new ArrayList<>();
        for (int i = 0; i < partie.joueurs().size(); i++) {
            JoueurMyLudo joueur = partie.joueurs().get(i);
            String equipe = garderEquipes && joueur.equipe() != null ? tronquer(joueur.equipe(), Equipe.NOM_MAX) : null;
            boolean gagnant = cooperatif ? victoireCooperative : joueur.victoire();
            participants.add(new ParticipantPlan(cles.get(i), scores ? joueur.score() : null, gagnant, equipe));
        }
        String lieu = partie.lieu() == null || partie.lieu().isBlank() ? null : partie.lieu().trim();
        if (lieu != null && lieu.length() > Partie.LIEU_MAX) {
            lieu = lieu.substring(0, Partie.LIEU_MAX);
            avertissements.ajouter("Lieu", reference + " : lieu tronqué à " + Partie.LIEU_MAX + " caractères");
        }
        return new Candidate(jeuCle, partie.date(), new PartiePlan(jeuCle, partie.date(), lieu, resultat,
                participants), partie.contexte());
    }

    private static String tronquer(String texte, int max) {
        return texte.length() > max ? texte.substring(0, max) : texte;
    }

    private Map<String, JeuExistant> jeuxExistants() {
        Map<String, JeuExistant> jeux = new HashMap<>();
        for (Object[] ligne : em.createQuery(
                "select j.id, j.nom, j.prix, j.dateAchat, j.cooperatif from Jeu j order by j.id", Object[].class)
                .getResultList()) {
            jeux.putIfAbsent(NomsNormalises.cle((String) ligne[1]), new JeuExistant((Long) ligne[0],
                    (BigDecimal) ligne[2], (LocalDate) ligne[3], (Boolean) ligne[4]));
        }
        return jeux;
    }

    private Map<String, Long> joueursExistants() {
        Map<String, Long> joueurs = new HashMap<>();
        for (Object[] ligne : em.createQuery(
                "select j.id, j.nom from Joueur j where j.anonymise = false order by j.id", Object[].class)
                .getResultList()) {
            joueurs.putIfAbsent(NomsNormalises.cle((String) ligne[1]), (Long) ligne[0]);
        }
        return joueurs;
    }

    /** Joueurs de chaque partie déjà en base, pour les jeux et la période de l'export, en une seule requête. */
    private Map<Groupe, Map<Long, Set<Long>>> partiesEnBase(List<Candidate> candidates,
            Map<String, JeuExistant> jeuxExistants) {
        Set<Long> jeuIds = new HashSet<>();
        LocalDate debut = null;
        LocalDate fin = null;
        for (Candidate candidate : candidates) {
            JeuExistant existant = jeuxExistants.get(candidate.jeuCle());
            if (existant != null) {
                jeuIds.add(existant.id());
                debut = debut == null || candidate.date().isBefore(debut) ? candidate.date() : debut;
                fin = fin == null || candidate.date().isAfter(fin) ? candidate.date() : fin;
            }
        }
        Map<Groupe, Map<Long, Set<Long>>> resultat = new HashMap<>();
        if (jeuIds.isEmpty()) {
            return resultat;
        }
        for (Object[] ligne : em.createQuery("select p.jeu.id, p.datePartie, p.id, a.joueur.id from Participation a "
                + "join a.partie p where p.jeu.id in :jeux and p.datePartie between :debut and :fin", Object[].class)
                .setParameter("jeux", jeuIds).setParameter("debut", debut).setParameter("fin", fin)
                .getResultList()) {
            resultat.computeIfAbsent(new Groupe((Long) ligne[0], (LocalDate) ligne[1]), g -> new LinkedHashMap<>())
                    .computeIfAbsent((Long) ligne[2], id -> new HashSet<>()).add((Long) ligne[3]);
        }
        return resultat;
    }

    private List<JeuPlan> plansJeux(Map<String, JeuSource> jeux, Map<String, JeuExistant> existants,
            LocalDate aujourdhui, Collecteur avertissements) {
        List<JeuPlan> plans = new ArrayList<>();
        for (Map.Entry<String, JeuSource> entree : jeux.entrySet()) {
            JeuSource source = entree.getValue();
            if (!source.dansCollection && !source.utilise) {
                continue;
            }
            BigDecimal prix = prixValide(source, avertissements);
            LocalDate dateAchat = source.dateAchat;
            if (dateAchat != null && dateAchat.isAfter(aujourdhui)) {
                avertissements.ajouter("Date d'achat", source.nom + " : date d'achat dans le futur ignorée");
                dateAchat = null;
            }
            JeuExistant existant = existants.get(entree.getKey());
            if (existant != null) {
                boolean complete = (existant.prix() == null && prix != null)
                        || (existant.dateAchat() == null && dateAchat != null);
                plans.add(new JeuPlan(entree.getKey(), source.nom, complete ? Action.COMPLETER : Action.INCHANGE,
                        existant.id(), existant.prix() == null ? prix : null,
                        existant.dateAchat() == null ? dateAchat : null, existant.cooperatif()));
            } else {
                plans.add(new JeuPlan(entree.getKey(), source.nom, Action.CREER, null, prix, dateAchat,
                        source.cooperatifParties || source.mecaniqueCooperative));
            }
        }
        return plans;
    }

    private BigDecimal prixValide(JeuSource source, Collecteur avertissements) {
        if (source.prix == null) {
            return null;
        }
        BigDecimal prix = source.prix.setScale(2, RoundingMode.HALF_UP);
        if (prix.compareTo(Jeu.PRIX_MAX) > 0) {
            avertissements.ajouter("Prix", source.nom + " : prix trop élevé ignoré");
            return null;
        }
        return prix;
    }

    private List<JoueurPlan> plansJoueurs(Map<String, JoueurAcc> joueurs, Map<String, Long> existants,
            Map<String, Long> effectifs) {
        List<JoueurPlan> plans = new ArrayList<>();
        for (Map.Entry<String, JoueurAcc> entree : joueurs.entrySet()) {
            JoueurAcc acc = entree.getValue();
            plans.add(new JoueurPlan(acc.index, entree.getKey(), acc.nom, existants.get(entree.getKey()),
                    effectifs.get(entree.getKey()), acc.participations));
        }
        return plans;
    }
}
