package com.alexgit95.ludalex.importmyludo;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.JeuMyLudo;
import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.PartieMyLudo;
import com.alexgit95.ludalex.importmyludo.PlanImport.JoueurPlan;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurRepository;

/**
 * Une analyse ou un import à la fois (application mono-utilisateur) : le traitement tourne sur un fil dédié et la
 * page en suit l'avancement. Les fichiers, de 2 Mo au plus, sont lus une seule fois et conservés en mémoire sous
 * forme d'enregistrements jusqu'à la fin, l'annulation ou l'expiration.
 */
@Service
public class ServiceImportMyLudo {

    public enum Etat {
        ANALYSE_EN_COURS, ANALYSE_PRETE, IMPORT_EN_COURS, TERMINE, ERREUR
    }

    public record JoueurChoix(Long id, String nom) {
    }

    /** Photographie de la session pour l'affichage. */
    public record Vue(Etat etat, Progression progression, PlanImport plan, RapportImport rapport, String erreur,
            List<String> detailsErreur, List<String> fichiers, List<JoueurChoix> joueursExistants,
            String signature) {

        public boolean enCours() {
            return etat == Etat.ANALYSE_EN_COURS || etat == Etat.IMPORT_EN_COURS;
        }
    }

    private static final class Session {
        volatile Etat etat = Etat.ANALYSE_EN_COURS;
        final Progression progression = new Progression();
        final List<String> fichiers;
        volatile List<JeuMyLudo> jeux = List.of();
        volatile List<PartieMyLudo> parties = List.of();
        volatile PlanImport plan;
        volatile RapportImport rapport;
        volatile String erreur;
        volatile List<String> detailsErreur = List.of();
        volatile Instant expireLe;

        Session(List<String> fichiers) {
            this.fichiers = fichiers;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServiceImportMyLudo.class);

    private final LecteurExportMyLudo lecteur;
    private final AnalyseurImport analyseur;
    private final ImportTransactionnel importTransactionnel;
    private final JoueurRepository joueurs;
    private final ImportMyLudoProperties proprietes;
    private final Clock horloge;
    private final ExecutorService executeur = Executors.newSingleThreadExecutor(travail -> {
        Thread fil = new Thread(travail, "import-myludo");
        fil.setDaemon(true);
        return fil;
    });
    private Session session;

    public ServiceImportMyLudo(LecteurExportMyLudo lecteur, AnalyseurImport analyseur,
            ImportTransactionnel importTransactionnel, JoueurRepository joueurs, ImportMyLudoProperties proprietes,
            Clock horloge) {
        this.lecteur = lecteur;
        this.analyseur = analyseur;
        this.importTransactionnel = importTransactionnel;
        this.joueurs = joueurs;
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    @PreDestroy
    void arreter() {
        executeur.shutdownNow();
    }

    /** @param collection ou {@code parties} peut être vide : au moins un des deux est obligatoire */
    public synchronized void demarrerAnalyse(byte[] collection, String nomCollection, byte[] parties,
            String nomParties) {
        verifierLibre();
        boolean aCollection = collection != null && collection.length > 0;
        boolean aParties = parties != null && parties.length > 0;
        if (!aCollection && !aParties) {
            throw ImportMyLudoException.simple("Choisissez au moins un fichier d'export MyLudo");
        }
        Session nouvelle = new Session(java.util.stream.Stream.of(aCollection ? nomCollection : null,
                aParties ? nomParties : null).filter(nom -> nom != null).toList());
        session = nouvelle;
        executeur.execute(() -> analyser(nouvelle, aCollection ? collection : null, nomCollection,
                aParties ? parties : null, nomParties));
    }

    private void analyser(Session s, byte[] collection, String nomCollection, byte[] parties, String nomParties) {
        try {
            s.progression.demarrer(Progression.Phase.LECTURE, 0);
            s.jeux = collection == null ? List.of() : lecteur.lireCollection(collection, nomCollection);
            s.parties = parties == null ? List.of() : lecteur.lireParties(parties, nomParties);
            if (s.jeux.isEmpty() && s.parties.isEmpty()) {
                throw ImportMyLudoException.simple("Les fichiers ne contiennent aucun enregistrement à importer");
            }
            s.plan = analyseur.analyser(s.jeux, s.parties, Map.of(), s.progression);
            terminer(s, Etat.ANALYSE_PRETE);
        } catch (ImportMyLudoException e) {
            echec(s, e);
        } catch (RuntimeException e) {
            LOG.error("Échec de l'analyse de l'import MyLudo", e);
            echec(s, null);
        }
    }

    /** @param choix index d'un joueur du plan vers l'identifiant choisi (vide : créer ; absent : proposition) */
    public synchronized void demarrerImport(Map<Integer, String> choix) {
        Session courante = session;
        if (courante == null || courante.etat != Etat.ANALYSE_PRETE) {
            throw ImportMyLudoException.simple("Aucune analyse prête à importer : relancez l'analyse");
        }
        Map<String, Long> associations = associations(courante.plan, choix);
        courante.etat = Etat.IMPORT_EN_COURS;
        executeur.execute(() -> importer(courante, associations));
    }

    private Map<String, Long> associations(PlanImport plan, Map<Integer, String> choix) {
        Set<Long> valides = joueurs.findByAnonymiseFalseOrderByNomNormaliseAsc().stream().map(Joueur::getId)
                .collect(Collectors.toSet());
        Map<String, Long> associations = new HashMap<>();
        for (JoueurPlan joueur : plan.joueurs()) {
            String valeur = choix.get(joueur.index());
            if (valeur == null) {
                continue;
            }
            if (valeur.isBlank()) {
                associations.put(joueur.cle(), null);
                continue;
            }
            try {
                Long id = Long.valueOf(valeur.trim());
                if (!valides.contains(id)) {
                    throw new NumberFormatException();
                }
                associations.put(joueur.cle(), id);
            } catch (NumberFormatException e) {
                throw ImportMyLudoException.simple("Choix de joueur invalide pour « " + joueur.nom() + " »");
            }
        }
        return associations;
    }

    private void importer(Session s, Map<String, Long> associations) {
        try {
            s.rapport = importTransactionnel.executer(s.jeux, s.parties, associations, s.progression);
            terminer(s, Etat.TERMINE);
        } catch (ImportMyLudoException e) {
            echec(s, e);
        } catch (RuntimeException e) {
            LOG.error("Échec de l'import MyLudo", e);
            echec(s, null);
        }
    }

    public synchronized void abandonner() {
        if (session != null && !session.etat.name().endsWith("EN_COURS")) {
            session = null;
        }
    }

    /** Session courante, ou {@code null} s'il n'y en a pas ou qu'elle a expiré. */
    public synchronized Vue vue() {
        Session courante = session;
        if (courante == null) {
            return null;
        }
        boolean enCours = courante.etat == Etat.ANALYSE_EN_COURS || courante.etat == Etat.IMPORT_EN_COURS;
        if (!enCours && courante.expireLe != null && Instant.now(horloge).isAfter(courante.expireLe)) {
            session = null;
            return null;
        }
        List<JoueurChoix> existants = courante.etat == Etat.ANALYSE_PRETE
                ? joueurs.findByAnonymiseFalseOrderByNomNormaliseAsc().stream()
                        .map(joueur -> new JoueurChoix(joueur.getId(), joueur.getNom())).toList()
                : List.of();
        return new Vue(courante.etat, courante.progression, courante.plan, courante.rapport, courante.erreur,
                courante.detailsErreur, courante.fichiers, existants,
                courante.etat + ":" + courante.progression.signature());
    }

    private void verifierLibre() {
        if (session != null && (session.etat == Etat.ANALYSE_EN_COURS || session.etat == Etat.IMPORT_EN_COURS)) {
            throw new ImportEnCoursException();
        }
    }

    private void terminer(Session s, Etat etat) {
        s.expireLe = Instant.now(horloge).plus(proprietes.dureeSession());
        s.etat = etat;
    }

    /** Un message précis pour une erreur d'export, un message générique pour tout le reste. */
    private void echec(Session s, ImportMyLudoException e) {
        s.erreur = e == null ? "L'opération a échoué de façon inattendue ; rien n'a été enregistré. "
                + "Consultez les journaux de l'application." : e.getMessage();
        s.detailsErreur = e == null ? List.of() : e.details();
        terminer(s, Etat.ERREUR);
    }
}
