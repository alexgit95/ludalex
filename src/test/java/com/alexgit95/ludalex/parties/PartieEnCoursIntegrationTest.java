package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuSaisie;
import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurService;

/** Cycle de vie et tokens sur base réelle (H2) : atomicité, concurrence, retour arrière. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:encours;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class PartieEnCoursIntegrationTest {

    @Autowired
    private PartieEnCoursService enCours;

    @Autowired
    private TokenFinService tokens;

    @Autowired
    private PartieService parties;

    @Autowired
    private JeuService jeux;

    @Autowired
    private JoueurService joueurs;

    @BeforeEach
    void viderLaPartieActive() {
        enCours.active().ifPresent(vue -> parties.supprimer(vue.partie().getId()));
    }

    private Jeu jeu(boolean cooperatif) {
        return jeux.creer(new JeuSaisie("Jeu-" + UUID.randomUUID(), null, null, cooperatif, null,
                EntreeImage.inchangee()));
    }

    private Joueur joueur() {
        return joueurs.creer("Joueur-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
    }

    private LancementSaisie lancement(Jeu jeu, Joueur... participants) {
        return new LancementSaisie(jeu.getId(), java.util.Arrays.stream(participants).map(Joueur::getId).toList(), null,
                null);
    }

    private static FinSaisie gagnant(Joueur joueur) {
        return new FinSaisie(List.of(joueur.getId()), null, null, null);
    }

    private VuePartie vue(Partie partie) {
        return enCours.vue(partie.getId());
    }

    @Test
    void lanceUnePartieEnCoursAvecUnTokenValide() {
        Joueur a = joueur();
        Joueur b = joueur();

        Partie partie = enCours.lancer(lancement(jeu(false), a, b));

        VuePartie vue = vue(partie);
        assertThat(vue.partie().getStatut()).isEqualTo(StatutPartie.EN_COURS);
        assertThat(vue.participations()).hasSize(2).allMatch(p -> !p.isGagnant() && p.getScore() == null);
        assertThat(tokens.aUnTokenValide(partie.getId())).isTrue();
        assertThat(enCours.active()).get().extracting(v -> v.partie().getId()).isEqualTo(partie.getId());
    }

    @Test
    void refuseUneSecondePartieTantQueLaPremiereEstActive() {
        Joueur a = joueur();
        Partie premiere = enCours.lancer(lancement(jeu(false), a));

        assertThatThrownBy(() -> enCours.lancer(lancement(jeu(false), a)))
                .isInstanceOfSatisfying(PartieActiveException.class,
                        e -> assertThat(e.getPartieActiveId()).isEqualTo(premiere.getId()));
        enCours.pauser(premiere.getId(), a.getId());
        assertThatThrownBy(() -> enCours.lancer(lancement(jeu(false), a))).isInstanceOf(PartieActiveException.class);
    }

    @Test
    void refuseUnLancementInvalide() {
        Joueur ancien = joueur();
        Jeu jeu = jeu(false);
        Joueur b = joueur();
        joueurs.supprimer(ancien.getId());

        assertThatThrownBy(() -> enCours.lancer(lancement(jeu))).isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> enCours.lancer(new LancementSaisie(null, List.of(b.getId()), null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThat(enCours.active()).isEmpty();
    }

    @Test
    void termineLaPartiePrecedenteEtLanceLaNouvelle() {
        Joueur a = joueur();
        Joueur b = joueur();
        Partie precedente = enCours.lancer(lancement(jeu(false), a, b));

        Partie nouvelle = enCours.terminerEtLancer(precedente.getId(),
                new FinSaisie(List.of(a.getId()), null, Map.of(a.getId(), 12), null), lancement(jeu(false), b));

        DetailPartie ancienne = parties.detail(precedente.getId());
        assertThat(ancienne.partie().getStatut()).isEqualTo(StatutPartie.TERMINEE);
        assertThat(ancienne.partie().getTermineeLe()).isNotNull();
        assertThat(ancienne.participations()).filteredOn(Participation::isGagnant)
                .extracting(p -> p.getJoueur().getId()).containsExactly(a.getId());
        assertThat(ancienne.participations()).filteredOn(p -> p.getScore() != null).hasSize(1);
        assertThat(tokens.aUnTokenValide(precedente.getId())).isFalse();
        assertThat(enCours.active()).get().extracting(v -> v.partie().getId()).isEqualTo(nouvelle.getId());
        assertThat(tokens.aUnTokenValide(nouvelle.getId())).isTrue();
    }

    @Test
    void laissePartieEtTokenIntactsSiLeLancementSuivantEstInvalide() {
        Joueur a = joueur();
        Partie precedente = enCours.lancer(lancement(jeu(false), a));

        assertThatThrownBy(() -> enCours.terminerEtLancer(precedente.getId(), gagnant(a),
                new LancementSaisie(null, List.of(a.getId()), null, null)))
                .isInstanceOf(PartieInvalideException.class);

        assertThat(parties.detail(precedente.getId()).partie().getStatut()).isEqualTo(StatutPartie.EN_COURS);
        assertThat(parties.detail(precedente.getId()).partie().getTermineeLe()).isNull();
        assertThat(parties.detail(precedente.getId()).participations()).noneMatch(Participation::isGagnant);
        assertThat(tokens.aUnTokenValide(precedente.getId())).isTrue();
    }

    @Test
    void metEnPauseInvalideLesTokensPuisRepriseEnGenereUnNouveau() {
        Joueur a = joueur();
        Joueur b = joueur();
        Partie partie = enCours.lancer(lancement(jeu(false), a, b));

        enCours.pauser(partie.getId(), b.getId());

        VuePartie enPause = vue(partie);
        assertThat(enPause.partie().getStatut()).isEqualTo(StatutPartie.EN_PAUSE);
        assertThat(enPause.joueurSuivant().getId()).isEqualTo(b.getId());
        assertThat(tokens.aUnTokenValide(partie.getId())).isFalse();

        enCours.reprendre(partie.getId());

        VuePartie reprise = vue(partie);
        assertThat(reprise.partie().getStatut()).isEqualTo(StatutPartie.EN_COURS);
        assertThat(reprise.joueurSuivant()).isNull();
        assertThat(tokens.aUnTokenValide(partie.getId())).isTrue();
    }

    @Test
    void termineUnePartieEnPause() {
        Joueur a = joueur();
        Partie partie = enCours.lancer(lancement(jeu(false), a));
        enCours.pauser(partie.getId(), a.getId());

        enCours.terminer(partie.getId(), gagnant(a));

        assertThat(parties.detail(partie.getId()).partie().getStatut()).isEqualTo(StatutPartie.TERMINEE);
        assertThat(enCours.active()).isEmpty();
    }

    @Test
    void termineUnePartieCooperativeEtDesEquipesGagnantes() {
        Joueur a = joueur();
        Joueur b = joueur();
        Partie cooperative = enCours.lancer(lancement(jeu(true), a, b));
        enCours.terminer(cooperative.getId(), new FinSaisie(null, null, null, ResultatCooperatif.VICTOIRE));
        assertThat(parties.detail(cooperative.getId()).participations()).allMatch(Participation::isGagnant);

        Joueur c = joueur();
        Partie equipes = enCours.lancer(new LancementSaisie(jeu(false).getId(), List.of(a.getId(), b.getId(), c.getId()),
                Map.of(a.getId(), 1, b.getId(), 1, c.getId(), 2), Map.of(1, "Les Rouges")));
        Long equipeRouge = vue(equipes).equipes().stream().filter(e -> e.getNom().equals("Les Rouges")).findFirst()
                .orElseThrow().getId();

        enCours.terminer(equipes.getId(), new FinSaisie(null, List.of(equipeRouge), null, null));

        assertThat(parties.detail(equipes.getId()).participations()).filteredOn(Participation::isGagnant)
                .extracting(p -> p.getJoueur().getId()).containsExactlyInAnyOrder(a.getId(), b.getId());
    }

    @Test
    void laissePartieEtTokenIntactsSiLaFinEstInvalide() {
        Joueur a = joueur();
        Partie partie = enCours.lancer(lancement(jeu(false), a));

        assertThatThrownBy(() -> enCours.terminer(partie.getId(), new FinSaisie(List.of(), null, null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> enCours.terminer(partie.getId(), new FinSaisie(List.of(a.getId()), null,
                Map.of(999999L, 3), null))).isInstanceOf(PartieInvalideException.class);

        assertThat(parties.detail(partie.getId()).partie().getStatut()).isEqualTo(StatutPartie.EN_COURS);
        assertThat(tokens.aUnTokenValide(partie.getId())).isTrue();
    }

    @Test
    void neLanceQuUneSeulePartieMemeAvecDeuxLancementsSimultanes() throws Exception {
        Joueur a = joueur();
        Jeu jeu = jeu(false);
        ExecutorService executeur = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch depart = new CountDownLatch(1);
            List<Future<Boolean>> resultats = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                resultats.add(executeur.submit(tentative(depart, () -> enCours.lancer(lancement(jeu, a)))));
            }
            depart.countDown();
            long acceptes = 0;
            for (Future<Boolean> resultat : resultats) {
                acceptes += resultat.get() ? 1 : 0;
            }

            assertThat(acceptes).isEqualTo(1);
            assertThat(enCours.active()).isPresent();
        } finally {
            executeur.shutdownNow();
        }
    }

    @Test
    void neConsommeUnTokenQuUneFoisMemeAvecDeuxAppelsSimultanes() throws Exception {
        Joueur a = joueur();
        Joueur b = joueur();
        Partie partie = enCours.lancer(lancement(jeu(false), a, b));
        String clair = tokens.generer(partie);
        ExecutorService executeur = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch depart = new CountDownLatch(1);
            List<Future<Boolean>> resultats = new ArrayList<>();
            for (Joueur gagnant : new Joueur[] { a, b }) {
                resultats.add(executeur.submit(tentative(depart,
                        () -> {
                            enCours.terminerViaToken(clair, gagnant(gagnant));
                            return null;
                        })));
            }
            depart.countDown();
            long acceptes = 0;
            for (Future<Boolean> resultat : resultats) {
                acceptes += resultat.get() ? 1 : 0;
            }

            assertThat(acceptes).isEqualTo(1);
            assertThat(parties.detail(partie.getId()).participations()).filteredOn(Participation::isGagnant).hasSize(1);
        } finally {
            executeur.shutdownNow();
        }
    }

    @Test
    void unTokenNeServQuUneFois() {
        Joueur a = joueur();
        Partie partie = enCours.lancer(lancement(jeu(false), a));
        String clair = tokens.generer(partie);

        enCours.terminerViaToken(clair, gagnant(a));

        assertThatThrownBy(() -> enCours.terminerViaToken(clair, gagnant(a))).isInstanceOf(TokenFinInvalideException.class);
        assertThatThrownBy(() -> enCours.pauserViaToken(clair, a.getId())).isInstanceOf(TokenFinInvalideException.class);
        assertThat(enCours.vueDuLien(clair)).isEmpty();
    }

    @Test
    void unEchecDeValidationNeConsommePasLeToken() {
        Joueur a = joueur();
        Joueur etranger = joueur();
        Partie partie = enCours.lancer(lancement(jeu(false), a));
        String clair = tokens.generer(partie);

        assertThatThrownBy(() -> enCours.terminerViaToken(clair, new FinSaisie(List.of(), null, null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> enCours.terminerViaToken(clair, gagnant(etranger)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> enCours.pauserViaToken(clair, etranger.getId()))
                .isInstanceOf(PartieInvalideException.class);

        assertThat(enCours.vueDuLien(clair)).isPresent();
        enCours.terminerViaToken(clair, gagnant(a));
        assertThat(parties.detail(partie.getId()).partie().getStatut()).isEqualTo(StatutPartie.TERMINEE);
    }

    @Test
    void metEnPauseViaLeTokenEtLeConsomme() {
        Joueur a = joueur();
        Joueur b = joueur();
        Partie partie = enCours.lancer(lancement(jeu(false), a, b));
        String clair = tokens.generer(partie);

        enCours.pauserViaToken(clair, b.getId());

        assertThat(vue(partie).partie().getStatut()).isEqualTo(StatutPartie.EN_PAUSE);
        assertThat(vue(partie).joueurSuivant().getId()).isEqualTo(b.getId());
        assertThat(enCours.vueDuLien(clair)).isEmpty();
    }

    @Test
    void unTokenNeFonctionnePlusQuandUnNouveauEstGenere() {
        Joueur a = joueur();
        Partie partie = enCours.lancer(lancement(jeu(false), a));
        String ancien = tokens.generer(partie);
        String recent = tokens.generer(partie);

        assertThat(enCours.vueDuLien(ancien)).isEmpty();
        assertThat(enCours.vueDuLien(recent)).isPresent();
    }

    @Test
    void supprimerUnePartieActiveSupprimeSesTokens() {
        Joueur a = joueur();
        Partie partie = enCours.lancer(lancement(jeu(false), a));
        String clair = tokens.generer(partie);

        parties.supprimer(partie.getId());

        assertThat(enCours.vueDuLien(clair)).isEmpty();
        assertThat(enCours.active()).isEmpty();
    }

    private Callable<Boolean> tentative(CountDownLatch depart, Callable<?> action) {
        return () -> {
            depart.await();
            try {
                action.call();
                return true;
            } catch (PartieActiveException | TokenFinInvalideException e) {
                return false;
            }
        };
    }
}
