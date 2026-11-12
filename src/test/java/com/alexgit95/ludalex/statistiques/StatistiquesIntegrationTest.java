package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuSaisie;
import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurService;
import com.alexgit95.ludalex.parties.PartieSaisie;
import com.alexgit95.ludalex.parties.PartieService;
import com.alexgit95.ludalex.parties.ResultatCooperatif;

import jakarta.persistence.EntityManagerFactory;

/** Statistiques sur données réelles (H2) : agrégation, égalité, anonymisation, nombre de requêtes. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stats;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class StatistiquesIntegrationTest {

    @Autowired
    private StatistiquesService statistiques;

    @Autowired
    private PartieService parties;

    @Autowired
    private JeuService jeux;

    @Autowired
    private JoueurService joueurs;

    @Autowired
    private EntityManagerFactory fabrique;

    private Jeu jeu(boolean cooperatif) {
        return jeux.creer(new JeuSaisie("Jeu-" + UUID.randomUUID(), null, null, cooperatif, null,
                EntreeImage.inchangee()));
    }

    private Joueur joueur() {
        return joueurs.creer("Joueur-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
    }

    private void partie(Jeu jeu, int jour, List<Joueur> participants, Joueur gagnant) {
        parties.declarer(new PartieSaisie(jeu.getId(), LocalDate.of(2026, 1, 1).plusDays(jour), "Lieu " + jour,
                participants.stream().map(Joueur::getId).toList(), null, List.of(gagnant.getId()), null, null, null,
                null));
    }

    @Test
    void calculeClassementEtChampionsALegalite() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        Joueur b = joueur();
        Joueur c = joueur();
        for (int i = 0; i < 3; i++) {
            partie(catan, i, List.of(a, b, c), a);
        }
        for (int i = 3; i < 6; i++) {
            partie(catan, i, List.of(a, b, c), b);
        }
        partie(catan, 6, List.of(a, b, c), c);

        StatistiquesJeu stats = statistiques.pourJeu(catan.getId());

        assertThat(stats.classement()).extracting(l -> l.joueur().getId())
                .containsExactlyInAnyOrder(a.getId(), b.getId(), c.getId());
        assertThat(stats.classement().get(2).joueur().getId()).isEqualTo(c.getId());
        assertThat(stats.classement()).allSatisfy(ligne -> assertThat(ligne.parties()).isEqualTo(7));
        assertThat(stats.champions()).extracting(Joueur::getId).containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(stats.classement().get(0).tauxPourcent()).isEqualTo(43);
    }

    @Test
    void uneVictoireCooperativeCompteChezChaqueParticipant() {
        Jeu pandemic = jeu(true);
        Joueur a = joueur();
        Joueur b = joueur();
        parties.declarer(new PartieSaisie(pandemic.getId(), null, null, List.of(a.getId(), b.getId()), null, null,
                null, null, null, ResultatCooperatif.VICTOIRE));
        parties.declarer(new PartieSaisie(pandemic.getId(), null, null, List.of(a.getId(), b.getId()), null, null,
                null, null, null, ResultatCooperatif.DEFAITE));

        StatistiquesJeu stats = statistiques.pourJeu(pandemic.getId());

        assertThat(stats.classement()).allSatisfy(ligne -> {
            assertThat(ligne.parties()).isEqualTo(2);
            assertThat(ligne.victoires()).isEqualTo(1);
            assertThat(ligne.tauxPourcent()).isEqualTo(50);
        });
        assertThat(stats.champions()).hasSize(2);
    }

    @Test
    void lAnonymisationEcarteLeJoueurDuTitreSansEffacerSesParties() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        Joueur b = joueur();
        for (int i = 0; i < 3; i++) {
            partie(catan, i, List.of(a, b), a);
        }
        for (int i = 3; i < 5; i++) {
            partie(catan, i, List.of(a, b), b);
        }

        joueurs.supprimer(a.getId());
        StatistiquesJeu stats = statistiques.pourJeu(catan.getId());

        assertThat(stats.champions()).extracting(Joueur::getId).containsExactly(b.getId());
        assertThat(stats.classement()).hasSize(2);
        assertThat(stats.classement().get(0).joueur().getNom()).isEqualTo(Joueur.NOM_ANONYME);
        assertThat(statistiques.pour(List.of(catan.getId())).get(catan.getId())).extracting(Joueur::getId)
                .containsExactly(b.getId());
    }

    @Test
    void neRetientQueLesDixDernieresPartiesDuPlusRecentAuPlusAncien() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        for (int i = 0; i < 12; i++) {
            partie(catan, i, List.of(a), a);
        }

        List<com.alexgit95.ludalex.parties.LignePartie> dernieres = statistiques.pourJeu(catan.getId())
                .dernieresParties();

        assertThat(dernieres).hasSize(10);
        assertThat(dernieres.get(0).partie().getLieu()).isEqualTo("Lieu 11");
        assertThat(dernieres.get(9).partie().getLieu()).isEqualTo("Lieu 2");
        assertThat(dernieres.get(0).participations()).hasSize(1);
        assertThat(dernieres.get(0).participations().get(0).isGagnant()).isTrue();
    }

    @Test
    void neRenvoieRienPourUnJeuSansPartie() {
        StatistiquesJeu stats = statistiques.pourJeu(jeu(false).getId());

        assertThat(stats.vide()).isTrue();
        assertThat(stats.champions()).isEmpty();
        assertThat(stats.dernieresParties()).isEmpty();
    }

    @Test
    void obtientLesChampionsDUnePageEnUnNombreConstantDeRequetes() {
        Joueur a = joueur();
        Joueur b = joueur();
        List<Long> deuxJeux = new ArrayList<>();
        List<Long> vingtQuatreJeux = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Jeu jeu = jeu(false);
            partie(jeu, 0, List.of(a, b), i % 2 == 0 ? a : b);
            vingtQuatreJeux.add(jeu.getId());
            if (i < 2) {
                deuxJeux.add(jeu.getId());
            }
        }
        Statistics suivi = fabrique.unwrap(SessionFactory.class).getStatistics();
        suivi.setStatisticsEnabled(true);

        suivi.clear();
        Map<Long, List<Joueur>> pourDeux = statistiques.pour(deuxJeux);
        long requetesPourDeux = suivi.getPrepareStatementCount();
        suivi.clear();
        Map<Long, List<Joueur>> pourTous = statistiques.pour(vingtQuatreJeux);
        long requetesPourTous = suivi.getPrepareStatementCount();
        suivi.setStatisticsEnabled(false);

        assertThat(pourDeux).hasSize(2);
        assertThat(pourTous).hasSize(24);
        assertThat(requetesPourTous).isEqualTo(requetesPourDeux).isLessThanOrEqualTo(3);
    }
}
