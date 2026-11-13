package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class CopiesImagesTest {
    @TempDir Path dossier;
    private final CopieImageRepository depot = mock(CopieImageRepository.class);
    private final ReferencesImages references = mock(ReferencesImages.class);
    private final TelechargeurImage telechargeur = mock(TelechargeurImage.class);
    private final CoordinationImages coordination = new CoordinationImages();
    private final CopieImage copie = new CopieImage("https://example.org/a.png");
    private final Instant maintenant = Instant.parse("2026-11-13T10:00:00Z");
    private CopiesImages service;
    private StockageImages stockage;

    @BeforeEach
    void preparer() {
        stockage = new StockageImages(ImagesDeTest.proprietes(dossier.toString()));
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        service = new CopiesImages(depot, references, stockage, telechargeur, coordination,
                Clock.fixed(maintenant, ZoneOffset.UTC), transactions);
        when(depot.findById(copie.getCle())).thenReturn(Optional.of(copie));
        when(depot.eligibles(eq(CopieImage.Etat.EN_ATTENTE), any(), any())).thenReturn(List.of(copie));
        when(references.existe(copie.getUrl(), TypeImage.URL)).thenReturn(true);
        when(depot.findByCleGreaterThanOrderByCle(anyString(), any())).thenReturn(List.of(copie));
    }

    @Test
    void telechargePublieEtExposeSansAccesReseauDansLaVue() throws Exception {
        doAnswer(appel -> {
            assertThat(coordination.verrou().isHeldByCurrentThread()).isFalse();
            Files.write(appel.getArgument(1, Path.class), ImagesDeTest.png(4, 4, Color.RED));
            return null;
        })
                .when(telechargeur).telecharger(eq(copie.getUrl()), any(Path.class));
        service.traiterUne();
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.DISPONIBLE);
        assertThat(stockage.charger(copie.getFichier())).isPresent();
        when(depot.findAllById(any())).thenReturn(List.of(copie));
        assertThat(service.disponibles(List.of(copie.getUrl()))).containsEntry(copie.getUrl(), "/images/" + copie.getFichier());
        verify(telechargeur, times(1)).telecharger(anyString(), any(Path.class));
    }

    @Test
    void borneLesErreursTransitoiresEtRefuseDefinitivementUnFauxFormat() throws Exception {
        doThrow(new ImageInvalideException("secret interne")).when(telechargeur).telecharger(anyString(), any(Path.class));
        service.traiterUne();
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.EN_ATTENTE);
        assertThat(copie.getProchaineTentative()).isEqualTo(maintenant.plusSeconds(60));
        assertThat(copie.getErreur()).doesNotContain("secret");
        doAnswer(appel -> { Files.writeString(appel.getArgument(1, Path.class), "<html>404</html>"); return null; })
                .when(telechargeur).telecharger(anyString(), any(Path.class));
        service.traiterUne();
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.ECHEC);
        try (var fichiers = Files.list(dossier.resolve(".copies"))) { assertThat(fichiers).isEmpty(); }
    }

    @Test
    void nePubliePasUneAncienneGenerationOuUneReferenceSupprimee() throws Exception {
        doAnswer(appel -> { coordination.invalider(); return null; }).when(telechargeur).telecharger(anyString(), any(Path.class));
        service.traiterUne();
        assertThat(copie.getFichier()).isNull();
        doNothing().when(telechargeur).telecharger(anyString(), any(Path.class));
        when(references.existe(copie.getUrl(), TypeImage.URL)).thenReturn(false);
        service.traiterUne();
        verify(depot).deleteById(copie.getCle());
        assertThat(copie.getFichier()).isNull();
    }

    @Test
    void reconcilieSansReinitialiserLesEchecsEtConserveLesReferencesPartagees() {
        copie.commencer(maintenant);
        copie.echouer(maintenant, true, "Refus");
        service.reconcilier();
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.ECHEC);
        verify(depot, never()).delete(any());
        service.relancerEchecs();
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.EN_ATTENTE);
        service.referencee(new CopiesImages.Referencee(copie.getUrl()));
        verify(depot, never()).save(any());
    }

    @Test
    void reprendUnTravailEtReplanifieUnFichierDisparu() {
        copie.commencer(maintenant);
        service.initialiser();
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.EN_ATTENTE);
        copie.disponible("00000000-0000-0000-0000-000000000000.png", 12);
        service.reconcilier();
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.EN_ATTENTE);
        when(references.existe(copie.getUrl(), TypeImage.URL)).thenReturn(false);
        service.reconcilier();
        verify(depot).delete(copie);
    }
}