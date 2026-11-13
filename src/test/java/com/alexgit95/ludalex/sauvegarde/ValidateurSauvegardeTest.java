package com.alexgit95.ludalex.sauvegarde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

import com.alexgit95.ludalex.images.ImagesProperties;
import com.alexgit95.ludalex.parties.Partie;

class ValidateurSauvegardeTest {

    @TempDir
    Path dossier;

    private static ValidateurSauvegarde validateur() {
        return validateur(DataSize.ofGigabytes(1), 10_000, 200, DataSize.ofMegabytes(2));
    }

    private static ValidateurSauvegarde validateur(DataSize decompresse, int entreesMax, int ratio, DataSize image) {
        return new ValidateurSauvegarde(
                new SauvegardeProperties(DataSize.ofMegabytes(512), decompresse, entreesMax, ratio, ""),
                new ImagesProperties("./inutile", image, 50_000_000, Duration.ofSeconds(5), 5));
    }

    private void refuse(ArchiveDeTest archive, String messagePartiel) {
        assertThatThrownBy(() -> validateur().valider(archive.ecrire(dossier)))
                .isInstanceOf(SauvegardeInvalideException.class).hasMessageContaining(messagePartiel);
    }

    @Test
    void accepteLArchiveExportee() {
        ValidateurSauvegarde.Resultat resultat = validateur().valider(ArchiveDeTest.valide().ecrire(dossier));

        assertThat(resultat.images()).containsExactly(ArchiveDeTest.IMAGE);
        assertThat(resultat.octetsImages()).isEqualTo(ArchiveDeTest.png().length);
        assertThat(resultat.manifeste().formatVersion()).isEqualTo(1);
        assertThat(resultat.manifeste().compteurs()).containsEntry("participations", 2L);
    }

    @Test
    void accepteUneArchiveSansImageNiPrets() {
        ArchiveDeTest archive = ArchiveDeTest.valide().sans("images/" + ArchiveDeTest.IMAGE)
                .modifier("donnees/joueurs.json", "{\"type\":\"UPLOAD\",\"reference\":\"" + ArchiveDeTest.IMAGE + "\"}",
                        "null")
                .avec("donnees/prets.json", "[]")
                .modifier("manifest.json", "\"prets\":1,\"images\":1", "\"prets\":0,\"images\":0");

        assertThat(validateur().valider(archive.ecrire(dossier)).images()).isEmpty();
    }

    @Test
    void accepteLeFormatV2AvecVariantes() {
        ArchiveDeTest archive = ArchiveDeTest.valide()
                .modifier("manifest.json", "\"formatVersion\":1", "\"formatVersion\":2")
                .modifier("donnees/jeux.json", "\"cooperatif\":false",
                        "\"cooperatif\":false,\"plusieursVariantes\":true")
                .modifier("donnees/parties.json", "\"termineeLe\":null",
                        "\"termineeLe\":null,\"variante\":\"Enquête 1\"");

        assertThat(validateur().valider(archive.ecrire(dossier)).manifeste().formatVersion()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = { "../../etc/cron.d/x", "donnees/../../x.json", "/etc/passwd", "images\\x.png",
            "C:/windows/x", "donnees//joueurs.json" })
    void refuseLesCheminsDangereux(String nom) {
        refuse(ArchiveDeTest.valide().avec(nom, "x"), "Chemin dangereux");
    }

    @ParameterizedTest
    @ValueSource(strings = { "autre.txt", "donnees/secret.json", "images/pas-un-uuid.png", "images/sous/dossier.png",
            "scripts/run.sh", "images/22222222-2222-4222-8222-222222222222.exe" })
    void refuseLesEntreesInattendues(String nom) {
        refuse(ArchiveDeTest.valide().avec(nom, "x"), "Entrée inattendue");
    }

    @Test
    void refuseUneArchiveTropVolumineuseUneFoisDecompressee() {
        ArchiveDeTest archive = ArchiveDeTest.valide();

        assertThatThrownBy(() -> validateur(DataSize.ofBytes(300), 10_000, 200, DataSize.ofMegabytes(2))
                .valider(archive.ecrire(dossier)))
                .isInstanceOf(SauvegardeInvalideException.class).hasMessageContaining("taille maximale");
    }

    @Test
    void refuseUneBombeDeDecompression() {
        byte[] espaces = new byte[8 * 1024 * 1024];
        Arrays.fill(espaces, (byte) ' ');
        ArchiveDeTest archive = ArchiveDeTest.valide().avec("donnees/prets.json", espaces);

        assertThatThrownBy(() -> validateur(DataSize.ofGigabytes(1), 10_000, 100, DataSize.ofMegabytes(2))
                .valider(archive.ecrire(dossier)))
                .isInstanceOf(SauvegardeInvalideException.class).hasMessageContaining("compression");
    }

    @Test
    void refuseTropDEntrees() {
        assertThatThrownBy(() -> validateur(DataSize.ofGigabytes(1), 3, 200, DataSize.ofMegabytes(2))
                .valider(ArchiveDeTest.valide().ecrire(dossier)))
                .isInstanceOf(SauvegardeInvalideException.class).hasMessageContaining("trop d'entrées");
    }

    @Test
    void refuseUneImageQuiNEstPasUneImage() {
        refuse(ArchiveDeTest.valide().avec("images/" + ArchiveDeTest.IMAGE, "#!/bin/sh\necho pirate"),
                "PNG, JPEG, GIF ou WebP");
    }

    @Test
    void refuseUneImageDontLExtensionNeCorrespondPasAuFormat() {
        String nom = "33333333-3333-4333-8333-333333333333.jpg";
        refuse(ArchiveDeTest.valide().avec("images/" + nom, ArchiveDeTest.png())
                .modifier("manifest.json", "\"images\":1", "\"images\":2"), "extension");
    }

    @Test
    void refuseUneImageDeTropGrandeTaille() {
        ArchiveDeTest archive = ArchiveDeTest.valide().avec("images/" + ArchiveDeTest.IMAGE,
                ArchiveDeTest.pngRemplie(3000));

        assertThatThrownBy(() -> validateur(DataSize.ofGigabytes(1), 10_000, 200, DataSize.ofKilobytes(1))
                .valider(archive.ecrire(dossier)))
                .isInstanceOf(SauvegardeInvalideException.class).hasMessageContaining("taille maximale");
    }

    @Test
    void refuseUneVersionDeFormatInconnue() {
        refuse(ArchiveDeTest.valide().modifier("manifest.json", "\"formatVersion\":1", "\"formatVersion\":3"),
            "versions acceptées : [1, 2]");
    }

    @Test
    void refuseUnManifesteAbsentOuIllisible() {
        refuse(ArchiveDeTest.valide().sans("manifest.json"), "Manifeste manquant");
        refuse(ArchiveDeTest.valide().avec("manifest.json", "pas du json"), "manifeste");
        refuse(ArchiveDeTest.valide().avec("manifest.json", "{\"appVersion\":\"x\"}"), "Version de format");
    }

    @Test
    void refuseUnFichierDeDonneesManquant() {
        refuse(ArchiveDeTest.valide().sans("donnees/equipes.json"), "donnees/equipes.json");
    }

    @Test
    void refuseUneImageReferenceeMaisAbsente() {
        refuse(ArchiveDeTest.valide().sans("images/" + ArchiveDeTest.IMAGE)
                .modifier("manifest.json", "\"images\":1", "\"images\":0"), "absente de la sauvegarde");
    }

    @Test
    void refuseDesCompteursQuiNeCorrespondentPasAuManifeste() {
        refuse(ArchiveDeTest.valide().modifier("manifest.json", "\"joueurs\":2", "\"joueurs\":3"), "manifeste");
        refuse(ArchiveDeTest.valide().modifier("manifest.json", "\"images\":1", "\"images\":0"), "manifeste");
    }

    @Test
    void refuseUneArchiveCorrompue() throws IOException {
        Path aleatoire = Files.write(dossier.resolve("bruit.zip"), new Random(1).ints(2000, 0, 256)
                .collect(java.io.ByteArrayOutputStream::new, (b, i) -> b.write(i), (a, b) -> { }).toByteArray());
        byte[] valide = ArchiveDeTest.valide().octets();
        Path tronquee = Files.write(dossier.resolve("tronquee.zip"), Arrays.copyOf(valide, valide.length / 2));

        assertThatThrownBy(() -> validateur().valider(aleatoire)).isInstanceOf(SauvegardeInvalideException.class)
                .hasMessageContaining("ZIP valide");
        assertThatThrownBy(() -> validateur().valider(tronquee)).isInstanceOf(SauvegardeInvalideException.class);
    }

    static Stream<Arguments> donneesIncoherentes() {
        return Stream.of(
                Arguments.of("donnees/participations.json", "\"joueurId\":9002", "\"joueurId\":9999", "joueur"),
                Arguments.of("donnees/participations.json", "\"partieId\":5001,\"joueurId\":9002",
                        "\"partieId\":5999,\"joueurId\":9002", "partie"),
                Arguments.of("donnees/participations.json", "\"joueurId\":9002", "\"joueurId\":9001",
                        "deux fois"),
                Arguments.of("donnees/participations.json", "\"idSauvegarde\":3002", "\"idSauvegarde\":3001",
                        "en double"),
                Arguments.of("donnees/participations.json", "\"equipeId\":4001", "\"equipeId\":4999", "équipe"),
                Arguments.of("donnees/equipes.json", "\"partieId\":5001", "\"partieId\":5999", "partie"),
                Arguments.of("donnees/parties.json", "\"jeuId\":7001", "\"jeuId\":7999", "jeu"),
                Arguments.of("donnees/parties.json", "\"joueurSuivantId\":null", "\"joueurSuivantId\":9999",
                        "joueur suivant"),
                Arguments.of("donnees/prets.json", "\"jeuId\":7001", "\"jeuId\":7999", "jeu"),
                Arguments.of("donnees/joueurs.json", "\"idSauvegarde\":9002", "\"idSauvegarde\":9001", "en double"),
                Arguments.of("donnees/joueurs.json", "\"nom\":\"Alice\"", "\"nom\":\"\"", "Nom de joueur"),
                Arguments.of("donnees/joueurs.json", "\"nom\":\"Alice\"", "\"nom\":\"" + "x".repeat(101) + "\"",
                        "Nom de joueur"));
    }

    @ParameterizedTest
    @MethodSource("donneesIncoherentes")
    void refuseDesReferencesEtDesDonneesIncoherentes(String fichier, String ancien, String nouveau, String message) {
        refuse(ArchiveDeTest.valide().modifier(fichier, ancien, nouveau), message);
    }

    static Stream<Arguments> valeursInvalides() {
        return Stream.of(
                Arguments.of("donnees/jeux.json", "#112233", "rouge", "Couleur"),
                Arguments.of("donnees/jeux.json", "\"39.90\"", "\"-1\"", "Prix"),
                Arguments.of("donnees/jeux.json", "\"39.90\"", "\"abc\"", "Prix"),
                Arguments.of("donnees/jeux.json", "2024-05-01", "pas une date", "Date d'achat"),
                Arguments.of("donnees/parties.json", "TERMINEE", "INCONNUE", "Statut"),
                Arguments.of("donnees/parties.json", "2025-01-02", "02/01/2025", "Date de partie"),
                Arguments.of("donnees/parties.json", "\"termineeLe\":null", "\"termineeLe\":\"hier\"", "Instant"),
                Arguments.of("donnees/prets.json", "\"emprunteur\":\"Voisin\"", "\"emprunteur\":\" \"", "Emprunteur"),
                Arguments.of("donnees/joueurs.json", "https://exemple.org/b.png", "javascript:alert(1)", "Adresse"),
                Arguments.of("donnees/joueurs.json", "https://exemple.org/b.png", "ftp://exemple.org/b.png",
                        "Adresse"),
                Arguments.of("donnees/joueurs.json", "\"type\":\"URL\"", "\"type\":\"AUTRE\"", "Type d'image"),
                Arguments.of("donnees/joueurs.json", ArchiveDeTest.IMAGE, "../../x.png", "Nom de fichier d'image"));
    }

    @ParameterizedTest
    @MethodSource("valeursInvalides")
    void refuseDesValeursInvalides(String fichier, String ancien, String nouveau, String message) {
        refuse(ArchiveDeTest.valide().modifier(fichier, ancien, nouveau), message);
    }

    @Test
    void refuseUneVarianteTropLongueDansUneArchiveV2() {
        ArchiveDeTest archive = ArchiveDeTest.valide()
                .modifier("manifest.json", "\"formatVersion\":1", "\"formatVersion\":2")
                .modifier("donnees/parties.json", "\"termineeLe\":null",
                        "\"termineeLe\":null,\"variante\":\"" + "x".repeat(Partie.VARIANTE_MAX + 1) + "\"");

        refuse(archive, "Variante de partie trop longue");
    }

    @Test
    void refuseUnJsonMalFormeOuQuiNEstPasUnTableau() {
        refuse(ArchiveDeTest.valide().avec("donnees/jeux.json", "[{\"idSauvegarde\":1,"), "illisible");
        refuse(ArchiveDeTest.valide().avec("donnees/jeux.json", "{\"a\":1}"), "illisible");
        refuse(ArchiveDeTest.valide().avec("donnees/jeux.json", "[1,2]"), "jeux.json");
        refuse(ArchiveDeTest.valide().avec("donnees/jeux.json", JEUX_AVEC_QUEUE), "illisible");
    }

    private static final String JEUX_AVEC_QUEUE = ArchiveDeTest.JEUX + " []";
}
