package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.alexgit95.ludalex.jeux.Jeu;

class TokenFinServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-12T10:00:00Z");
    private static final Clock HORLOGE = Clock.fixed(MAINTENANT, ZoneOffset.UTC);

    private TokenFinPartieRepository tokens;
    private TokenFinService service;
    private Partie partie;

    @BeforeEach
    void preparer() {
        tokens = mock(TokenFinPartieRepository.class);
        service = new TokenFinService(tokens, new FinPartieProperties(Duration.ofDays(7), 30, ""), HORLOGE);
        partie = new Partie(new Jeu("Catan"), LocalDate.of(2026, 11, 12));
        ReflectionTestUtils.setField(partie, "id", 5L);
        partie.setStatut(StatutPartie.EN_COURS);
    }

    private TokenFinPartie token(String clair, Instant expiration, boolean utilise, StatutPartie statut) {
        Partie liee = new Partie(new Jeu("Catan"), LocalDate.of(2026, 11, 12));
        liee.setStatut(statut);
        TokenFinPartie token = new TokenFinPartie(liee, TokenFinService.empreinte(clair), expiration);
        ReflectionTestUtils.setField(token, "id", 9L);
        ReflectionTestUtils.setField(token, "utilise", utilise);
        when(tokens.trouverParEmpreinte(TokenFinService.empreinte(clair))).thenReturn(Optional.of(token));
        return token;
    }

    private static String clairValide() {
        return "A".repeat(43);
    }

    @Test
    void genereUnTokenDe256BitsEtNeStockeQueSonEmpreinte() {
        String clair = service.generer(partie);

        assertThat(clair).matches("[A-Za-z0-9_-]{43}");
        ArgumentCaptor<TokenFinPartie> capture = ArgumentCaptor.forClass(TokenFinPartie.class);
        verify(tokens).save(capture.capture());
        TokenFinPartie stocke = capture.getValue();
        assertThat(stocke.getEmpreinte()).matches("[0-9a-f]{64}").isNotEqualTo(clair)
                .isEqualTo(TokenFinService.empreinte(clair));
        assertThat(stocke.getPartie()).isSameAs(partie);
        assertThat(stocke.isUtilise()).isFalse();
    }

    @Test
    void faitExpirerLeTokenApresSeptJours() {
        service.generer(partie);

        ArgumentCaptor<TokenFinPartie> capture = ArgumentCaptor.forClass(TokenFinPartie.class);
        verify(tokens).save(capture.capture());
        assertThat(capture.getValue().getExpireLe()).isEqualTo(MAINTENANT.plus(Duration.ofDays(7)));
    }

    @Test
    void invalideLesTokensPrecedentsAvantDEnCreerUnNouveau() {
        service.generer(partie);

        org.mockito.InOrder ordre = org.mockito.Mockito.inOrder(tokens);
        ordre.verify(tokens).invaliderPourPartie(5L);
        ordre.verify(tokens).save(any(TokenFinPartie.class));
    }

    @Test
    void genereDesTokensDifferents() {
        assertThat(service.generer(partie)).isNotEqualTo(service.generer(partie));
    }

    @Test
    void calculeUneEmpreinteSha256Standard() {
        assertThat(TokenFinService.empreinte("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void accepteUnTokenValide() {
        token(clairValide(), MAINTENANT.plusSeconds(60), false, StatutPartie.EN_COURS);

        assertThat(service.verifier(clairValide())).isPresent();
    }

    @Test
    void refuseUnTokenMalFormeSansInterrogerLaBase() {
        assertThat(service.verifier(null)).isEmpty();
        assertThat(service.verifier("")).isEmpty();
        assertThat(service.verifier("trop-court")).isEmpty();
        assertThat(service.verifier("A".repeat(44))).isEmpty();
        assertThat(service.verifier("!".repeat(43))).isEmpty();
        verify(tokens, never()).trouverParEmpreinte(any());
    }

    @Test
    void refuseUnTokenInconnuExpireUtiliseOuDUnePartieNonEnCours() {
        assertThat(service.verifier(clairValide())).isEmpty();

        token(clairValide(), MAINTENANT.minusSeconds(1), false, StatutPartie.EN_COURS);
        assertThat(service.verifier(clairValide())).isEmpty();

        token(clairValide(), MAINTENANT.plusSeconds(60), true, StatutPartie.EN_COURS);
        assertThat(service.verifier(clairValide())).isEmpty();

        token(clairValide(), MAINTENANT.plusSeconds(60), false, StatutPartie.TERMINEE);
        assertThat(service.verifier(clairValide())).isEmpty();

        token(clairValide(), MAINTENANT.plusSeconds(60), false, StatutPartie.EN_PAUSE);
        assertThat(service.verifier(clairValide())).isEmpty();
    }

    @Test
    void refuseUnTokenExpirePile() {
        token(clairValide(), MAINTENANT, false, StatutPartie.EN_COURS);

        assertThat(service.verifier(clairValide())).isEmpty();
    }

    @Test
    void consommeUnTokenValideUneSeuleFois() {
        token(clairValide(), MAINTENANT.plusSeconds(60), false, StatutPartie.EN_COURS);
        when(tokens.consommer(eq(9L), any())).thenReturn(1);

        assertThat(service.consommer(clairValide()).getId()).isEqualTo(9L);
    }

    @Test
    void refuseLaConsommationSiUnAutreAppelAGagne() {
        token(clairValide(), MAINTENANT.plusSeconds(60), false, StatutPartie.EN_COURS);
        when(tokens.consommer(eq(9L), any())).thenReturn(0);

        assertThatThrownBy(() -> service.consommer(clairValide())).isInstanceOf(TokenFinInvalideException.class);
    }

    @Test
    void neConsommeJamaisUnTokenInvalide() {
        assertThatThrownBy(() -> service.consommer(clairValide())).isInstanceOf(TokenFinInvalideException.class);
        assertThatThrownBy(() -> service.consommer("court")).isInstanceOf(TokenFinInvalideException.class);

        verify(tokens, never()).consommer(anyLong(), any());
    }

    @Test
    void indiqueSiUneTokenValideExistePourLaPartie() {
        when(tokens.valides(eq(5L), any())).thenReturn(List.of());
        assertThat(service.aUnTokenValide(5L)).isFalse();

        when(tokens.valides(eq(5L), any())).thenReturn(
                List.of(new TokenFinPartie(partie, "e", MAINTENANT.plusSeconds(5))));
        assertThat(service.aUnTokenValide(5L)).isTrue();
    }

    @Test
    void invalideLesTokensDUnePartie() {
        service.invaliderPour(5L);

        verify(tokens).invaliderPourPartie(5L);
    }
}
