package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class StockageImagesTest {

    @TempDir
    Path volume;

    private StockageImages stockage;

    @BeforeEach
    void preparer() {
        stockage = new StockageImages(ImagesDeTest.proprietes(volume.toString()));
    }

    @AfterEach
    void nettoyer() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void enregistreUneImageSousUnNomGenere() throws IOException {
        ImageSource source = stockage.enregistrer(ImagesDeTest.png(4, 4, Color.RED));

        assertThat(source.getType()).isEqualTo(TypeImage.UPLOAD);
        assertThat(source.getReference()).matches("[0-9a-f-]{36}\\.png");
        assertThat(source.getUrl()).isEqualTo("/images/" + source.getReference());
        assertThat(Files.exists(volume.resolve(source.getReference()))).isTrue();
    }

    @Test
    void refuseUnContenuQuiNEstPasUneImage() throws IOException {
        assertThatThrownBy(() -> stockage.enregistrer("<script>alert(1)</script>".getBytes()))
                .isInstanceOf(ImageInvalideException.class);

        try (Stream<Path> fichiers = Files.list(volume)) {
            assertThat(fichiers).isEmpty();
        }
    }

    @Test
    void chargeUnFichierDuVolume() {
        ImageSource source = stockage.enregistrer(ImagesDeTest.png(4, 4, Color.RED));

        assertThat(stockage.charger(source.getReference())).isPresent();
    }

    @Test
    void neChargeJamaisUnFichierHorsDuVolume() throws IOException {
        Files.writeString(volume.getParent().resolve("secret.png"), "secret");

        assertThat(stockage.charger("../secret.png")).isEmpty();
        assertThat(stockage.charger("..\\secret.png")).isEmpty();
        assertThat(stockage.charger("secret.png")).isEmpty();
        assertThat(stockage.charger("/etc/passwd")).isEmpty();
        assertThat(stockage.charger(null)).isEmpty();
    }

    @Test
    void neChargePasUnFichierInexistant() {
        assertThat(stockage.charger("00000000-0000-0000-0000-000000000000.png")).isEmpty();
    }

    @Test
    void supprimeLeFichierUploadeMaisPasUneUrl() {
        ImageSource source = stockage.enregistrer(ImagesDeTest.png(4, 4, Color.RED));

        stockage.supprimer(ImageSource.url("https://exemple.org/a.png"));
        assertThat(Files.exists(volume.resolve(source.getReference()))).isTrue();

        stockage.supprimer(source);
        assertThat(Files.exists(volume.resolve(source.getReference()))).isFalse();
    }

    @Test
    void ignoreUnNomDeFichierSuspectALaSuppression() throws IOException {
        Path voisin = Files.writeString(volume.getParent().resolve("voisin.png"), "x");

        stockage.supprimer(ImageSource.upload("../voisin.png"));

        assertThat(Files.exists(voisin)).isTrue();
    }

    @Test
    void supprimeApresLeCommitSeulement() {
        ImageSource source = stockage.enregistrer(ImagesDeTest.png(4, 4, Color.RED));
        TransactionSynchronizationManager.initSynchronization();

        stockage.supprimerApresCommit(source);
        assertThat(Files.exists(volume.resolve(source.getReference()))).isTrue();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(Files.exists(volume.resolve(source.getReference()))).isFalse();
    }

    @Test
    void supprimeLeNouveauFichierSiLaTransactionEstAnnulee() {
        ImageSource source = stockage.enregistrer(ImagesDeTest.png(4, 4, Color.RED));
        TransactionSynchronizationManager.initSynchronization();
        stockage.supprimerSiAnnulation(source);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(Files.exists(volume.resolve(source.getReference()))).isFalse();
    }

    @Test
    void conserveLeNouveauFichierSiLaTransactionEstValidee() {
        ImageSource source = stockage.enregistrer(ImagesDeTest.png(4, 4, Color.RED));
        TransactionSynchronizationManager.initSynchronization();
        stockage.supprimerSiAnnulation(source);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        assertThat(Files.exists(volume.resolve(source.getReference()))).isTrue();
    }
}
