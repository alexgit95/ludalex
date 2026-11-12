package com.alexgit95.ludalex.importmyludo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NomsEtAppariementTest {

    @ParameterizedTest
    @CsvSource({ "Galèrapagos,galerapagos", "  CHÂTEAU   Combo ,chateau combo",
            "Ghost Fightin\u2019 Treasure Hunters,ghost fightin' treasure hunters", "Émma,emma", "Twin It!,twin it!" })
    void normaliseCasseAccentsEspacesEtApostrophes(String nom, String attendu) {
        assertThat(NomsNormalises.cle(nom)).isEqualTo(attendu);
    }

    @Test
    void uneValeurNulleDonneUneCleVide() {
        assertThat(NomsNormalises.cle(null)).isEmpty();
    }

    private static List<Set<Long>> sets(Set<Long>... valeurs) {
        return List.of(valeurs);
    }

    @Test
    void uneMancheIdentiqueEnBaseAbsorbeUneSeuleManche() {
        List<Set<Long>> fichier = sets(Set.of(1L, 2L), Set.of(1L, 2L), Set.of(1L, 2L));
        List<Set<Long>> base = sets(Set.of(1L, 2L));

        assertThat(Appariement.apparier(fichier, base)).containsExactly(true, false, false);
    }

    @Test
    void autantDeManchesEnBaseQueDansLeFichierNeLaisseRienAAjouter() {
        List<Set<Long>> fichier = sets(Set.of(1L, 2L), Set.of(1L, 2L));

        assertThat(Appariement.apparier(fichier, fichier)).containsExactly(true, true);
    }

    @Test
    void unJoueurEnCommunSuffitAFaireUnConflit() {
        List<Set<Long>> fichier = sets(Set.of(1L, 2L, 3L));
        List<Set<Long>> base = sets(Set.of(1L, 9L));

        assertThat(Appariement.apparier(fichier, base)).containsExactly(true);
    }

    @Test
    void sansJoueurEnCommunLaPartieEstAjoutee() {
        assertThat(Appariement.apparier(sets(Set.of(1L)), sets(Set.of(2L)))).containsExactly(false);
        assertThat(Appariement.apparier(sets(Set.of()), sets(Set.of(2L)))).containsExactly(false);
        assertThat(Appariement.apparier(sets(Set.of(1L)), List.of())).containsExactly(false);
    }

    @Test
    void laCorrespondanceExactePasseAvantLeRecouvrementPartiel() {
        List<Set<Long>> fichier = sets(Set.of(1L, 2L, 3L), Set.of(1L, 2L));
        List<Set<Long>> base = sets(Set.of(1L, 2L, 3L, 4L), Set.of(1L, 2L));

        // la partie {1,2} du fichier prend {1,2} en base ; {1,2,3} prend alors l'autre
        assertThat(Appariement.apparier(fichier, base)).containsExactly(true, true);
    }

    @Test
    void chaquePartieEnBaseNeServQuUneFois() {
        List<Set<Long>> fichier = sets(Set.of(1L, 2L), Set.of(1L, 3L));
        List<Set<Long>> base = sets(Set.of(1L));

        boolean[] absorbees = Appariement.apparier(fichier, base);

        assertThat(absorbees[0] ^ absorbees[1]).isTrue();
    }
}
