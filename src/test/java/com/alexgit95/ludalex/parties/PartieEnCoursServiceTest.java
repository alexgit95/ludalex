package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;

class PartieEnCoursServiceTest {

    private PartieService parties;
    private PartieRepository repertoire;
    private TokenFinService tokens;
    private PartieEnCoursService service;
    private Jeu catan;
    private Joueur marie;
    private Joueur paul;

    @BeforeEach
    void preparer() {
        parties = mock(PartieService.class);
        repertoire = mock(PartieRepository.class);
        tokens = mock(TokenFinService.class);
        service = new PartieEnCoursService(parties, repertoire, tokens, mock(PlatformTransactionManager.class));
        catan = new Jeu("Catan");
        marie = joueur(10, "Marie");
        paul = joueur(11, "Paul");
        when(repertoire.trouverActives()).thenReturn(List.of());
        when(repertoire.save(any(Partie.class))).thenAnswer(appel -> appel.getArgument(0));
        when(repertoire.participationsDesParties(any())).thenReturn(List.of());
    }

    private static Joueur joueur(long id, String nom) {
        Joueur joueur = new Joueur(nom);
        ReflectionTestUtils.setField(joueur, "id", id);
        return joueur;
    }

    private Partie partie(long id, StatutPartie statut) {
        Partie partie = new Partie(catan, LocalDate.of(2026, 11, 12));
        ReflectionTestUtils.setField(partie, "id", id);
        partie.setStatut(statut);
        partie.getParticipations().add(new Participation(partie, marie));
        partie.getParticipations().add(new Participation(partie, paul));
        when(repertoire.trouverAvecJeu(id)).thenReturn(Optional.of(partie));
        return partie;
    }

    private static LancementSaisie lancement() {
        return new LancementSaisie(1L, List.of(10L, 11L), null, null);
    }

    private static FinSaisie fin() {
        return new FinSaisie(List.of(10L), null, null, null);
    }

    @Test
    void lanceUnePartieEtGenereSonToken() {
        Partie creee = partie(5, StatutPartie.EN_COURS);
        when(parties.creerEnCours(any())).thenReturn(creee);

        assertThat(service.lancer(lancement())).isSameAs(creee);

        verify(tokens).generer(creee);
    }

    @Test
    void refuseDeLancerQuandUnePartieEstEnCoursOuEnPause() {
        Partie enPause = partie(5, StatutPartie.EN_PAUSE);
        when(repertoire.trouverActives()).thenReturn(List.of(enPause));

        assertThatThrownBy(() -> service.lancer(lancement()))
                .isInstanceOfSatisfying(PartieActiveException.class, e -> assertThat(e.getPartieActiveId()).isEqualTo(5L));
        verify(parties, never()).creerEnCours(any());
        verify(tokens, never()).generer(any());
    }

    @Test
    void termineLaPartiePrecedenteAvantDeLancerLaNouvelle() {
        Partie precedente = partie(5, StatutPartie.EN_COURS);
        Partie nouvelle = partie(6, StatutPartie.EN_COURS);
        when(parties.creerEnCours(any())).thenReturn(nouvelle);

        Partie resultat = service.terminerEtLancer(5L, fin(), lancement());

        assertThat(resultat).isSameAs(nouvelle);
        InOrder ordre = inOrder(parties, tokens, repertoire);
        ordre.verify(parties).cloturer(precedente, fin());
        ordre.verify(tokens).invaliderPour(5L);
        ordre.verify(repertoire).trouverActives();
        ordre.verify(parties).creerEnCours(lancement());
        ordre.verify(tokens).generer(nouvelle);
    }

    @Test
    void neLancePasLaNouvellePartieSiLaFinDeLaPrecedenteEstInvalide() {
        Partie precedente = partie(5, StatutPartie.EN_COURS);
        doThrow(new PartieInvalideException("gagnants", "Désignez au moins un gagnant"))
                .when(parties).cloturer(precedente, fin());

        assertThatThrownBy(() -> service.terminerEtLancer(5L, fin(), lancement()))
                .isInstanceOf(PartieInvalideException.class);

        verify(parties, never()).creerEnCours(any());
    }

    @Test
    void refuseDeTerminerEtLancerAvecUnePartieDejaTerminee() {
        partie(5, StatutPartie.TERMINEE);

        assertThatThrownBy(() -> service.terminerEtLancer(5L, fin(), lancement()))
                .isInstanceOf(PartieInvalideException.class);
        verify(parties, never()).cloturer(any(), any());
    }

    @Test
    void metEnPauseAvecLeJoueurSuivantEtInvalideLesTokens() {
        Partie partie = partie(5, StatutPartie.EN_COURS);

        service.pauser(5L, 11L);

        assertThat(partie.getStatut()).isEqualTo(StatutPartie.EN_PAUSE);
        assertThat(partie.getJoueurSuivant()).isSameAs(paul);
        verify(tokens).invaliderPour(5L);
        verify(repertoire).save(partie);
    }

    @Test
    void refuseUnePauseInvalide() {
        partie(5, StatutPartie.EN_COURS);

        assertThatThrownBy(() -> service.pauser(5L, null))
                .isInstanceOfSatisfying(PartieInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("joueurSuivant"));
        assertThatThrownBy(() -> service.pauser(5L, 99L)).isInstanceOf(PartieInvalideException.class);
        verify(tokens, never()).invaliderPour(any());
    }

    @Test
    void refuseDeMettreEnPauseUnePartieNonEnCours() {
        partie(5, StatutPartie.EN_PAUSE);
        partie(6, StatutPartie.TERMINEE);

        assertThatThrownBy(() -> service.pauser(5L, 10L)).isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.pauser(6L, 10L)).isInstanceOf(PartieInvalideException.class);
        when(repertoire.trouverAvecJeu(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pauser(9L, 10L)).isInstanceOf(PartieIntrouvableException.class);
    }

    @Test
    void repreneUnePartieEnPauseAvecUnNouveauToken() {
        Partie partie = partie(5, StatutPartie.EN_PAUSE);
        partie.setJoueurSuivant(marie);

        service.reprendre(5L);

        assertThat(partie.getStatut()).isEqualTo(StatutPartie.EN_COURS);
        assertThat(partie.getJoueurSuivant()).isNull();
        verify(tokens).generer(partie);
    }

    @Test
    void refuseDeReprendreUnePartieQuiNEstPasEnPause() {
        partie(5, StatutPartie.EN_COURS);

        assertThatThrownBy(() -> service.reprendre(5L)).isInstanceOf(PartieInvalideException.class);
        verify(tokens, never()).generer(any());
    }

    @Test
    void termineUnePartieEtInvalideSesTokens() {
        Partie partie = partie(5, StatutPartie.EN_PAUSE);

        service.terminer(5L, fin());

        verify(parties).cloturer(partie, fin());
        verify(tokens).invaliderPour(5L);
    }

    @Test
    void neTerminePasUnePartieDejaTerminee() {
        partie(5, StatutPartie.TERMINEE);

        assertThatThrownBy(() -> service.terminer(5L, fin())).isInstanceOf(PartieInvalideException.class);
        verify(parties, never()).cloturer(any(), any());
    }

    @Test
    void consommeLeTokenAvantDeTerminer() {
        Partie partie = partie(5, StatutPartie.EN_COURS);
        TokenFinPartie token = new TokenFinPartie(partie, "e", java.time.Instant.now().plusSeconds(60));
        when(tokens.consommer("jeton")).thenReturn(token);

        service.terminerViaToken("jeton", fin());

        InOrder ordre = inOrder(tokens, parties);
        ordre.verify(tokens).consommer("jeton");
        ordre.verify(parties).cloturer(partie, fin());
        ordre.verify(tokens).invaliderPour(5L);
    }

    @Test
    void neTermineRienQuandLeTokenEstInvalide() {
        when(tokens.consommer("jeton")).thenThrow(new TokenFinInvalideException());

        assertThatThrownBy(() -> service.terminerViaToken("jeton", fin())).isInstanceOf(TokenFinInvalideException.class);
        assertThatThrownBy(() -> service.pauserViaToken("jeton", 10L)).isInstanceOf(TokenFinInvalideException.class);

        verify(parties, never()).cloturer(any(), any());
        verify(repertoire, never()).save(any());
    }

    @Test
    void refuseUnTokenDontLaPartieNEstPlusEnCours() {
        Partie partie = partie(5, StatutPartie.EN_PAUSE);
        when(tokens.consommer("jeton")).thenReturn(new TokenFinPartie(partie, "e", java.time.Instant.now().plusSeconds(60)));

        assertThatThrownBy(() -> service.terminerViaToken("jeton", fin())).isInstanceOf(TokenFinInvalideException.class);
        verify(parties, never()).cloturer(any(), any());
    }

    @Test
    void metEnPauseViaLeToken() {
        Partie partie = partie(5, StatutPartie.EN_COURS);
        when(tokens.consommer("jeton")).thenReturn(new TokenFinPartie(partie, "e", java.time.Instant.now().plusSeconds(60)));

        service.pauserViaToken("jeton", 10L);

        assertThat(partie.getStatut()).isEqualTo(StatutPartie.EN_PAUSE);
        assertThat(partie.getJoueurSuivant()).isSameAs(marie);
    }

    @Test
    void composeLaVueDuLienSeulementPourUnTokenValide() {
        Partie partie = partie(5, StatutPartie.EN_COURS);
        when(tokens.verifier("jeton")).thenReturn(Optional.of(new TokenFinPartie(partie, "e",
                java.time.Instant.now().plusSeconds(60))));
        when(tokens.verifier("autre")).thenReturn(Optional.empty());

        assertThat(service.vueDuLien("jeton")).get().satisfies(vue -> {
            assertThat(vue.jeu()).isSameAs(catan);
            assertThat(vue.partie()).isSameAs(partie);
        });
        assertThat(service.vueDuLien("autre")).isEmpty();
    }

    @Test
    void exposeLaPartieActive() {
        assertThat(service.active()).isEmpty();

        Partie enPause = partie(5, StatutPartie.EN_PAUSE);
        when(repertoire.trouverActives()).thenReturn(List.of(enPause));

        assertThat(service.active()).get().extracting(VuePartie::partie).extracting(Partie::getId).isEqualTo(5L);
    }
}
