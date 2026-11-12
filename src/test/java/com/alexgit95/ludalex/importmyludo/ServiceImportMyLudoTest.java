package com.alexgit95.ludalex.importmyludo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alexgit95.ludalex.joueurs.JoueurRepository;

class ServiceImportMyLudoTest {

    private static final byte[] PARTIES = ("[{\"Titre\":\"Jeu A\",\"Date\":\"2025-01-01\",\"Joueurs\":"
            + "[{\"Nom\":\"Alice\",\"Victoire\":true}]}]").getBytes(StandardCharsets.UTF_8);

    private static final class HorlogeMobile extends Clock {
        private Instant instant = Instant.parse("2026-11-12T10:00:00Z");

        void avancer(Duration duree) {
            instant = instant.plus(duree);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private AnalyseurImport analyseur;
    private ImportTransactionnel importTransactionnel;
    private HorlogeMobile horloge;
    private ServiceImportMyLudo service;
    private final PlanImport planVide = new PlanImport(List.of(), List.of(), List.of(), 0, 0, Map.of(), List.of(),
            List.of(), 0);

    @BeforeEach
    void preparer() {
        analyseur = mock(AnalyseurImport.class);
        importTransactionnel = mock(ImportTransactionnel.class);
        JoueurRepository joueurs = mock(JoueurRepository.class);
        when(joueurs.findByAnonymiseFalseOrderByNomNormaliseAsc()).thenReturn(List.of());
        horloge = new HorlogeMobile();
        service = new ServiceImportMyLudo(new LecteurExportMyLudo(), analyseur, importTransactionnel, joueurs,
                new ImportMyLudoProperties(Duration.ofMinutes(30), 200), horloge);
        when(analyseur.analyser(any(), any(), any(), any())).thenReturn(planVide);
    }

    @AfterEach
    void arreter() {
        service.arreter();
    }

    private ServiceImportMyLudo.Vue attendre(Predicate<ServiceImportMyLudo.Vue> condition) throws Exception {
        long fin = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < fin) {
            ServiceImportMyLudo.Vue vue = service.vue();
            if (vue != null && condition.test(vue)) {
                return vue;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Délai dépassé : " + service.vue());
    }

    @Test
    void refuseUneSecondeOperationPendantUneAnalyse() throws Exception {
        CountDownLatch commence = new CountDownLatch(1);
        CountDownLatch libere = new CountDownLatch(1);
        when(analyseur.analyser(any(), any(), any(), any())).thenAnswer(appel -> {
            commence.countDown();
            libere.await(10, TimeUnit.SECONDS);
            return planVide;
        });
        service.demarrerAnalyse(null, null, PARTIES, "p.json");
        assertThat(commence.await(10, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> service.demarrerAnalyse(null, null, PARTIES, "p.json"))
                .isInstanceOf(ImportEnCoursException.class).hasMessageContaining("déjà en cours");
        service.abandonner();
        assertThat(service.vue()).isNotNull().satisfies(vue -> assertThat(vue.enCours()).isTrue());
        assertThatThrownBy(() -> service.demarrerImport(Map.of())).isInstanceOf(ImportMyLudoException.class);

        libere.countDown();
        attendre(vue -> vue.etat() == ServiceImportMyLudo.Etat.ANALYSE_PRETE);
        service.demarrerAnalyse(null, null, PARTIES, "p.json");
    }

    @Test
    void refuseUnSecondImportPendantUnImport() throws Exception {
        CountDownLatch commence = new CountDownLatch(1);
        CountDownLatch libere = new CountDownLatch(1);
        when(importTransactionnel.executer(any(), any(), any(), any())).thenAnswer(appel -> {
            commence.countDown();
            libere.await(10, TimeUnit.SECONDS);
            return null;
        });
        service.demarrerAnalyse(null, null, PARTIES, "p.json");
        attendre(vue -> vue.etat() == ServiceImportMyLudo.Etat.ANALYSE_PRETE);
        service.demarrerImport(Map.of());
        assertThat(commence.await(10, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> service.demarrerImport(Map.of())).isInstanceOf(ImportMyLudoException.class)
                .hasMessageContaining("Aucune analyse prête");
        assertThatThrownBy(() -> service.demarrerAnalyse(null, null, PARTIES, "p.json"))
                .isInstanceOf(ImportEnCoursException.class);

        libere.countDown();
        attendre(vue -> !vue.enCours());
    }

    @Test
    void uneErreurInattendueDonneUnMessageGeneriqueSansDetail() throws Exception {
        when(analyseur.analyser(any(), any(), any(), any())).thenThrow(new IllegalStateException("secret SQL"));

        service.demarrerAnalyse(null, null, PARTIES, "p.json");
        ServiceImportMyLudo.Vue vue = attendre(v -> v.etat() == ServiceImportMyLudo.Etat.ERREUR);

        assertThat(vue.erreur()).contains("inattendue").contains("rien n'a été enregistré")
                .doesNotContain("secret");
        assertThat(vue.detailsErreur()).isEmpty();
    }

    @Test
    void uneErreurDImportEstRapporteeAvecSonDiagnostic() throws Exception {
        when(importTransactionnel.executer(any(), any(), any(), any()))
                .thenThrow(ImportMyLudoException.simple("Un joueur nommé « Emma » existe déjà"));
        service.demarrerAnalyse(null, null, PARTIES, "p.json");
        attendre(vue -> vue.etat() == ServiceImportMyLudo.Etat.ANALYSE_PRETE);

        service.demarrerImport(Map.of());
        ServiceImportMyLudo.Vue vue = attendre(v -> v.etat() == ServiceImportMyLudo.Etat.ERREUR);

        assertThat(vue.erreur()).contains("Emma").contains("existe déjà");
        assertThat(vue.detailsErreur()).containsExactly("Cause : Un joueur nommé « Emma » existe déjà");
    }

    @Test
    void oubliLAnalyseApresLaDureeDeConservation() throws Exception {
        service.demarrerAnalyse(null, null, PARTIES, "p.json");
        attendre(vue -> vue.etat() == ServiceImportMyLudo.Etat.ANALYSE_PRETE);

        horloge.avancer(Duration.ofMinutes(29));
        assertThat(service.vue()).isNotNull();
        horloge.avancer(Duration.ofMinutes(2));

        assertThat(service.vue()).isNull();
    }

    @Test
    void laSignatureChangeAvecLAvancement() {
        Progression progression = new Progression();
        progression.demarrer(Progression.Phase.ANALYSE, 10);
        String debut = progression.signature();

        progression.avancer("Toy Battle, 2025-01-01");

        assertThat(progression.signature()).isNotEqualTo(debut);
        assertThat(progression.fait()).isEqualTo(1);
        assertThat(progression.total()).isEqualTo(10);
        assertThat(progression.courant()).isEqualTo("Toy Battle, 2025-01-01");
        assertThat(Progression.Phase.IMPORT.libelle()).isEqualTo("Import");
    }

    @Test
    void lesDetailsDUneErreurNommentLeFichierLEntreeEtLaCause() {
        ImportMyLudoException erreur = new ImportMyLudoException("plays.json", 42, "Zodianimo, 2026-10-04", "Date",
                "date illisible");

        assertThat(erreur.getMessage()).isEqualTo(
                "plays.json, entrée n°42 (Zodianimo, 2026-10-04), champ « Date » : date illisible");
        assertThat(erreur.details()).containsExactly("Fichier : plays.json", "Entrée n°42",
                "Concerne : Zodianimo, 2026-10-04", "Champ : Date", "Cause : date illisible");
        assertThat(ImportMyLudoException.simple("oups").getMessage()).isEqualTo("oups");
    }
}
