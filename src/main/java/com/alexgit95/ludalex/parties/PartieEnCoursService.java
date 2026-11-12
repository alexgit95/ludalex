package com.alexgit95.ludalex.parties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.alexgit95.ludalex.joueurs.Joueur;

/**
 * Cycle de vie d'une partie en direct : lancement, pause, reprise, fin. Une seule partie est active
 * (en cours ou en pause) à la fois ; les lancements sont sérialisés, l'application n'ayant qu'une instance.
 */
@Service
public class PartieEnCoursService {

    private final ReentrantLock verrou = new ReentrantLock();

    private final PartieService parties;
    private final PartieRepository repertoire;
    private final TokenFinService tokens;
    private final TransactionTemplate transaction;

    public PartieEnCoursService(PartieService parties, PartieRepository repertoire, TokenFinService tokens,
            PlatformTransactionManager gestionnaire) {
        this.parties = parties;
        this.repertoire = repertoire;
        this.tokens = tokens;
        this.transaction = new TransactionTemplate(gestionnaire);
    }

    /** Lance une partie ; refuse si une autre est déjà en cours ou en pause. */
    public Partie lancer(LancementSaisie lancement) {
        return sousVerrou(() -> creerSiLibre(lancement));
    }

    /** Termine la partie précédente puis lance la nouvelle, atomiquement : si le lancement échoue, rien ne change. */
    public Partie terminerEtLancer(Long precedenteId, FinSaisie fin, LancementSaisie lancement) {
        return sousVerrou(() -> {
            Partie precedente = trouverActive(precedenteId);
            parties.cloturer(precedente, fin);
            tokens.invaliderPour(precedente.getId());
            return creerSiLibre(lancement);
        });
    }

    @Transactional
    public Partie pauser(Long id, Long joueurSuivantId) {
        return mettreEnPause(trouverActive(id), joueurSuivantId);
    }

    @Transactional
    public Partie reprendre(Long id) {
        Partie partie = trouverActive(id);
        if (partie.getStatut() != StatutPartie.EN_PAUSE) {
            throw new PartieInvalideException("statut", "La partie n'est pas en pause");
        }
        partie.setStatut(StatutPartie.EN_COURS);
        partie.setJoueurSuivant(null);
        repertoire.save(partie);
        tokens.generer(partie);
        return partie;
    }

    @Transactional
    public Partie terminer(Long id, FinSaisie fin) {
        Partie partie = trouverActive(id);
        parties.cloturer(partie, fin);
        tokens.invaliderPour(partie.getId());
        return partie;
    }

    /** Fin par lien : le token est consommé en premier, donc tout échec ultérieur l'annule avec le reste. */
    @Transactional
    public void terminerViaToken(String token, FinSaisie fin) {
        Partie partie = partieDuToken(token);
        parties.cloturer(partie, fin);
        tokens.invaliderPour(partie.getId());
    }

    @Transactional
    public void pauserViaToken(String token, Long joueurSuivantId) {
        mettreEnPause(partieDuToken(token), joueurSuivantId);
    }

    @Transactional(readOnly = true)
    public Optional<VuePartie> vueDuLien(String token) {
        return tokens.verifier(token).map(valide -> composer(trouverActive(valide.getPartie().getId())));
    }

    @Transactional(readOnly = true)
    public Optional<VuePartie> active() {
        return repertoire.trouverActives().stream().findFirst().map(this::composer);
    }

    @Transactional(readOnly = true)
    public VuePartie vue(Long id) {
        return composer(trouverActive(id));
    }

    private Partie creerSiLibre(LancementSaisie lancement) {
        List<Partie> actives = repertoire.trouverActives();
        if (!actives.isEmpty()) {
            throw new PartieActiveException(actives.get(0).getId());
        }
        Partie partie = parties.creerEnCours(lancement);
        tokens.generer(partie);
        return partie;
    }

    private Partie mettreEnPause(Partie partie, Long joueurSuivantId) {
        if (partie.getStatut() != StatutPartie.EN_COURS) {
            throw new PartieInvalideException("statut", "La partie n'est pas en cours");
        }
        Joueur suivant = partie.getParticipations().stream().map(Participation::getJoueur)
                .filter(joueur -> joueur.getId().equals(joueurSuivantId)).findFirst()
                .orElseThrow(() -> new PartieInvalideException("joueurSuivant",
                        "Indiquez à quel joueur c'était de jouer"));
        partie.setJoueurSuivant(suivant);
        partie.setStatut(StatutPartie.EN_PAUSE);
        repertoire.save(partie);
        tokens.invaliderPour(partie.getId());
        return partie;
    }

    private Partie partieDuToken(String token) {
        TokenFinPartie consomme = tokens.consommer(token);
        Partie partie = repertoire.trouverAvecJeu(consomme.getPartie().getId())
                .orElseThrow(TokenFinInvalideException::new);
        if (partie.getStatut() != StatutPartie.EN_COURS) {
            throw new TokenFinInvalideException();
        }
        return partie;
    }

    private Partie trouverActive(Long id) {
        Partie partie = repertoire.trouverAvecJeu(id).orElseThrow(() -> new PartieIntrouvableException(id));
        if (partie.getStatut() == StatutPartie.TERMINEE) {
            throw new PartieInvalideException("statut", "Cette partie est déjà terminée");
        }
        return partie;
    }

    private VuePartie composer(Partie partie) {
        List<Participation> participations = repertoire.participationsDesParties(List.of(partie.getId()));
        Joueur suivant = partie.getJoueurSuivant();
        if (suivant != null) {
            suivant.getNom();
        }
        return new VuePartie(partie, partie.getJeu(), participations, new ArrayList<>(partie.getEquipes()), suivant);
    }

    private <T> T sousVerrou(Supplier<T> action) {
        verrou.lock();
        try {
            return transaction.execute(statut -> action.get());
        } finally {
            verrou.unlock();
        }
    }
}
