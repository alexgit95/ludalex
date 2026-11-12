package com.alexgit95.ludalex.affichage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.DetailPartie;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieEnCoursService;
import com.alexgit95.ludalex.parties.PartieRepository;
import com.alexgit95.ludalex.parties.PartieService;
import com.alexgit95.ludalex.parties.ResultatCooperatif;
import com.alexgit95.ludalex.parties.StatutPartie;
import com.alexgit95.ludalex.parties.VuePartie;

class AffichageServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-12T10:00:00Z");
    private static final String ORIGINE = "https://ludalex.example.org";

    private PartieEnCoursService enCours;
    private PartieService parties;
    private PartieRepository repertoire;
    private QrFinPartie qr;
    private CodeQr codeQr;
    private DiaporamaImages diaporama;
    private AffichageService service;

    @BeforeEach
    void preparer() {
        enCours = mock(PartieEnCoursService.class);
        parties = mock(PartieService.class);
        repertoire = mock(PartieRepository.class);
        qr = mock(QrFinPartie.class);
        codeQr = mock(CodeQr.class);
        diaporama = mock(DiaporamaImages.class);
        service = new AffichageService(enCours, parties, repertoire, qr, codeQr, diaporama,
                new AffichageProperties(Duration.ofSeconds(15), Duration.ofHours(24), ""),
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        when(enCours.active()).thenReturn(Optional.empty());
        when(repertoire.terminesDepuis(any(), any())).thenReturn(List.of());
        when(diaporama.urls()).thenReturn(List.of("/diaporama/a.svg", "/diaporama/b.svg"));
        when(codeQr.svg(anyString())).thenReturn("<svg>qr</svg>");
    }

    private static Joueur joueur(long id, String nom) {
        Joueur joueur = new Joueur(nom);
        ReflectionTestUtils.setField(joueur, "id", id);
        return joueur;
    }

    private static Partie partie(long id, StatutPartie statut) {
        Partie partie = new Partie(new Jeu("Catan"), LocalDate.of(2026, 11, 12));
        ReflectionTestUtils.setField(partie, "id", id);
        partie.setStatut(statut);
        return partie;
    }

    private static VuePartie vue(Partie partie, Joueur suivant, Joueur... joueurs) {
        List<Participation> participations = java.util.Arrays.stream(joueurs)
                .map(j -> new Participation(partie, j)).toList();
        return new VuePartie(partie, partie.getJeu(), participations, List.of(), suivant);
    }

    private Partie termineeIlYA(long id, Duration age) {
        Partie partie = partie(id, StatutPartie.TERMINEE);
        partie.setTermineeLe(MAINTENANT.minus(age));
        return partie;
    }

    @Test
    void montreLeDiaporamaEnVeille() {
        EtatAffichage etat = service.etat(ORIGINE, false);

        assertThat(etat.type()).isEqualTo(EtatAffichage.Type.VEILLE);
        assertThat(etat.images()).containsExactly("/diaporama/a.svg", "/diaporama/b.svg");
        assertThat(etat.signature()).isEqualTo("VEILLE");
        assertThat(etat.vue()).isNull();
        assertThat(etat.fond()).isNull();
    }

    @Test
    void cherchePartiesTermineesDepuisQuinzeSecondes() {
        service.etat(ORIGINE, false);

        ArgumentCaptor<Instant> depuis = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repertoire).terminesDepuis(depuis.capture(), page.capture());
        assertThat(depuis.getValue()).isEqualTo(MAINTENANT.minusSeconds(15));
        assertThat(page.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void montreLaPartieEnCoursAvecUnQrCodeVersLeLienDeFin() {
        Partie partie = partie(5L, StatutPartie.EN_COURS);
        when(enCours.active()).thenReturn(Optional.of(vue(partie, null, joueur(1, "Anne"), joueur(2, "Marc"))));
        when(qr.obtenir(partie)).thenReturn("CLAIR");

        EtatAffichage etat = service.etat(ORIGINE, false);

        assertThat(etat.type()).isEqualTo(EtatAffichage.Type.EN_COURS);
        assertThat(etat.qrSvg()).isEqualTo("<svg>qr</svg>");
        assertThat(etat.vue().participations()).hasSize(2);
        assertThat(etat.images()).isEmpty();
        verify(codeQr).svg("https://ludalex.example.org/fin/CLAIR");
        verify(qr, never()).regenerer(any());
        verify(repertoire, never()).terminesDepuis(any(), any());
    }

    @Test
    void regenereLeTokenALOuvertureDeLaPage() {
        Partie partie = partie(5L, StatutPartie.EN_COURS);
        when(enCours.active()).thenReturn(Optional.of(vue(partie, null, joueur(1, "Anne"))));
        when(qr.regenerer(partie)).thenReturn("NEUF");

        service.etat(ORIGINE, true);

        verify(codeQr).svg("https://ludalex.example.org/fin/NEUF");
        verify(qr, never()).obtenir(any());
    }

    @Test
    void neMontrePasDeQrCodeEnPauseEtDesigneLeJoueurSuivant() {
        Partie partie = partie(5L, StatutPartie.EN_PAUSE);
        Joueur marc = joueur(2, "Marc");
        when(enCours.active()).thenReturn(Optional.of(vue(partie, marc, joueur(1, "Anne"), marc)));

        EtatAffichage etat = service.etat(ORIGINE, false);

        assertThat(etat.type()).isEqualTo(EtatAffichage.Type.EN_PAUSE);
        assertThat(etat.qrSvg()).isNull();
        assertThat(etat.vue().joueurSuivant()).isSameAs(marc);
        assertThat(etat.signature()).isEqualTo("EN_PAUSE:5:2");
        verify(qr, never()).obtenir(any());
        verify(qr, never()).regenerer(any());
        verify(codeQr, never()).svg(anyString());
    }

    @Test
    void gereUnePauseSansJoueurSuivant() {
        Partie partie = partie(5L, StatutPartie.EN_PAUSE);
        when(enCours.active()).thenReturn(Optional.of(vue(partie, null, joueur(1, "Anne"))));

        assertThat(service.etat(ORIGINE, false).signature()).isEqualTo("EN_PAUSE:5:null");
    }

    @Test
    void celebreLesVainqueursUneFoisLaPartieTerminee() {
        Partie terminee = termineeIlYA(9L, Duration.ofSeconds(5));
        when(repertoire.terminesDepuis(any(), any())).thenReturn(List.of(terminee));
        Joueur anne = joueur(1, "Anne");
        Joueur marc = joueur(2, "Marc");
        Participation gagnante = new Participation(terminee, anne);
        gagnante.setGagnant(true);
        when(parties.detail(9L)).thenReturn(new DetailPartie(terminee,
                List.of(gagnante, new Participation(terminee, marc)), List.of()));

        EtatAffichage etat = service.etat(ORIGINE, false);

        assertThat(etat.type()).isEqualTo(EtatAffichage.Type.VICTOIRE);
        assertThat(etat.vainqueurs()).containsExactly(anne);
        assertThat(etat.defaite()).isFalse();
        assertThat(etat.signature()).isEqualTo("VICTOIRE:9:" + terminee.getTermineeLe().toEpochMilli());
    }

    @Test
    void annonceUneDefaiteCooperative() {
        Partie terminee = termineeIlYA(9L, Duration.ofSeconds(2));
        terminee.setResultatCooperatif(ResultatCooperatif.DEFAITE);
        when(repertoire.terminesDepuis(any(), any())).thenReturn(List.of(terminee));
        when(parties.detail(9L)).thenReturn(new DetailPartie(terminee,
                List.of(new Participation(terminee, joueur(1, "Anne"))), List.of()));

        EtatAffichage etat = service.etat(ORIGINE, false);

        assertThat(etat.type()).isEqualTo(EtatAffichage.Type.VICTOIRE);
        assertThat(etat.defaite()).isTrue();
        assertThat(etat.vainqueurs()).isEmpty();
    }

    @Test
    void laPartieActivePrendLePasSurLaVictoireRecente() {
        Partie active = partie(5L, StatutPartie.EN_COURS);
        when(enCours.active()).thenReturn(Optional.of(vue(active, null, joueur(1, "Anne"))));
        when(qr.obtenir(active)).thenReturn("CLAIR");

        assertThat(service.etat(ORIGINE, false).type()).isEqualTo(EtatAffichage.Type.EN_COURS);
        verify(repertoire, never()).terminesDepuis(any(), any());
    }

    @Test
    void lesSignaturesSontStablesEtDistinctesSelonLEtat() {
        Partie partie = partie(5L, StatutPartie.EN_COURS);
        when(enCours.active()).thenReturn(Optional.of(vue(partie, null, joueur(1, "Anne"))));
        when(qr.obtenir(partie)).thenReturn("CLAIR");
        String premiere = service.etat(ORIGINE, false).signature();
        String seconde = service.etat(ORIGINE, false).signature();
        when(qr.obtenir(partie)).thenReturn("AUTRE");
        String apresRenouvellement = service.etat(ORIGINE, false).signature();
        when(enCours.active()).thenReturn(Optional.empty());
        String veille = service.etat(ORIGINE, false).signature();

        assertThat(premiere).isEqualTo(seconde).isNotEqualTo(apresRenouvellement).isNotEqualTo(veille);
        assertThat(apresRenouvellement).isNotEqualTo(veille);
        assertThat(premiere).startsWith("EN_COURS:5:").doesNotContain("CLAIR");
    }
}
