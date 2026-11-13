package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.alexgit95.ludalex.statistiques.StatistiquesGlobalesRepository.CoutJeu;

/** Règles de calcul pures du tableau de bord : évolution, série de semaines, coût par partie. */
class StatistiquesGlobalesServiceTest {

    private static final LocalDate VENDREDI = LocalDate.of(2026, 11, 13);

    @Test
    void evolutionComparePeriodeCouranteEtPrecedente() {
        assertThat(StatistiquesGlobalesService.evolution(12, 8L)).startsWith("+50 %");
        assertThat(StatistiquesGlobalesService.evolution(4, 8L)).startsWith("-50 %");
        assertThat(StatistiquesGlobalesService.evolution(8, 8L)).startsWith("0 %");
    }

    @Test
    void evolutionSansPeriodePrecedenteOuSansPartiePrecedente() {
        assertThat(StatistiquesGlobalesService.evolution(3, null)).isEqualTo("tout l'historique");
        assertThat(StatistiquesGlobalesService.evolution(3, 0L)).contains("aucune partie");
        assertThat(StatistiquesGlobalesService.evolution(0, 0L)).contains("stable");
    }

    @Test
    void serieDeSemainesConsecutivesDepuisLaSemaineCourante() {
        Map<LocalDate, Long> parties = Map.of(VENDREDI.minusDays(3), 1L, VENDREDI.minusDays(10), 2L,
                VENDREDI.minusDays(15), 1L, VENDREDI.minusDays(40), 1L);

        assertThat(StatistiquesGlobalesService.serieSemaines(parties, VENDREDI)).isEqualTo(3);
    }

    @Test
    void laSerieNEstPasInterrompueParLaSemaineCouranteSansPartie() {
        Map<LocalDate, Long> parties = Map.of(VENDREDI.minusDays(10), 1L, VENDREDI.minusDays(17), 1L);

        assertThat(StatistiquesGlobalesService.serieSemaines(parties, VENDREDI)).isEqualTo(2);
    }

    @Test
    void laSerieEstNulleApresUneSemaineEntiereSansPartie() {
        assertThat(StatistiquesGlobalesService.serieSemaines(Map.of(VENDREDI.minusDays(30), 1L), VENDREDI)).isZero();
        assertThat(StatistiquesGlobalesService.serieSemaines(Map.of(), VENDREDI)).isZero();
    }

    @Test
    void coutParPartieClasseLesJeuxLesPlusRentabilisesEnPremier() {
        List<InstantaneStatistiques.Ligne> lignes = StatistiquesGlobalesService.coutParPartie(List.of(
                new CoutJeu("Rare", new BigDecimal("20.00"), 1), new CoutJeu("Catan", new BigDecimal("40.00"), 6),
                new CoutJeu("Pandemic", new BigDecimal("30.00"), 4)));

        assertThat(lignes).extracting(InstantaneStatistiques.Ligne::libelle).containsExactly("Catan", "Pandemic",
                "Rare");
        assertThat(lignes.get(0).valeur()).contains("6,67");
        assertThat(lignes.get(0).detail()).contains("6 parties");
        assertThat(lignes.get(2).detail()).contains("1 partie").doesNotContain("parties");
    }

    @Test
    void coutParPartieEstLimiteAuxDixMeilleurs() {
        List<CoutJeu> couts = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            couts.add(new CoutJeu("Jeu " + i, BigDecimal.valueOf(10 + i), 1));
        }

        assertThat(StatistiquesGlobalesService.coutParPartie(couts)).hasSize(10);
    }
}
