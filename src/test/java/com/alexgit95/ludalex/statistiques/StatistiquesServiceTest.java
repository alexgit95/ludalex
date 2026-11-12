package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurRepository;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieRepository;
import com.alexgit95.ludalex.parties.StatutPartie;

class StatistiquesServiceTest {

    private StatistiqueRepository statistiques;
    private JoueurRepository joueurs;
    private PartieRepository parties;
    private StatistiquesService service;
    private Joueur alex;
    private Joueur marie;
    private Joueur paul;

    @BeforeEach
    void preparer() {
        statistiques = mock(StatistiqueRepository.class);
        joueurs = mock(JoueurRepository.class);
        parties = mock(PartieRepository.class);
        service = new StatistiquesService(statistiques, joueurs, parties);
        alex = joueur(1, "Alex");
        marie = joueur(2, "Marie");
        paul = joueur(3, "Paul");
        when(joueurs.findAllById(anyCollection())).thenAnswer(appel -> {
            List<Joueur> trouves = new ArrayList<>();
            for (Object id : (Iterable<?>) appel.getArgument(0)) {
                for (Joueur joueur : List.of(alex, marie, paul)) {
                    if (joueur.getId().equals(id)) {
                        trouves.add(joueur);
                    }
                }
            }
            return trouves;
        });
        when(parties.findByJeuIdAndStatutOrderByDatePartieDescIdDesc(any(), any(), any())).thenReturn(List.of());
    }

    private static Joueur joueur(long id, String nom) {
        Joueur joueur = new Joueur(nom);
        ReflectionTestUtils.setField(joueur, "id", id);
        return joueur;
    }

    private static ResultatJoueur resultat(Joueur joueur, long parties, long victoires) {
        return new ResultatJoueur(joueur.getId(), parties, victoires);
    }

    @Test
    void classeLesJoueursParVictoiresPuisTauxPuisNom() {
        when(statistiques.classement(1L)).thenReturn(List.of(
                resultat(marie, 8, 3), resultat(alex, 8, 5), resultat(paul, 4, 3)));

        var classement = service.pourJeu(1L).classement();

        assertThat(classement).extracting(ligne -> ligne.joueur().getNom()).containsExactly("Alex", "Paul", "Marie");
    }

    @Test
    void departageLesEgalitesParLeNom() {
        when(statistiques.classement(1L)).thenReturn(List.of(resultat(paul, 4, 2), resultat(alex, 4, 2)));

        assertThat(service.pourJeu(1L).classement()).extracting(ligne -> ligne.joueur().getNom())
                .containsExactly("Alex", "Paul");
    }

    @Test
    void calculeLeTauxDeVictoireArrondi() {
        assertThat(new LigneClassement(alex, 4, 3).tauxPourcent()).isEqualTo(75);
        assertThat(new LigneClassement(alex, 3, 2).tauxPourcent()).isEqualTo(67);
        assertThat(new LigneClassement(alex, 8, 5).tauxPourcent()).isEqualTo(63);
        assertThat(new LigneClassement(alex, 5, 0).tauxPourcent()).isZero();
        assertThat(new LigneClassement(alex, 0, 0).tauxPourcent()).isZero();
    }

    @Test
    void designeUnChampionUnique() {
        when(statistiques.classement(1L)).thenReturn(List.of(resultat(alex, 8, 5), resultat(marie, 8, 3)));

        assertThat(service.pourJeu(1L).champions()).containsExactly(alex);
    }

    @Test
    void designeTousLesJoueursALegalite() {
        when(statistiques.classement(1L)).thenReturn(List.of(
                resultat(alex, 8, 4), resultat(marie, 6, 4), resultat(paul, 9, 2)));

        assertThat(service.pourJeu(1L).champions()).containsExactlyInAnyOrder(alex, marie);
    }

    @Test
    void neDesigneAucunChampionSansVictoire() {
        when(statistiques.classement(1L)).thenReturn(List.of(resultat(alex, 3, 0), resultat(marie, 2, 0)));

        StatistiquesJeu stats = service.pourJeu(1L);

        assertThat(stats.champions()).isEmpty();
        assertThat(stats.classement()).hasSize(2);
    }

    @Test
    void ecarteLesJoueursAnonymisesDuTitreDeChampion() {
        paul.anonymiser();
        when(statistiques.classement(1L)).thenReturn(List.of(
                resultat(paul, 8, 6), resultat(alex, 8, 4), resultat(marie, 8, 1)));

        StatistiquesJeu stats = service.pourJeu(1L);

        assertThat(stats.champions()).containsExactly(alex);
        assertThat(stats.classement()).hasSize(3);
    }

    @Test
    void nAffichePasDeStatistiquesPourUnJeuSansPartie() {
        when(statistiques.classement(1L)).thenReturn(List.of());

        StatistiquesJeu stats = service.pourJeu(1L);

        assertThat(stats.vide()).isTrue();
        assertThat(stats.champions()).isEmpty();
        assertThat(stats.dernieresParties()).isEmpty();
        verify(parties, never()).participationsDesParties(any());
    }

    @Test
    void chargeLesDixDernieresPartiesTermineesAvecLeursParticipants() {
        Jeu jeu = new Jeu("Catan");
        Partie recente = new Partie(jeu, LocalDate.of(2026, 5, 2));
        ReflectionTestUtils.setField(recente, "id", 7L);
        Participation participation = new Participation(recente, alex);
        participation.setGagnant(true);
        recente.getParticipations().add(participation);
        when(statistiques.classement(1L)).thenReturn(List.of(resultat(alex, 1, 1)));
        when(parties.findByJeuIdAndStatutOrderByDatePartieDescIdDesc(eq(1L), eq(StatutPartie.TERMINEE), any()))
                .thenReturn(List.of(recente));
        when(parties.participationsDesParties(anyCollection())).thenReturn(List.of(participation));

        var dernieres = service.pourJeu(1L).dernieresParties();

        assertThat(dernieres).hasSize(1);
        assertThat(dernieres.get(0).participations()).containsExactly(participation);
        ArgumentCaptor<Pageable> limite = ArgumentCaptor.forClass(Pageable.class);
        verify(parties).findByJeuIdAndStatutOrderByDatePartieDescIdDesc(eq(1L), eq(StatutPartie.TERMINEE),
                limite.capture());
        assertThat(limite.getValue().getPageSize()).isEqualTo(StatistiquesService.DERNIERES_PARTIES).isEqualTo(10);
    }

    @Test
    void calculeLesChampionsDePlusieursJeuxEnUneSeuleLectureDeVictoires() {
        when(statistiques.victoires(anyCollection())).thenReturn(List.of(
                new VictoiresJeuJoueur(1L, 1L, 5L), new VictoiresJeuJoueur(1L, 2L, 3L),
                new VictoiresJeuJoueur(2L, 2L, 4L), new VictoiresJeuJoueur(2L, 3L, 4L),
                new VictoiresJeuJoueur(2L, 1L, 1L)));

        Map<Long, List<Joueur>> champions = service.pour(List.of(1L, 2L, 3L));

        assertThat(champions.get(1L)).containsExactly(alex);
        assertThat(champions.get(2L)).containsExactly(marie, paul);
        assertThat(champions).doesNotContainKey(3L);
        verify(statistiques).victoires(List.of(1L, 2L, 3L));
        verify(joueurs).findAllById(anyCollection());
    }

    @Test
    void neDesignePasDeChampionSansVictoireNiSansJeu() {
        when(statistiques.victoires(anyCollection())).thenReturn(List.of());

        assertThat(service.pour(List.of(1L))).isEmpty();
        assertThat(service.pour(List.of())).isEmpty();
        assertThat(service.pour(null)).isEmpty();
        verify(statistiques, org.mockito.Mockito.times(1)).victoires(anyCollection());
    }
}
