package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Anneau;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Barre;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Calendrier;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Courbe;

class DiagrammesTest {

    @Test
    void barresSontRelativesAuMaximumEtResteVisibles() {
        List<Barre> barres = Diagrammes.barres(List.of("a", "b", "c", "d"), List.of(200L, 100L, 1L, 0L));

        assertThat(barres).extracting(Barre::hauteur).containsExactly(100, 50, 3, 0);
        assertThat(barres).extracting(Barre::libelle).containsExactly("a", "b", "c", "d");
    }

    @Test
    void barresSansValeurNonNulleRestentVides() {
        assertThat(Diagrammes.barres(List.of("a"), List.of(0L))).extracting(Barre::hauteur).containsExactly(0);
        assertThat(Diagrammes.barres(List.of(), List.of())).isEmpty();
    }

    @Test
    void anneauRepartitLaCirconferenceEtDecaleChaqueTranche() {
        Anneau anneau = Diagrammes.anneau(List.of("A", "B", "C"), List.of(50L, 25L, 25L));

        assertThat(anneau.total()).isEqualTo(100);
        assertThat(anneau.tranches()).extracting(t -> t.longueur()).containsExactly(50.0, 25.0, 25.0);
        assertThat(anneau.tranches()).extracting(t -> t.decalage()).containsExactly(25.0, -25.0, -50.0);
        assertThat(anneau.tranches()).extracting(t -> t.pourcent()).containsExactly(50, 25, 25);
        assertThat(anneau.tranches()).extracting(t -> t.couleur()).containsExactly(0, 1, 2);
    }

    @Test
    void anneauVideSansValeur() {
        assertThat(Diagrammes.anneau(List.of("A"), List.of(0L)).vide()).isTrue();
        assertThat(Diagrammes.anneau(List.of("A"), List.of(0L)).tranches()).isEmpty();
    }

    @Test
    void anneauReutiliseLesTeintesAuDela() {
        Anneau anneau = Diagrammes.anneau(List.of("1", "2", "3", "4", "5", "6", "7"),
                List.of(1L, 1L, 1L, 1L, 1L, 1L, 1L));

        assertThat(anneau.tranches().get(6).couleur()).isEqualTo(0);
    }

    @Test
    void courbeCumuleeCommenceEnBasEtFinitEnHaut() {
        Courbe courbe = Diagrammes.courbe(List.of(0L, 5L, 10L));

        assertThat(courbe.points()).isEqualTo("0.0,29.0 50.0,15.0 100.0,1.0");
        assertThat(courbe.total()).isEqualTo(10);
        assertThat(courbe.vide()).isFalse();
    }

    @Test
    void courbeVideSansAssezDePointsOuDeValeur() {
        assertThat(Diagrammes.courbe(List.of(4L)).vide()).isTrue();
        assertThat(Diagrammes.courbe(List.of(0L, 0L)).vide()).isTrue();
        assertThat(Diagrammes.courbe(List.of()).vide()).isTrue();
    }

    @Test
    void niveauxDuCalendrier() {
        assertThat(List.of(0L, 1L, 2L, 3L, 4L, 5L, 9L).stream().map(Diagrammes::niveau).toList())
                .containsExactly(0, 1, 2, 3, 3, 4, 4);
    }

    @Test
    void calendrierPlaceChaqueJourSurSaSemaineEtSaLigne() {
        LocalDate lundi = LocalDate.of(2026, 11, 2);
        Calendrier calendrier = Diagrammes.calendrier(Map.of(lundi, 1L, lundi.plusDays(9), 3L), lundi,
                lundi.plusDays(13));

        assertThat(calendrier.colonnes()).isEqualTo(2);
        assertThat(calendrier.total()).isEqualTo(4);
        assertThat(calendrier.cellules()).hasSize(14);
        assertThat(calendrier.cellules().get(0)).satisfies(c -> {
            assertThat(c.colonne()).isZero();
            assertThat(c.ligne()).isZero();
            assertThat(c.niveau()).isEqualTo(1);
            assertThat(c.titre()).isEqualTo("02/11/2026 : 1 partie");
        });
        assertThat(calendrier.cellules().get(9)).satisfies(c -> {
            assertThat(c.colonne()).isEqualTo(1);
            assertThat(c.ligne()).isEqualTo(2);
            assertThat(c.niveau()).isEqualTo(3);
            assertThat(c.titre()).isEqualTo("11/11/2026 : 3 parties");
        });
    }

    @Test
    void calendrierSeLimiteACinquanteTroisSemaines() {
        LocalDate fin = LocalDate.of(2026, 11, 13);
        Calendrier calendrier = Diagrammes.calendrier(Map.of(), LocalDate.of(2000, 1, 1), fin);

        assertThat(calendrier.colonnes()).isEqualTo(Diagrammes.SEMAINES_MAX);
        assertThat(calendrier.vide()).isTrue();
    }

    @Test
    void calendrierCommenceAuDebutDeLaPeriodeSansColonneVide() {
        LocalDate debut = LocalDate.of(2026, 11, 11);
        Calendrier calendrier = Diagrammes.calendrier(Map.of(), debut, LocalDate.of(2026, 11, 13));

        assertThat(calendrier.cellules()).hasSize(3);
        assertThat(calendrier.cellules().get(0).colonne()).isZero();
        assertThat(calendrier.cellules().get(0).ligne()).isEqualTo(2);
    }
}
