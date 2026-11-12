package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuRepository;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurRepository;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieRepository;
import com.alexgit95.ludalex.parties.StatutPartie;

@DataJpaTest(properties = "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect")
class StatistiqueRepositoryTest {

    @Autowired
    private StatistiqueRepository statistiques;

    @Autowired
    private JeuRepository jeux;

    @Autowired
    private JoueurRepository joueurs;

    @Autowired
    private PartieRepository parties;

    private Partie partie(Jeu jeu, StatutPartie statut, Object... joueursEtVictoires) {
        Partie partie = new Partie(jeu, LocalDate.of(2026, 5, 2));
        partie.setStatut(statut);
        for (int i = 0; i < joueursEtVictoires.length; i += 2) {
            Participation participation = new Participation(partie, (Joueur) joueursEtVictoires[i]);
            participation.setGagnant((Boolean) joueursEtVictoires[i + 1]);
            partie.getParticipations().add(participation);
        }
        return parties.save(partie);
    }

    @Test
    void comptePartiesEtVictoiresDesPartiesTermineesDUnJeu() {
        Jeu catan = jeux.save(new Jeu("Catan"));
        Jeu azul = jeux.save(new Jeu("Azul"));
        Joueur alex = joueurs.save(new Joueur("Alex"));
        Joueur marie = joueurs.save(new Joueur("Marie"));
        partie(catan, StatutPartie.TERMINEE, alex, true, marie, false);
        partie(catan, StatutPartie.TERMINEE, alex, true, marie, false);
        partie(catan, StatutPartie.TERMINEE, alex, false, marie, true);
        partie(azul, StatutPartie.TERMINEE, marie, true);

        List<ResultatJoueur> classement = statistiques.classement(catan.getId());

        assertThat(classement).containsExactlyInAnyOrder(
                new ResultatJoueur(alex.getId(), 3L, 2L), new ResultatJoueur(marie.getId(), 3L, 1L));
    }

    @Test
    void ignoreLesPartiesEnCoursEtEnPause() {
        Jeu catan = jeux.save(new Jeu("Catan"));
        Joueur alex = joueurs.save(new Joueur("Alex"));
        partie(catan, StatutPartie.TERMINEE, alex, true);
        partie(catan, StatutPartie.EN_COURS, alex, true);
        partie(catan, StatutPartie.EN_PAUSE, alex, true);

        assertThat(statistiques.classement(catan.getId()))
                .containsExactly(new ResultatJoueur(alex.getId(), 1L, 1L));
        assertThat(statistiques.victoires(List.of(catan.getId())))
                .containsExactly(new VictoiresJeuJoueur(catan.getId(), alex.getId(), 1L));
    }

    @Test
    void uneVictoireCooperativeCompteChezChaqueParticipant() {
        Jeu pandemic = jeux.save(new Jeu("Pandemic"));
        Joueur alex = joueurs.save(new Joueur("Alex"));
        Joueur marie = joueurs.save(new Joueur("Marie"));
        partie(pandemic, StatutPartie.TERMINEE, alex, true, marie, true);

        assertThat(statistiques.classement(pandemic.getId())).containsExactlyInAnyOrder(
                new ResultatJoueur(alex.getId(), 1L, 1L), new ResultatJoueur(marie.getId(), 1L, 1L));
    }

    @Test
    void retourneUnJoueurSansVictoireAvecZeroVictoire() {
        Jeu catan = jeux.save(new Jeu("Catan"));
        Joueur alex = joueurs.save(new Joueur("Alex"));
        partie(catan, StatutPartie.TERMINEE, alex, false);

        assertThat(statistiques.classement(catan.getId())).containsExactly(new ResultatJoueur(alex.getId(), 1L, 0L));
        assertThat(statistiques.victoires(List.of(catan.getId()))).isEmpty();
    }

    @Test
    void agregeLesVictoiresDePlusieursJeuxSansLesJoueursAnonymises() {
        Jeu catan = jeux.save(new Jeu("Catan"));
        Jeu azul = jeux.save(new Jeu("Azul"));
        Joueur alex = joueurs.save(new Joueur("Alex"));
        Joueur ancien = new Joueur("Paul");
        ancien.anonymiser();
        ancien = joueurs.save(ancien);
        partie(catan, StatutPartie.TERMINEE, alex, true, ancien, false);
        partie(catan, StatutPartie.TERMINEE, alex, true);
        partie(azul, StatutPartie.TERMINEE, ancien, true, alex, false);

        List<VictoiresJeuJoueur> victoires = statistiques.victoires(List.of(catan.getId(), azul.getId()));

        assertThat(victoires).containsExactly(new VictoiresJeuJoueur(catan.getId(), alex.getId(), 2L));
    }

    @Test
    void renvoieUneListeVidePourUnJeuSansPartie() {
        Jeu catan = jeux.save(new Jeu("Catan"));

        assertThat(statistiques.classement(catan.getId())).isEmpty();
        assertThat(statistiques.victoires(List.of(catan.getId()))).isEmpty();
    }

    @Test
    void limiteLesDernieresPartiesAuNombreDemande() {
        Jeu catan = jeux.save(new Jeu("Catan"));
        Joueur alex = joueurs.save(new Joueur("Alex"));
        for (int i = 0; i < 12; i++) {
            Partie partie = new Partie(catan, LocalDate.of(2026, 1, 1).plusDays(i));
            partie.getParticipations().add(new Participation(partie, alex));
            parties.save(partie);
        }

        var dernieres = parties.findByJeuIdAndStatutOrderByDatePartieDescIdDesc(catan.getId(), StatutPartie.TERMINEE,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(dernieres).hasSize(10);
        assertThat(dernieres.get(0).getDatePartie()).isEqualTo(LocalDate.of(2026, 1, 12));
        assertThat(dernieres.get(9).getDatePartie()).isEqualTo(LocalDate.of(2026, 1, 3));
    }
}
