package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class PlanificationCopiesTest {

    @TempDir
    Path dossier;

    @Test
    void demarreEnPauseParDefautEtSeLaisseActiverOuMettreEnPause() {
        EtatCopies etat = new EtatCopies(false, true);

        assertThat(etat.actif()).isFalse();
        etat.activer();
        etat.activer();
        assertThat(etat.actif()).isTrue();
        etat.mettreEnPause();
        etat.mettreEnPause();
        assertThat(etat.actif()).isFalse();
    }

    @Test
    void peutDemarrerActifParConfigurationEtSignaleLaDesactivationTotale() {
        assertThat(new EtatCopies(true, true).actif()).isTrue();
        assertThat(new EtatCopies(false, false).traitementConfigure()).isFalse();
        assertThat(new EtatCopies(false, true).traitementConfigure()).isTrue();
    }

    @Test
    void enPauseLePlanificateurNeTelechargeRienMaisContinueLInscription() {
        CopiesImages copies = mock(CopiesImages.class);
        PlanificationCopies planification = new PlanificationCopies(copies, new EtatCopies(false, true));

        planification.traiter();
        planification.traiter();
        planification.reconcilier();
        planification.initialiser();

        verify(copies, never()).traiterUne();
        verify(copies, times(1)).reconcilier();
        verify(copies, times(1)).initialiser();
    }

    @Test
    void activeLePlanificateurTraiteUneCopieParPassage() {
        CopiesImages copies = mock(CopiesImages.class);
        EtatCopies etat = new EtatCopies(false, true);
        PlanificationCopies planification = new PlanificationCopies(copies, etat);

        etat.activer();
        planification.traiter();
        planification.traiter();
        etat.mettreEnPause();
        planification.traiter();

        verify(copies, times(2)).traiterUne();
    }

    @Test
    void laPauseLaisseTerminerLeTelechargementEnCoursSansEnDemarrerUnAutre() throws Exception {
        CopiesImages copies = mock(CopiesImages.class);
        CountDownLatch demarre = new CountDownLatch(1);
        CountDownLatch libere = new CountDownLatch(1);
        doAnswer(appel -> {
            demarre.countDown();
            libere.await(5, TimeUnit.SECONDS);
            return null;
        }).when(copies).traiterUne();
        EtatCopies etat = new EtatCopies(true, true);
        PlanificationCopies planification = new PlanificationCopies(copies, etat);

        try (ExecutorService execution = Executors.newSingleThreadExecutor()) {
            Future<?> enCours = execution.submit(planification::traiter);
            assertThat(demarre.await(5, TimeUnit.SECONDS)).isTrue();
            etat.mettreEnPause();
            planification.traiter();
            assertThat(enCours.isDone()).isFalse();
            libere.countDown();
            enCours.get(5, TimeUnit.SECONDS);
        }

        verify(copies, times(1)).traiterUne();
    }

    @Test
    void enPauseUneCopieEnAttenteNeConsommeAucuneTentativeEtEstRepriseALActivation() throws Exception {
        Instant maintenant = Instant.parse("2026-11-13T10:00:00Z");
        CopieImageRepository depot = mock(CopieImageRepository.class);
        ReferencesImages references = mock(ReferencesImages.class);
        TelechargeurImage telechargeur = mock(TelechargeurImage.class);
        StockageImages stockage = new StockageImages(ImagesDeTest.proprietes(dossier.toString()));
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        CopiesImages service = new CopiesImages(depot, references, stockage, telechargeur, new CoordinationImages(),
                Clock.fixed(maintenant.plusSeconds(3600), ZoneOffset.UTC), transactions);
        CopieImage copie = new CopieImage("https://example.org/pause.png");
        copie.commencer(maintenant);
        copie.echouer(maintenant, false, "Indisponible");
        when(depot.findById(copie.getCle())).thenReturn(Optional.of(copie));
        when(depot.eligibles(eq(CopieImage.Etat.EN_ATTENTE), any(), any())).thenReturn(List.of(copie));
        when(references.existe(copie.getUrl(), TypeImage.URL)).thenReturn(true);
        doAnswer(appel -> {
            Files.write(appel.getArgument(1, Path.class), ImagesDeTest.png(4, 4, Color.RED));
            return null;
        }).when(telechargeur).telecharger(anyString(), any(Path.class));
        EtatCopies etat = new EtatCopies(false, true);
        PlanificationCopies planification = new PlanificationCopies(service, etat);

        planification.traiter();
        planification.traiter();

        verify(telechargeur, never()).telecharger(anyString(), any(Path.class));
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.EN_ATTENTE);
        assertThat(copie.getTentatives()).isEqualTo(1);

        etat.activer();
        planification.traiter();

        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.DISPONIBLE);
        verify(telechargeur, times(1)).telecharger(anyString(), any(Path.class));
    }
}
