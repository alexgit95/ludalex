package com.alexgit95.ludalex.reglages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ReglagesServiceTest {

    private final ReglageRepository depot = mock(ReglageRepository.class);
    private final ReglagesService service = new ReglagesService(depot);

    @Test
    void utiliseLesValeursParDefautSansReglageEnregistre() {
        when(depot.findAll()).thenReturn(List.of());
        when(depot.findByCle(any())).thenReturn(Optional.empty());

        assertThat(service.valeur(ReglageCle.JEU_DORMANT_MOIS)).isEqualTo(6);
        assertThat(service.valeur(ReglageCle.CLASSEMENT_PARTIES_MIN)).isEqualTo(5);
        assertThat(service.valeur(ReglageCle.PRET_LONG_JOURS)).isEqualTo(30);
        assertThat(service.tous()).containsEntry(ReglageCle.JEU_DORMANT_MOIS, 6)
                .containsEntry(ReglageCle.CLASSEMENT_PARTIES_MIN, 5).containsEntry(ReglageCle.PRET_LONG_JOURS, 30);
    }

    @Test
    void litLaValeurEnregistreeEtIgnoreLesClesInconnuesOuCorrompues() {
        when(depot.findAll()).thenReturn(List.of(new Reglage("statistiques.jeu-dormant-mois", "12"),
                new Reglage("statistiques.pret-long-jours", "abc"), new Reglage("inconnue", "3")));

        Map<ReglageCle, Integer> valeurs = service.tous();

        assertThat(valeurs.get(ReglageCle.JEU_DORMANT_MOIS)).isEqualTo(12);
        assertThat(valeurs.get(ReglageCle.PRET_LONG_JOURS)).isEqualTo(30);
    }

    @Test
    void creeEtModifieUnReglage() {
        Reglage existant = new Reglage("statistiques.jeu-dormant-mois", "6");
        when(depot.findByCle("statistiques.jeu-dormant-mois")).thenReturn(Optional.of(existant));
        when(depot.findByCle("statistiques.pret-long-jours")).thenReturn(Optional.empty());

        service.enregistrer(Map.of(ReglageCle.JEU_DORMANT_MOIS, " 12 ", ReglageCle.PRET_LONG_JOURS, "45"));

        assertThat(existant.getValeur()).isEqualTo("12");
        verify(depot).save(any(Reglage.class));
    }

    @Test
    void refuseLesValeursInvalidesSansRienEcrire() {
        Map<ReglageCle, String> saisies = Map.of(ReglageCle.JEU_DORMANT_MOIS, "0",
                ReglageCle.CLASSEMENT_PARTIES_MIN, "-3", ReglageCle.PRET_LONG_JOURS, "4000");

        assertThatThrownBy(() -> service.enregistrer(saisies)).isInstanceOfSatisfying(ReglageInvalideException.class,
                e -> assertThat(e.erreurs()).containsOnlyKeys(ReglageCle.JEU_DORMANT_MOIS,
                        ReglageCle.CLASSEMENT_PARTIES_MIN, ReglageCle.PRET_LONG_JOURS));
        verify(depot, never()).save(any());
    }

    @Test
    void refuseUneValeurVideOuNonNumerique() {
        assertThat(ReglageCle.JEU_DORMANT_MOIS.analyser(null)).isEmpty();
        assertThat(ReglageCle.JEU_DORMANT_MOIS.analyser("")).isEmpty();
        assertThat(ReglageCle.JEU_DORMANT_MOIS.analyser("6,5")).isEmpty();
        assertThat(ReglageCle.JEU_DORMANT_MOIS.analyser("999999999999")).isEmpty();
        assertThat(ReglageCle.JEU_DORMANT_MOIS.analyser("120")).contains(120);
        assertThat(ReglageCle.JEU_DORMANT_MOIS.analyser("121")).isEmpty();
    }

    @Test
    void retabliSupprimeLaValeurEnregistree() {
        Reglage existant = new Reglage("statistiques.jeu-dormant-mois", "12");
        when(depot.findByCle("statistiques.jeu-dormant-mois")).thenReturn(Optional.of(existant));

        service.retablir(ReglageCle.JEU_DORMANT_MOIS);

        verify(depot).delete(existant);
    }

    @Test
    void retrouveUneCleDuCatalogue() {
        assertThat(ReglageCle.depuisCle("statistiques.pret-long-jours")).contains(ReglageCle.PRET_LONG_JOURS);
        assertThat(ReglageCle.depuisCle("autre")).isEmpty();
    }
}
