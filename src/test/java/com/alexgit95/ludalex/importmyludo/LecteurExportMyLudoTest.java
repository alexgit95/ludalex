package com.alexgit95.ludalex.importmyludo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.JeuMyLudo;
import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.PartieMyLudo;

class LecteurExportMyLudoTest {

    private final LecteurExportMyLudo lecteur = new LecteurExportMyLudo();

    static byte[] json(String texte) {
        return texte.getBytes(StandardCharsets.UTF_8);
    }

    private static final String JEU = "{\"Titre\":\"Ghost Fightin\\u0027 Treasure Hunters\",\"Prix indicatif\":34.5,"
            + "\"Date d\\u0027acquisition\":\"2023-06-24\",\"Mécanisme(s)\":[\"Coopération\",\"Jet De Dés\"],"
            + "\"Durée\":30,\"Note moyenne\":7.9}";
    private static final String PARTIE = "{\"Titre\":\"Toy Battle\",\"Date\":\"2025-08-08\",\"Lieu\":\"Lieu A\","
            + "\"Joueurs\":[{\"Nom\":\"Alice\",\"Score\":12,\"Victoire\":true,\"Équipe\":\"blue\"},"
            + "{\"Nom\":\"Bruno\",\"Score\":0,\"Victoire\":false}],\"Coop\":\"Non\",\"Coop Victoire\":\"Non\","
            + "\"Exclue\":\"Non\",\"Commentaire\":\"x\"}";

    @Test
    void litUnJeuDeLaCollection() {
        List<JeuMyLudo> jeux = lecteur.lireCollection(json("[" + JEU + "]"), "collection.json");

        assertThat(jeux).singleElement().satisfies(jeu -> {
            assertThat(jeu.entree()).isEqualTo(1);
            assertThat(jeu.titre()).isEqualTo("Ghost Fightin' Treasure Hunters");
            assertThat(jeu.prix()).isEqualByComparingTo(new BigDecimal("34.5"));
            assertThat(jeu.dateAcquisition()).isEqualTo(LocalDate.of(2023, 6, 24));
            assertThat(jeu.mecaniqueCooperative()).isTrue();
        });
    }

    @Test
    void unPrixNulOuAbsentEtUneDateVideSontVides() {
        List<JeuMyLudo> jeux = lecteur.lireCollection(json("[{\"Titre\":\"A\",\"Prix indicatif\":0,"
                + "\"Date d\\u0027acquisition\":\"\"},{\"Titre\":\"B\"}]"), "c.json");

        assertThat(jeux).allSatisfy(jeu -> {
            assertThat(jeu.prix()).isNull();
            assertThat(jeu.dateAcquisition()).isNull();
            assertThat(jeu.mecaniqueCooperative()).isFalse();
        });
    }

    @Test
    void litUnePartieAvecSesJoueursEtSesEquipes() {
        List<PartieMyLudo> parties = lecteur.lireParties(json("[" + PARTIE + "]"), "plays.json");

        assertThat(parties).singleElement().satisfies(partie -> {
            assertThat(partie.titre()).isEqualTo("Toy Battle");
            assertThat(partie.date()).isEqualTo(LocalDate.of(2025, 8, 8));
            assertThat(partie.lieu()).isEqualTo("Lieu A");
            assertThat(partie.coop()).isFalse();
            assertThat(partie.exclue()).isFalse();
            assertThat(partie.joueurs()).hasSize(2);
            assertThat(partie.joueurs().get(0).nom()).isEqualTo("Alice");
            assertThat(partie.joueurs().get(0).score()).isEqualTo(12);
            assertThat(partie.joueurs().get(0).victoire()).isTrue();
            assertThat(partie.joueurs().get(0).equipe()).isEqualTo("blue");
            assertThat(partie.joueurs().get(1).equipe()).isNull();
        });
    }

    @Test
    void accepteUnTableauVide() {
        assertThat(lecteur.lireCollection(json("[]"), "c.json")).isEmpty();
        assertThat(lecteur.lireParties(json(" [ ] "), "p.json")).isEmpty();
    }

    @Test
    void signaleLaPositionDUnJsonMalForme() {
        assertThatThrownBy(() -> lecteur.lireParties(json("[\n  {\"Titre\": \"A\",\n  \"Date\" \"2025\"}\n]"),
                "plays.json")).isInstanceOfSatisfying(ImportMyLudoException.class, e -> {
                    assertThat(e.getMessage()).contains("plays.json").contains("JSON mal formé").contains("ligne 3");
                    assertThat(e.details()).anyMatch(ligne -> ligne.startsWith("Position : ligne 3"));
                });
    }

    @Test
    void refuseUnFichierQuiNEstPasUnTableau() {
        assertThatThrownBy(() -> lecteur.lireCollection(json("{\"Titre\":\"A\"}"), "c.json"))
                .isInstanceOf(ImportMyLudoException.class).hasMessageContaining("tableau JSON");
        assertThatThrownBy(() -> lecteur.lireCollection(json("[1,2]"), "c.json"))
                .isInstanceOf(ImportMyLudoException.class).hasMessageContaining("entrée n°1")
                .hasMessageContaining("pas un objet");
        assertThatThrownBy(() -> lecteur.lireCollection(json(""), "c.json"))
                .isInstanceOf(ImportMyLudoException.class);
    }

    @Test
    void refuseUnFichierDansLaMauvaiseZone() {
        assertThatThrownBy(() -> lecteur.lireCollection(json("[" + PARTIE + "]"), "plays.json"))
                .isInstanceOf(ImportMyLudoException.class).hasMessageContaining("export de parties");
        assertThatThrownBy(() -> lecteur.lireParties(json("[" + JEU + "]"), "collection.json"))
                .isInstanceOf(ImportMyLudoException.class).hasMessageContaining("export de collection");
    }

    static java.util.stream.Stream<Arguments> entreesInvalides() {
        String joueur = "\"Joueurs\":[{\"Nom\":\"Alice\",\"Victoire\":true}]";
        return java.util.stream.Stream.of(
                Arguments.of("{\"Date\":\"2025-01-01\"," + joueur + "}", "entrée n°1", "Titre", "valeur absente"),
                Arguments.of("{\"Titre\":\"  \",\"Date\":\"2025-01-01\"," + joueur + "}", "entrée n°1", "Titre",
                        "valeur absente"),
                Arguments.of("{\"Titre\":\"" + "x".repeat(201) + "\",\"Date\":\"2025-01-01\"," + joueur + "}",
                        "entrée n°1", "Titre", "trop longue"),
                Arguments.of("{\"Titre\":\"A\"," + joueur + "}", "(A)", "Date", "date absente"),
                Arguments.of("{\"Titre\":\"A\",\"Date\":\"01/02/2025\"," + joueur + "}", "(A)", "Date",
                        "date illisible « 01/02/2025 »"),
                Arguments.of("{\"Titre\":\"A\",\"Date\":\"2025-01-01\"}", "(A, 2025-01-01)", "Joueurs",
                        "liste des joueurs absente"),
                Arguments.of("{\"Titre\":\"A\",\"Date\":\"2025-01-01\",\"Joueurs\":[{\"Victoire\":true}]}",
                        "(A, 2025-01-01)", "Nom", "valeur absente"),
                Arguments.of("{\"Titre\":\"A\",\"Date\":\"2025-01-01\",\"Joueurs\":[{\"Nom\":\"Alice\","
                        + "\"Score\":\"beaucoup\"}]}", "(A, 2025-01-01)", "Score", "nombre entier attendu"),
                Arguments.of("{\"Titre\":\"A\",\"Date\":\"2025-01-01\",\"Joueurs\":[{\"Nom\":\"Alice\","
                        + "\"Score\":1.5}]}", "(A, 2025-01-01)", "Score", "nombre entier attendu"));
    }

    @ParameterizedTest
    @MethodSource("entreesInvalides")
    void diagnostiqueLesEntreesInvalidesDesParties(String entree, String contexte, String champ, String cause) {
        assertThatThrownBy(() -> lecteur.lireParties(json("[" + entree + "]"), "plays.json"))
                .isInstanceOfSatisfying(ImportMyLudoException.class, e -> {
                    assertThat(e.getMessage()).contains("plays.json").contains("entrée n°1").contains(contexte)
                            .contains("champ « " + champ + " »").contains(cause);
                    assertThat(e.details()).contains("Fichier : plays.json", "Champ : " + champ);
                });
    }

    @Test
    void indiqueLeNumeroDeLEntreeFautive() {
        assertThatThrownBy(() -> lecteur.lireCollection(json("[{\"Titre\":\"A\"},{\"Titre\":\"B\"},"
                + "{\"Prix indicatif\":1}]"), "c.json")).hasMessageContaining("entrée n°3");
        assertThatThrownBy(() -> lecteur.lireCollection(json("[{\"Titre\":\"A\",\"Prix indicatif\":\"cher\"}]"),
                "c.json")).hasMessageContaining("Prix indicatif").hasMessageContaining("nombre attendu");
        assertThatThrownBy(() -> lecteur.lireCollection(json("[{\"Titre\":\"A\",\"Date d\\u0027acquisition\":\"hier\"}]"),
                "c.json")).hasMessageContaining("Date d'acquisition").hasMessageContaining("date illisible");
    }
}
