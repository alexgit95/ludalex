package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.alexgit95.ludalex.donnees.DonneesModifiees;
import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuSaisie;
import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.jeux.PretService;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurService;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieSaisie;
import com.alexgit95.ludalex.parties.PartieService;
import com.alexgit95.ludalex.reglages.ReglageCle;
import com.alexgit95.ludalex.reglages.ReglagesService;

import jakarta.persistence.EntityManager;

/** Chaque chemin d'écriture invalide le cache, et seulement après le commit. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:invalidation;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(InvalidationCacheIntegrationTest.Compteur.class)
class InvalidationCacheIntegrationTest {

    static class Compteur {
        static final AtomicInteger EVENEMENTS = new AtomicInteger();

        @EventListener(DonneesModifiees.class)
        void compter() {
            EVENEMENTS.incrementAndGet();
        }
    }

    @Autowired
    private JeuService jeux;
    @Autowired
    private JoueurService joueurs;
    @Autowired
    private PartieService parties;
    @Autowired
    private PretService prets;
    @Autowired
    private ReglagesService reglages;
    @Autowired
    private CacheStatistiques cache;
    @Autowired
    private PlatformTransactionManager gestionnaire;
    @Autowired
    private EntityManager em;

    @BeforeEach
    void reinitialiser() {
        Compteur.EVENEMENTS.set(0);
    }

    private Jeu jeu() {
        return jeux.creer(new JeuSaisie("Jeu-" + UUID.randomUUID(), null, null, false, null, EntreeImage.inchangee()));
    }

    private Joueur joueur() {
        return joueurs.creer("Joueur-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
    }

    private int evenements() {
        return Compteur.EVENEMENTS.get();
    }

    @Test
    void ajouterUnJeuInvalide() {
        jeu();

        assertThat(evenements()).isPositive();
    }

    @Test
    void creerUnJoueurInvalide() {
        joueur();

        assertThat(evenements()).isPositive();
    }

    @Test
    void declarerModifierEtSupprimerUnePartieInvalide() {
        Jeu jeu = jeu();
        Joueur a = joueur();
        Joueur b = joueur();
        Partie partie = parties.declarer(new PartieSaisie(jeu.getId(), LocalDate.now(), "Salon",
                List.of(a.getId(), b.getId()), null, List.of(a.getId()), null, null, null, null));
        assertThat(evenements()).isPositive();

        Compteur.EVENEMENTS.set(0);
        parties.supprimer(partie.getId());
        assertThat(evenements()).isPositive();
    }

    @Test
    void preterEtRetournerUnJeuInvalide() {
        Jeu jeu = jeu();
        Compteur.EVENEMENTS.set(0);

        prets.preter(jeu.getId(), "Voisin", LocalDate.now());
        assertThat(evenements()).isPositive();

        Compteur.EVENEMENTS.set(0);
        prets.retourner(jeu.getId(), LocalDate.now());
        assertThat(evenements()).isPositive();
    }

    @Test
    void anonymiserUnJoueurInvalide() {
        Jeu jeu = jeu();
        Joueur a = joueur();
        parties.declarer(new PartieSaisie(jeu.getId(), LocalDate.now(), null, List.of(a.getId()), null,
                List.of(a.getId()), null, null, null, null));
        Compteur.EVENEMENTS.set(0);

        joueurs.supprimer(a.getId());

        assertThat(evenements()).isPositive();
    }

    @Test
    void modifierUnReglageInvalide() {
        reglages.enregistrer(java.util.Map.of(ReglageCle.PRET_LONG_JOURS, "31"));
        assertThat(evenements()).isPositive();

        Compteur.EVENEMENTS.set(0);
        reglages.retablir(ReglageCle.PRET_LONG_JOURS);
        assertThat(evenements()).isPositive();
    }

    @Test
    void nInvalidePasAvantLeCommitNiApresUnRetourArriere() {
        TransactionTemplate tx = new TransactionTemplate(gestionnaire);
        tx.executeWithoutResult(statut -> {
            em.persist(new Jeu("Dans une transaction"));
            em.flush();
            assertThat(evenements()).isZero();
        });
        assertThat(evenements()).isEqualTo(1);

        Compteur.EVENEMENTS.set(0);
        tx.executeWithoutResult(statut -> {
            em.persist(new Jeu("Annule"));
            em.flush();
            statut.setRollbackOnly();
        });
        assertThat(evenements()).isZero();
    }

    @Test
    void uneSeuleInvalidationParTransaction() {
        TransactionTemplate tx = new TransactionTemplate(gestionnaire);
        tx.executeWithoutResult(statut -> {
            for (int i = 0; i < 5; i++) {
                em.persist(new Jeu("Lot " + i));
            }
        });

        assertThat(evenements()).isEqualTo(1);
    }

    @Test
    void leCacheEstVideApresUneEcriture() {
        AtomicInteger calculs = new AtomicInteger();
        cache.obtenir(PeriodeStatistiques.TOUT, calculs::incrementAndGet);
        cache.obtenir(PeriodeStatistiques.TOUT, calculs::incrementAndGet);
        assertThat(calculs).hasValue(1);

        jeu();

        cache.obtenir(PeriodeStatistiques.TOUT, calculs::incrementAndGet);
        assertThat(calculs).hasValue(2);
    }
}
