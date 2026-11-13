package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.alexgit95.ludalex.statistiques.PeriodeStatistiques.Fenetre;

class PeriodeStatistiquesTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 13);

    @Test
    void lePeriodeParDefautEstLesDouzeDerniersMois() {
        assertThat(PeriodeStatistiques.depuisCode(null)).isEqualTo(PeriodeStatistiques.MOIS_12);
        assertThat(PeriodeStatistiques.depuisCode("")).isEqualTo(PeriodeStatistiques.MOIS_12);
        assertThat(PeriodeStatistiques.depuisCode("n'importe quoi")).isEqualTo(PeriodeStatistiques.MOIS_12);
    }

    @Test
    void reconnaitLesQuatrePeriodes() {
        assertThat(PeriodeStatistiques.depuisCode("30j")).isEqualTo(PeriodeStatistiques.JOURS_30);
        assertThat(PeriodeStatistiques.depuisCode("12m")).isEqualTo(PeriodeStatistiques.MOIS_12);
        assertThat(PeriodeStatistiques.depuisCode("annee")).isEqualTo(PeriodeStatistiques.ANNEE);
        assertThat(PeriodeStatistiques.depuisCode("tout")).isEqualTo(PeriodeStatistiques.TOUT);
    }

    @Test
    void trenteJoursInclutAujourdhuiEtComparePeriodePrecedente() {
        Fenetre fenetre = PeriodeStatistiques.JOURS_30.fenetre(AUJOURDHUI);

        assertThat(fenetre.debut()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(fenetre.fin()).isEqualTo(AUJOURDHUI);
        assertThat(fenetre.precedente().debut()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(fenetre.precedente().fin()).isEqualTo(LocalDate.of(2026, 10, 14));
    }

    @Test
    void douzeMoisCouvrentDouzeMoisCompletsSansRecouvrement() {
        Fenetre fenetre = PeriodeStatistiques.MOIS_12.fenetre(AUJOURDHUI);

        assertThat(fenetre.debut()).isEqualTo(LocalDate.of(2025, 11, 14));
        assertThat(fenetre.precedente().debut()).isEqualTo(LocalDate.of(2024, 11, 14));
        assertThat(fenetre.precedente().fin()).isEqualTo(LocalDate.of(2025, 11, 13));
    }

    @Test
    void anneeCivileCommenceLePremierJanvierEtComparePremierePartieDeLAnneePrecedente() {
        Fenetre fenetre = PeriodeStatistiques.ANNEE.fenetre(AUJOURDHUI);

        assertThat(fenetre.debut()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(fenetre.fin()).isEqualTo(AUJOURDHUI);
        assertThat(fenetre.precedente().debut()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(fenetre.precedente().fin()).isEqualTo(LocalDate.of(2025, 11, 13));
    }

    @Test
    void toutLHistoriqueNAPasDeDebutNiDeComparaison() {
        Fenetre fenetre = PeriodeStatistiques.TOUT.fenetre(AUJOURDHUI);

        assertThat(fenetre.debut()).isNull();
        assertThat(fenetre.fin()).isEqualTo(AUJOURDHUI);
        assertThat(fenetre.precedente()).isNull();
    }
}
