package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuRepository;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurRepository;

class PartieServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 12);

    private PartieRepository parties;
    private JeuRepository jeux;
    private JoueurRepository joueurs;
    private PartieService service;
    private Jeu catan;
    private Jeu pandemic;
    private final Map<Long, Joueur> annuaire = new HashMap<>();

    @BeforeEach
    void preparer() {
        parties = mock(PartieRepository.class);
        jeux = mock(JeuRepository.class);
        joueurs = mock(JoueurRepository.class);
        when(parties.save(any(Partie.class))).thenAnswer(appel -> appel.getArgument(0));
        catan = jeu(1, "Catan", false);
        pandemic = jeu(2, "Pandemic", true);
        when(jeux.findById(1L)).thenReturn(Optional.of(catan));
        when(jeux.findById(2L)).thenReturn(Optional.of(pandemic));
        for (long id = 10; id < 14; id++) {
            annuaire.put(id, joueur(id, "Joueur" + id));
        }
        when(joueurs.findAllById(anyCollection())).thenAnswer(appel -> {
            List<Joueur> trouves = new ArrayList<>();
            for (Object id : (Iterable<?>) appel.getArgument(0)) {
                if (annuaire.containsKey(id)) {
                    trouves.add(annuaire.get(id));
                }
            }
            return trouves;
        });
        service = new PartieService(parties, jeux, joueurs, mock(TokenFinPartieRepository.class), HORLOGE);
    }

    private static Jeu jeu(long id, String nom, boolean cooperatif) {
        Jeu jeu = new Jeu(nom);
        jeu.setCooperatif(cooperatif);
        ReflectionTestUtils.setField(jeu, "id", id);
        return jeu;
    }

    private static Joueur joueur(long id, String nom) {
        Joueur joueur = new Joueur(nom);
        ReflectionTestUtils.setField(joueur, "id", id);
        return joueur;
    }

    private static PartieSaisie competitive(List<Long> joueurIds, List<Long> gagnants) {
        return new PartieSaisie(1L, null, null, joueurIds, null, gagnants, null, null, null, null);
    }

    private static Participation de(Partie partie, long joueurId) {
        return partie.getParticipations().stream()
                .filter(participation -> participation.getJoueur().getId() == joueurId).findFirst().orElseThrow();
    }

    @Test
    void declareUnePartieCompetitiveAvecScores() {
        Map<Long, Integer> scores = new LinkedHashMap<>();
        scores.put(10L, 8);
        scores.put(11L, 10);
        scores.put(12L, 6);

        Partie partie = service.declarer(new PartieSaisie(1L, LocalDate.of(2026, 5, 2), " Maison de Marie ",
                List.of(10L, 11L, 12L), scores, List.of(11L), null, null, null, null));

        assertThat(partie.getJeu()).isSameAs(catan);
        assertThat(partie.getStatut()).isEqualTo(StatutPartie.TERMINEE);
        assertThat(partie.getLieu()).isEqualTo("Maison de Marie");
        assertThat(partie.getParticipations()).hasSize(3);
        assertThat(de(partie, 11).isGagnant()).isTrue();
        assertThat(de(partie, 11).getScore()).isEqualTo(10);
        assertThat(de(partie, 10).isGagnant()).isFalse();
        assertThat(partie.getResultatCooperatif()).isNull();
    }

    @Test
    void utiliseLaDateDuJourParDefautEtRefuseUneDateFuture() {
        assertThat(service.declarer(competitive(List.of(10L), List.of(10L))).getDatePartie()).isEqualTo(AUJOURDHUI);

        PartieSaisie future = new PartieSaisie(1L, AUJOURDHUI.plusDays(1), null, List.of(10L), null, List.of(10L),
                null, null, null, null);
        assertThatThrownBy(() -> service.declarer(future))
                .isInstanceOfSatisfying(PartieInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("date"));
        assertThat(service.declarer(new PartieSaisie(1L, AUJOURDHUI, null, List.of(10L), null, List.of(10L), null, null,
                null, null)).getDatePartie()).isEqualTo(AUJOURDHUI);
    }

    @Test
    void refuseUnJeuManquantOuInconnu() {
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(null, null, null, List.of(10L), null, List.of(10L),
                null, null, null, null))).isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(99L, null, null, List.of(10L), null, List.of(10L),
                null, null, null, null))).isInstanceOf(PartieInvalideException.class);
    }

    @Test
    void refuseUnePartieSansJoueur() {
        assertThatThrownBy(() -> service.declarer(competitive(List.of(), List.of())))
                .isInstanceOf(PartieInvalideException.class).hasMessageContaining("au moins un joueur");
        assertThatThrownBy(() -> service.declarer(competitive(null, null))).isInstanceOf(PartieInvalideException.class);
    }

    @Test
    void refuseUnePartieCompetitiveSansGagnant() {
        assertThatThrownBy(() -> service.declarer(competitive(List.of(10L, 11L), List.of())))
                .isInstanceOfSatisfying(PartieInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("gagnants"));
        assertThatThrownBy(() -> service.declarer(competitive(List.of(10L, 11L), null)))
                .isInstanceOf(PartieInvalideException.class);
        verify(parties, never()).save(any());
    }

    @Test
    void accepteSeveralGagnants() {
        Partie partie = service.declarer(competitive(List.of(10L, 11L, 12L), List.of(10L, 12L)));

        assertThat(de(partie, 10).isGagnant()).isTrue();
        assertThat(de(partie, 11).isGagnant()).isFalse();
        assertThat(de(partie, 12).isGagnant()).isTrue();
    }

    @Test
    void refuseUnGagnantHorsParticipants() {
        assertThatThrownBy(() -> service.declarer(competitive(List.of(10L, 11L), List.of(12L))))
                .isInstanceOf(PartieInvalideException.class).hasMessageContaining("ne participe pas");
    }

    @Test
    void refuseUnJoueurEnDoubleInconnuOuAnonymise() {
        assertThatThrownBy(() -> service.declarer(competitive(List.of(10L, 10L), List.of(10L))))
                .isInstanceOf(PartieInvalideException.class).hasMessageContaining("qu'une fois");
        assertThatThrownBy(() -> service.declarer(competitive(List.of(10L, 99L), List.of(10L))))
                .isInstanceOf(PartieInvalideException.class);
        annuaire.get(11L).anonymiser();
        assertThatThrownBy(() -> service.declarer(competitive(List.of(10L, 11L), List.of(10L))))
                .isInstanceOf(PartieInvalideException.class).hasMessageContaining("indisponible");
    }

    @Test
    void accepteUnePartieSansScoreEtRefuseUnScoreHorsParticipants() {
        Partie partie = service.declarer(competitive(List.of(10L, 11L), List.of(10L)));
        assertThat(partie.getParticipations()).allSatisfy(p -> assertThat(p.getScore()).isNull());

        Map<Long, Integer> scores = Map.of(12L, 5);
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(1L, null, null, List.of(10L), scores, List.of(10L),
                null, null, null, null))).isInstanceOf(PartieInvalideException.class);
    }

    @Test
    void accepteUnScoreSurUnSeulJoueur() {
        Map<Long, Integer> scores = new HashMap<>();
        scores.put(10L, 7);
        scores.put(11L, null);

        Partie partie = service.declarer(new PartieSaisie(1L, null, null, List.of(10L, 11L), scores, List.of(10L), null,
                null, null, null));

        assertThat(de(partie, 10).getScore()).isEqualTo(7);
        assertThat(de(partie, 11).getScore()).isNull();
    }

    @Test
    void normaliseOuRefuseLeLieu() {
        assertThat(service.declarer(new PartieSaisie(1L, null, "   ", List.of(10L), null, List.of(10L), null, null, null,
                null)).getLieu()).isNull();
        assertThat(service.declarer(new PartieSaisie(1L, null, "a".repeat(100), List.of(10L), null, List.of(10L), null,
                null, null, null)).getLieu()).hasSize(100);
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(1L, null, "a".repeat(101), List.of(10L), null,
                List.of(10L), null, null, null, null)))
                .isInstanceOfSatisfying(PartieInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("lieu"));
    }

    @Test
    void creeDeuxEquipesPropresALaPartie() {
        Map<Long, Integer> equipeDe = new LinkedHashMap<>();
        equipeDe.put(10L, 1);
        equipeDe.put(11L, 1);
        equipeDe.put(12L, 2);
        equipeDe.put(13L, 2);

        Partie partie = service.declarer(new PartieSaisie(1L, null, null, List.of(10L, 11L, 12L, 13L), null, List.of(12L),
                equipeDe, Map.of(1, "Les Rouges"), null, null));

        assertThat(partie.getEquipes()).extracting(Equipe::getNom).containsExactly("Les Rouges", "Équipe 2");
        assertThat(de(partie, 10).getEquipe()).isSameAs(de(partie, 11).getEquipe());
        assertThat(de(partie, 10).getEquipe()).isNotSameAs(de(partie, 12).getEquipe());
        assertThat(partie.getEquipes()).allSatisfy(equipe -> assertThat(equipe.getPartie()).isSameAs(partie));
    }

    @Test
    void uneEquipeGagnanteRendGagnantsTousSesMembres() {
        Map<Long, Integer> equipeDe = new LinkedHashMap<>();
        equipeDe.put(10L, 1);
        equipeDe.put(11L, 1);
        equipeDe.put(12L, 2);
        equipeDe.put(13L, 2);

        Partie partie = service.declarer(new PartieSaisie(1L, null, null, List.of(10L, 11L, 12L, 13L), null, null,
                equipeDe, null, List.of(1), null));

        assertThat(de(partie, 10).isGagnant()).isTrue();
        assertThat(de(partie, 11).isGagnant()).isTrue();
        assertThat(de(partie, 12).isGagnant()).isFalse();
        assertThat(de(partie, 13).isGagnant()).isFalse();
    }

    @Test
    void refuseLesEquipesIncoherentes() {
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(1L, null, null, List.of(10L, 11L), null, List.of(10L),
                Map.of(10L, 1), Map.of(2, "Fantômes"), null, null)))
                .isInstanceOf(PartieInvalideException.class).hasMessageContaining("aucun membre");
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(1L, null, null, List.of(10L), null, List.of(10L),
                Map.of(12L, 1), null, null, null))).isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(1L, null, null, List.of(10L), null, List.of(10L),
                Map.of(10L, 9), null, null, null))).isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(1L, null, null, List.of(10L), null, null,
                Map.of(10L, 1), null, List.of(2), null))).isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(1L, null, null, List.of(10L), null, List.of(10L),
                Map.of(10L, 1), Map.of(1, "n".repeat(101)), null, null))).isInstanceOf(PartieInvalideException.class);
    }

    @Test
    void uneVictoireCooperativeCompteChezTousLesParticipants() {
        Partie partie = service.declarer(new PartieSaisie(2L, null, null, List.of(10L, 11L, 12L), null, List.of(10L), null,
                null, null, ResultatCooperatif.VICTOIRE));

        assertThat(partie.getResultatCooperatif()).isEqualTo(ResultatCooperatif.VICTOIRE);
        assertThat(partie.getParticipations()).allSatisfy(p -> assertThat(p.isGagnant()).isTrue());
    }

    @Test
    void uneDefaiteCooperativeNeDonneAucunGagnant() {
        Partie partie = service.declarer(new PartieSaisie(2L, null, null, List.of(10L, 11L), null, null, null, null,
                null, ResultatCooperatif.DEFAITE));

        assertThat(partie.getResultatCooperatif()).isEqualTo(ResultatCooperatif.DEFAITE);
        assertThat(partie.getParticipations()).allSatisfy(p -> assertThat(p.isGagnant()).isFalse());
    }

    @Test
    void exigeUnResultatPourUnJeuCooperatif() {
        assertThatThrownBy(() -> service.declarer(new PartieSaisie(2L, null, null, List.of(10L), null, List.of(10L), null,
                null, null, null)))
                .isInstanceOfSatisfying(PartieInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("resultat"));
    }

    private Partie partieExistante() {
        Partie partie = new Partie(catan, LocalDate.of(2026, 5, 2));
        ReflectionTestUtils.setField(partie, "id", 5L);
        for (long id : new long[] { 10, 11 }) {
            Participation participation = new Participation(partie, annuaire.get(id));
            participation.setGagnant(id == 10);
            participation.setScore((int) id);
            partie.getParticipations().add(participation);
        }
        when(parties.trouverAvecJeu(5L)).thenReturn(Optional.of(partie));
        return partie;
    }

    @Test
    void modifieLesParticipationsEnPlaceAjouteEtRetireDesJoueurs() {
        Partie partie = partieExistante();
        Participation dixAvant = de(partie, 10);

        service.modifier(5L, competitive(List.of(10L, 12L), List.of(12L)));

        assertThat(partie.getParticipations()).hasSize(2);
        assertThat(de(partie, 10)).isSameAs(dixAvant);
        assertThat(de(partie, 10).isGagnant()).isFalse();
        assertThat(de(partie, 12).isGagnant()).isTrue();
        assertThat(partie.getParticipations()).noneMatch(p -> p.getJoueur().getId() == 11);
    }

    @Test
    void remplaceLesEquipesLorsDeLaModification() {
        Partie partie = partieExistante();

        service.modifier(5L, new PartieSaisie(1L, null, null, List.of(10L, 11L), null, null, Map.of(10L, 1, 11L, 1), null,
                List.of(1), null));

        assertThat(partie.getEquipes()).hasSize(1);
        assertThat(de(partie, 10).getEquipe()).isSameAs(de(partie, 11).getEquipe());
        assertThat(partie.getParticipations()).allSatisfy(p -> assertThat(p.isGagnant()).isTrue());
    }

    @Test
    void conserveUnJoueurDevenuAnonymeMaisNeLAjoutePasAUneNouvellePartie() {
        Partie partie = partieExistante();
        annuaire.get(11L).anonymiser();

        service.modifier(5L, competitive(List.of(10L, 11L), List.of(10L)));
        assertThat(partie.getParticipations()).hasSize(2);

        assertThatThrownBy(() -> service.declarer(competitive(List.of(10L, 11L), List.of(10L))))
                .isInstanceOf(PartieInvalideException.class);
    }

    @Test
    void refuseDeModifierUnePartieEnCoursOuInconnue() {
        Partie partie = partieExistante();
        partie.setStatut(StatutPartie.EN_COURS);

        assertThatThrownBy(() -> service.modifier(5L, competitive(List.of(10L), List.of(10L))))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.modifier(99L, competitive(List.of(10L), List.of(10L))))
                .isInstanceOf(PartieIntrouvableException.class);
    }

    @Test
    void detacheLesEquipesAvantDeSupprimerLaPartie() {
        Partie partie = partieExistante();
        Equipe equipe = new Equipe(partie, "Les Rouges");
        partie.getEquipes().add(equipe);
        de(partie, 10).setEquipe(equipe);
        when(parties.findById(5L)).thenReturn(Optional.of(partie));

        service.supprimer(5L);

        InOrder ordre = inOrder(parties);
        ordre.verify(parties).flush();
        ordre.verify(parties).delete(partie);
        assertThat(de(partie, 10).getEquipe()).isNull();
    }

    @Test
    void repond404PourUneSuppressionInconnue() {
        when(parties.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimer(99L)).isInstanceOf(PartieIntrouvableException.class);
    }

    @Test
    void listeToutesLesPartiesOuFiltreParJeuAvecLeursParticipations() {
        Partie partie = partieExistante();
        when(parties.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(partie)));
        when(parties.findByJeuId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(partie)));
        when(parties.participationsDesParties(anyCollection())).thenReturn(partie.getParticipations());
        for (Participation participation : partie.getParticipations()) {
            ReflectionTestUtils.setField(participation, "partie", partie);
        }

        assertThat(service.lister(null, 0).getContent().get(0).participations()).hasSize(2);
        assertThat(service.lister(1L, -4).getContent()).hasSize(1);
        verify(parties).findAll(any(Pageable.class));
        verify(parties).findByJeuId(eq(1L), any(Pageable.class));
    }

    @Test
    void listeVideSansChargerDeParticipations() {
        when(parties.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThat(service.lister(null, 0).getContent()).isEmpty();
        verify(parties, never()).participationsDesParties(any());
    }

    @Test
    void suggereLesLieuxAvecUnMotifEchappeEtUneLimiteDeDix() {
        when(parties.lieuxCommencantPar(any(), any())).thenReturn(List.of("Maison de Marie"));
        ArgumentCaptor<String> motif = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> limite = ArgumentCaptor.forClass(Pageable.class);

        assertThat(service.suggererLieux(" MAI ")).containsExactly("Maison de Marie");
        service.suggererLieux("100%_!");

        verify(parties, org.mockito.Mockito.times(2)).lieuxCommencantPar(motif.capture(), limite.capture());
        assertThat(motif.getAllValues()).containsExactly("mai%", "100!%!_!!%");
        assertThat(limite.getValue().getPageSize()).isEqualTo(PartieService.LIEUX_MAX);
    }

    @Test
    void neSuggereRienPourUneSaisieVide() {
        assertThat(service.suggererLieux("  ")).isEmpty();
        assertThat(service.suggererLieux(null)).isEmpty();
        verify(parties, never()).lieuxCommencantPar(any(), any());
    }

    @Test
    void composeLeDetailPourLaModification() {
        Partie partie = partieExistante();
        when(parties.participationsDesParties(anyCollection())).thenReturn(partie.getParticipations());

        DetailPartie detail = service.detail(5L);

        assertThat(detail.partie()).isSameAs(partie);
        assertThat(detail.participations()).hasSize(2);
        assertThatThrownBy(() -> service.detail(99L)).isInstanceOf(PartieIntrouvableException.class);
    }
}
