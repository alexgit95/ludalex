package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ServiceImagesTest {

    @TempDir
    Path volume;

    private TelechargeurImage telechargeur;
    private ServiceImages service;

    @BeforeEach
    void preparer() {
        ImagesProperties proprietes = ImagesDeTest.proprietes(volume.toString());
        telechargeur = mock(TelechargeurImage.class);
        service = new ServiceImages(new StockageImages(proprietes), new UrlSortanteValidateur(), telechargeur,
                new CalculateurCouleurFond(proprietes), proprietes);
    }

    @Test
    void uploadeUneImageEtCalculeSaCouleur() {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "jeu.png", "image/png",
                ImagesDeTest.png(50, 50, new Color(200, 30, 30)));

        ImagePreparee prepare = service.preparerUpload(fichier);

        assertThat(prepare.source().getType()).isEqualTo(TypeImage.UPLOAD);
        assertThat(Files.exists(volume.resolve(prepare.source().getReference()))).isTrue();
        assertThat(Integer.parseInt(prepare.couleurFond().substring(1, 3), 16)).isGreaterThan(150);
    }

    @Test
    void refuseUnFichierDePlusDeDeuxMegaOctets() throws IOException {
        byte[] trop = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(ImagesDeTest.png(2, 2, Color.RED), 0, trop, 0, 8);
        MockMultipartFile fichier = new MockMultipartFile("fichier", "gros.png", "image/png", trop);

        assertThatThrownBy(() -> service.preparerUpload(fichier)).isInstanceOf(ImageInvalideException.class)
                .hasMessageContaining("2 Mo");
        assertThat(Files.exists(volume)).isTrue();
        try (Stream<Path> fichiers = Files.list(volume)) {
            assertThat(fichiers).isEmpty();
        }
    }

    @Test
    void refuseUnContenuNonImageMemeAvecUneExtensionDImage() {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "faux.png", "image/png", "contenu texte".getBytes());

        assertThatThrownBy(() -> service.preparerUpload(fichier)).isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void refuseUnFichierVide() {
        assertThatThrownBy(() -> service.preparerUpload(new MockMultipartFile("f", new byte[0])))
                .isInstanceOf(ImageInvalideException.class);
        assertThatThrownBy(() -> service.preparerUpload(null)).isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void ignoreLeNomDeFichierFourniParLUtilisateur() throws IOException {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "../../evil.png", "image/png",
                ImagesDeTest.png(4, 4, Color.RED));

        ImagePreparee prepare = service.preparerUpload(fichier);

        assertThat(prepare.source().getReference()).matches("[0-9a-f-]{36}\\.png");
        try (Stream<Path> fichiers = Files.list(volume)) {
            assertThat(fichiers).hasSize(1);
        }
        assertThat(Files.exists(volume.getParent().resolve("evil.png"))).isFalse();
    }

    @Test
    void neConserveQueLUrlEtCalculeLaCouleurApresUnTelechargementUnique() throws IOException {
        when(telechargeur.telecharger(anyString())).thenReturn(ImagesDeTest.png(50, 50, new Color(30, 60, 200)));

        ImagePreparee prepare = service.preparerUrl("https://exemple.org/jeu.png");

        assertThat(prepare.source()).isEqualTo(ImageSource.url("https://exemple.org/jeu.png"));
        assertThat(Integer.parseInt(prepare.couleurFond().substring(5, 7), 16)).isGreaterThan(150);
        try (Stream<Path> fichiers = Files.list(volume)) {
            assertThat(fichiers).isEmpty();
        }
    }

    @Test
    void enregistreLUrlAvecLaCouleurNeutreSiLeTelechargementEchoue() {
        when(telechargeur.telecharger(anyString())).thenThrow(new ImageInvalideException("Image inaccessible"));

        ImagePreparee prepare = service.preparerUrl("https://exemple.org/jeu.png");

        assertThat(prepare.source().getType()).isEqualTo(TypeImage.URL);
        assertThat(prepare.couleurFond()).isEqualTo(CalculateurCouleurFond.NEUTRE);
    }

    @Test
    void refuseUneUrlAuSchemaInterdit() {
        assertThatThrownBy(() -> service.preparerUrl("file:///etc/passwd")).isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void utiliseLaCouleurSaisieManuellement() {
        assertThat(service.resoudreCouleur("#336699", "#aabbcc", null)).isEqualTo("#336699");
        assertThat(service.resoudreCouleur("#336699", "#aabbcc", new ImagePreparee(ImageSource.url("https://a.b/c"), "#112233")))
                .isEqualTo("#336699");
    }

    @Test
    void conserveLaCouleurExistanteSiLImageNeChangePas() {
        assertThat(service.resoudreCouleur("#aabbcc", "#aabbcc", null)).isEqualTo("#aabbcc");
        assertThat(service.resoudreCouleur("", "#aabbcc", null)).isEqualTo("#aabbcc");
        assertThat(service.resoudreCouleur(null, "#AABBCC", null)).isEqualTo("#AABBCC");
    }

    @Test
    void recalculeLaCouleurQuandLImageChangeSansCouleurSaisie() {
        ImagePreparee nouvelle = new ImagePreparee(ImageSource.url("https://a.b/c"), "#112233");

        assertThat(service.resoudreCouleur("#aabbcc", "#aabbcc", nouvelle)).isEqualTo("#112233");
        assertThat(service.resoudreCouleur(null, "#aabbcc", nouvelle)).isEqualTo("#112233");
    }

    @Test
    void utiliseLaCouleurNeutreSansCouleurNiImage() {
        assertThat(service.resoudreCouleur(null, null, null)).isEqualTo(CalculateurCouleurFond.NEUTRE);
    }

    @Test
    void refuseUneCouleurSaisieInvalide() {
        assertThatThrownBy(() -> service.resoudreCouleur("rouge", "#aabbcc", null))
                .isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void enregistreUnUploadSansCalculerLaCouleur() {
        MockMultipartFile fichier = new MockMultipartFile("f", "a.png", "image/png", ImagesDeTest.png(4, 4, Color.RED));

        ImageSource source = service.enregistrerUpload(fichier);

        assertThat(source.getType()).isEqualTo(TypeImage.UPLOAD);
        assertThat(Files.exists(volume.resolve(source.getReference()))).isTrue();
    }

    @Test
    void enregistreUneUrlSansLaTelechargerNiLaCopier() throws IOException {
        ImageSource source = service.enregistrerUrl("https://exemple.org/a.png");

        assertThat(source).isEqualTo(ImageSource.url("https://exemple.org/a.png"));
        org.mockito.Mockito.verifyNoInteractions(telechargeur);
        try (Stream<Path> fichiers = Files.list(volume)) {
            assertThat(fichiers).isEmpty();
        }
        assertThatThrownBy(() -> service.enregistrerUrl("ftp://exemple.org/a.png"))
                .isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void supprimeLAncienFichierLorsDuRemplacement() {
        ImageSource ancienne = service.preparerUpload(
                new MockMultipartFile("f", "a.png", "image/png", ImagesDeTest.png(4, 4, Color.RED))).source();
        ImageSource nouvelle = ImageSource.url("https://exemple.org/a.png");

        service.remplacer(ancienne, nouvelle);

        assertThat(Files.exists(volume.resolve(ancienne.getReference()))).isFalse();
    }

    @Test
    void neSupprimeRienSiLImageNEstPasRemplacee() {
        ImageSource image = service.preparerUpload(
                new MockMultipartFile("f", "a.png", "image/png", ImagesDeTest.png(4, 4, Color.RED))).source();

        service.remplacer(image, image);
        service.remplacer(null, image);

        assertThat(Files.exists(volume.resolve(image.getReference()))).isTrue();
    }
}
