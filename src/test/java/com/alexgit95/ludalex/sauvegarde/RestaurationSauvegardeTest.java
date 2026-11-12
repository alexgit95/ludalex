package com.alexgit95.ludalex.sauvegarde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import com.alexgit95.ludalex.images.ImagesProperties;
import com.alexgit95.ludalex.images.StockageImages;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.Manifeste;

class RestaurationSauvegardeTest {

    private static final String ORPHELINE = "99999999-9999-4999-8999-999999999999.png";

    @TempDir
    Path dossier;

    private Path volume;
    private Path temporaire;
    private ImportDonnees importDonnees;

    @BeforeEach
    void preparer() throws IOException {
        volume = Files.createDirectories(dossier.resolve("images"));
        temporaire = dossier.resolve("tmp");
        importDonnees = mock(ImportDonnees.class);
    }

    private RestaurationSauvegarde service(DataSize tailleMax) {
        ImagesProperties images = new ImagesProperties(volume.toString(), DataSize.ofMegabytes(2), 50_000_000,
                Duration.ofSeconds(5), 5);
        SauvegardeProperties proprietes = new SauvegardeProperties(tailleMax, DataSize.ofGigabytes(1), 10_000, 200,
                temporaire.toString());
        return new RestaurationSauvegarde(proprietes, new ValidateurSauvegarde(proprietes, images), importDonnees,
                new StockageImages(images));
    }

    private long fichiersTemporaires() throws IOException {
        if (!Files.exists(temporaire)) {
            return 0;
        }
        try (Stream<Path> fichiers = Files.list(temporaire)) {
            return fichiers.count();
        }
    }

    private List<String> contenuDuVolume() throws IOException {
        try (Stream<Path> fichiers = Files.list(volume)) {
            return fichiers.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void installeLesImagesSupprimeLesOrphellinesEtNettoieTout() throws IOException {
        Files.write(volume.resolve(ORPHELINE), ArchiveDeTest.png());
        Files.write(volume.resolve(ArchiveDeTest.IMAGE), new byte[] { 1, 2, 3 });
        doNothing().when(importDonnees).remplacer(any(ZipFile.class));

        Manifeste manifeste = service(DataSize.ofMegabytes(512))
                .restaurer(new ByteArrayInputStream(ArchiveDeTest.valide().octets()));

        assertThat(manifeste.compteurs()).containsEntry("joueurs", 2L);
        assertThat(contenuDuVolume()).containsExactly(ArchiveDeTest.IMAGE);
        assertThat(Files.readAllBytes(volume.resolve(ArchiveDeTest.IMAGE))).isEqualTo(ArchiveDeTest.png());
        assertThat(fichiersTemporaires()).isZero();
    }

    @Test
    void unEchecDImportLaisseLesImagesEtNettoieLesFichiersTemporaires() throws IOException {
        Files.write(volume.resolve(ORPHELINE), ArchiveDeTest.png());
        doThrow(new IllegalStateException("panne")).when(importDonnees).remplacer(any(ZipFile.class));

        assertThatThrownBy(() -> service(DataSize.ofMegabytes(512))
                .restaurer(new ByteArrayInputStream(ArchiveDeTest.valide().octets())))
                .isInstanceOf(IllegalStateException.class);

        assertThat(contenuDuVolume()).containsExactly(ORPHELINE);
        assertThat(fichiersTemporaires()).isZero();
    }

    @Test
    void uneArchiveInvalideNeToucheRienEtN_appelleJamaisLImport() throws IOException {
        Files.write(volume.resolve(ORPHELINE), ArchiveDeTest.png());

        assertThatThrownBy(() -> service(DataSize.ofMegabytes(512))
                .restaurer(new ByteArrayInputStream(ArchiveDeTest.valide().avec("evil.sh", "x").octets())))
                .isInstanceOf(SauvegardeInvalideException.class);

        verify(importDonnees, never()).remplacer(any());
        assertThat(contenuDuVolume()).containsExactly(ORPHELINE);
        assertThat(fichiersTemporaires()).isZero();
    }

    @Test
    void refuseUnFichierPlusGrosQueLaLimiteEtSupprimeLeTemporaire() throws IOException {
        byte[] gros = new byte[3 * 1024 * 1024];

        assertThatThrownBy(() -> service(DataSize.ofMegabytes(1)).restaurer(new ByteArrayInputStream(gros)))
                .isInstanceOf(SauvegardeTropGrosseException.class);

        assertThat(fichiersTemporaires()).isZero();
        verify(importDonnees, never()).remplacer(any());
    }

    @Test
    void uneSeuleRestaurationALaFois() throws Exception {
        CountDownLatch commence = new CountDownLatch(1);
        CountDownLatch libere = new CountDownLatch(1);
        doAnswer(appel -> {
            commence.countDown();
            libere.await(10, TimeUnit.SECONDS);
            return null;
        }).when(importDonnees).remplacer(any(ZipFile.class));
        RestaurationSauvegarde service = service(DataSize.ofMegabytes(512));
        ExecutorService executeur = Executors.newSingleThreadExecutor();
        try {
            Future<Manifeste> premiere = executeur
                    .submit(() -> service.restaurer(new ByteArrayInputStream(ArchiveDeTest.valide().octets())));
            assertThat(commence.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> service.restaurer(new ByteArrayInputStream(new byte[0])))
                    .isInstanceOf(RestaurationEnCoursException.class);

            libere.countDown();
            assertThat(premiere.get(10, TimeUnit.SECONDS)).isNotNull();
            service.restaurer(new ByteArrayInputStream(ArchiveDeTest.valide().octets()));
        } finally {
            libere.countDown();
            executeur.shutdownNow();
        }
    }

    @Test
    void libereLeVerrouApresUnEchec() {
        RestaurationSauvegarde service = service(DataSize.ofMegabytes(512));

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.restaurer(new ByteArrayInputStream(new byte[] { 1, 2, 3 })))
                    .isInstanceOf(SauvegardeInvalideException.class);
        }
    }

    @Test
    void ecartesLesRestesDePreparationAuDemarrage() throws IOException {
        Path reste = Files.createDirectories(volume.resolve(RestaurationSauvegarde.PREFIXE_PREPARATION + "abc"));
        Files.write(reste.resolve("x.png"), ArchiveDeTest.png());
        Files.write(volume.resolve(ORPHELINE), ArchiveDeTest.png());

        service(DataSize.ofMegabytes(512)).nettoyerRestes();

        assertThat(contenuDuVolume()).containsExactly(ORPHELINE);
    }

    @Test
    void neFaitRienSiLeVolumeNExistePas() throws IOException {
        Files.delete(volume);

        service(DataSize.ofMegabytes(512)).nettoyerRestes();

        assertThat(Files.exists(volume)).isFalse();
    }

    @Test
    void leFluxEstLuEnEntierEtLaTailleMaxExposee() throws IOException {
        InputStream flux = new ByteArrayInputStream(ArchiveDeTest.valide().octets());
        doNothing().when(importDonnees).remplacer(any(ZipFile.class));
        RestaurationSauvegarde service = service(DataSize.ofMegabytes(512));

        service.restaurer(flux);

        assertThat(flux.available()).isZero();
        assertThat(service.tailleMax()).isEqualTo(512L * 1024 * 1024);
    }
}
