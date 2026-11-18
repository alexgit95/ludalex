package com.alexgit95.ludalex.affichage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieRepository;
import com.alexgit95.ludalex.parties.StatutPartie;
import com.alexgit95.ludalex.statistiques.StatistiqueRepository;

class HistoriqueAffichageTest {

    private final PartieRepository parties = mock(PartieRepository.class);
    private final StatistiqueRepository statistiques = mock(StatistiqueRepository.class);
    private final HistoriqueAffichage service = new HistoriqueAffichage(parties, statistiques);

    private Joueur joueur(long id, String nom) {
        Joueur joueur = new Joueur(nom);
        ReflectionTestUtils.setField(joueur, "id", id);
        return joueur;
    }

    @Test
    void chargeLesHistoriquesEnLotEtLesRangsAvecEgalites() {
        Partie partie = new Partie(new Jeu("Catan"), LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(partie, "id", 7L);
        Joueur anne = joueur(1, "Anne");
        Joueur marc = joueur(2, "Marc");
        Joueur lea = joueur(3, "Lea");
        Participation participation = new Participation(partie, anne);
        when(parties.findByJeuIdAndStatutOrderByDatePartieDescIdDesc(eq(42L), eq(StatutPartie.TERMINEE), any()))
                .thenReturn(List.of(partie));
        when(parties.findByStatutOrderByDatePartieDescIdDesc(eq(StatutPartie.TERMINEE), any()))
                .thenReturn(List.of(partie));
        when(parties.participationsDesParties(anyCollection())).thenReturn(List.of(participation));
        when(statistiques.scoresPodium(eq(42L), any())).thenReturn(List.of(2L, 0L));
        when(statistiques.joueursPodium(eq(42L), eq(2L), any())).thenReturn(List.of(anne, marc));
        when(statistiques.nombreJoueursPodium(42L, 2L)).thenReturn(5L);
        when(statistiques.joueursPodium(eq(42L), eq(0L), any())).thenReturn(List.of(lea));
        when(statistiques.nombreJoueursPodium(42L, 0L)).thenReturn(1L);

        HistoriqueAffichage.Donnees donnees = service.pourJeu(42L);

        assertThat(donnees.jeu()).singleElement().satisfies(ligne ->
                assertThat(ligne.participations()).containsExactly(participation));
        assertThat(donnees.global()).hasSize(1);
        assertThat(donnees.podium()).hasSize(2);
        assertThat(donnees.podium().get(0).joueurs()).containsExactly(anne, marc);
        assertThat(donnees.podium().get(0).autres()).isEqualTo(3);
        assertThat(donnees.podium().get(1).victoires()).isZero();
        assertThat(donnees.podium().get(1).joueurs()).containsExactly(lea);
        verify(parties).participationsDesParties(List.of(7L));
        ArgumentCaptor<Pageable> limite = ArgumentCaptor.forClass(Pageable.class);
        verify(parties).findByStatutOrderByDatePartieDescIdDesc(eq(StatutPartie.TERMINEE), limite.capture());
        assertThat(limite.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void neChargePasDeParticipationsOuDeJoueursQuandToutEstVide() {
        HistoriqueAffichage.Donnees donnees = service.pourJeu(42L);

        assertThat(donnees.jeu()).isEmpty();
        assertThat(donnees.global()).isEmpty();
        assertThat(donnees.podium()).isEmpty();
        verify(parties, never()).participationsDesParties(anyCollection());
        verify(statistiques, never()).joueursPodium(any(), any(Long.class), any());
    }

    @Test
    void chargeLHistoriqueGlobalEnVeilleSansCalculerLePodium() {
        Partie partie = new Partie(new Jeu("Catan"), LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(partie, "id", 7L);
        Participation participation = new Participation(partie, joueur(1, "Anne"));
        when(parties.findByStatutOrderByDatePartieDescIdDesc(eq(StatutPartie.TERMINEE), any()))
                .thenReturn(List.of(partie));
        when(parties.participationsDesParties(List.of(7L))).thenReturn(List.of(participation));

        HistoriqueAffichage.Donnees donnees = service.global();

        assertThat(donnees.global()).singleElement().satisfies(ligne ->
                assertThat(ligne.participations()).containsExactly(participation));
        assertThat(donnees.jeu()).isEmpty();
        assertThat(donnees.podium()).isEmpty();
        verify(parties, never()).findByJeuIdAndStatutOrderByDatePartieDescIdDesc(any(), any(), any());
        verify(statistiques, never()).scoresPodium(any(), any());
    }
}