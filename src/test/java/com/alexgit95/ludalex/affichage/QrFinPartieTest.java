package com.alexgit95.ludalex.affichage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.TokenFinPartie;
import com.alexgit95.ludalex.parties.TokenFinService;

class QrFinPartieTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-12T10:00:00Z");

    private TokenFinService tokens;
    private QrFinPartie qr;
    private Partie partie;

    @BeforeEach
    void preparer() {
        tokens = mock(TokenFinService.class);
        qr = new QrFinPartie(tokens, new AffichageProperties(Duration.ofSeconds(15), Duration.ofHours(24), ""),
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        partie = partie(5L);
        when(tokens.generer(partie)).thenReturn("clair-1", "clair-2", "clair-3");
    }

    private static Partie partie(long id) {
        Partie partie = new Partie(new Jeu("Catan"), LocalDate.of(2026, 11, 12));
        ReflectionTestUtils.setField(partie, "id", id);
        return partie;
    }

    private void tokenValideJusqua(String clair, Instant expiration) {
        when(tokens.verifier(clair)).thenReturn(Optional.of(new TokenFinPartie(partie, "e", expiration)));
    }

    @Test
    void genereUnTokenALaPremiereDemande() {
        assertThat(qr.obtenir(partie)).isEqualTo("clair-1");
        assertThat(qr.courant(5L)).contains("clair-1");
    }

    @Test
    void reutiliseLeTokenTantQuIlVautPlusDeVingtQuatreHeures() {
        qr.obtenir(partie);
        tokenValideJusqua("clair-1", MAINTENANT.plus(Duration.ofDays(5)));

        assertThat(qr.obtenir(partie)).isEqualTo("clair-1");
        assertThat(qr.obtenir(partie)).isEqualTo("clair-1");

        verify(tokens, times(1)).generer(partie);
    }

    @Test
    void renouvelleLeTokenQuandIlExpireDansMoinsDeVingtQuatreHeures() {
        qr.obtenir(partie);
        tokenValideJusqua("clair-1", MAINTENANT.plus(Duration.ofHours(23)));

        assertThat(qr.obtenir(partie)).isEqualTo("clair-2");
    }

    @Test
    void renouvellePilePoilALaLimite() {
        qr.obtenir(partie);
        tokenValideJusqua("clair-1", MAINTENANT.plus(Duration.ofHours(24)));

        assertThat(qr.obtenir(partie)).isEqualTo("clair-2");
    }

    @Test
    void renouvelleLeTokenInvalideParAilleurs() {
        qr.obtenir(partie);
        when(tokens.verifier("clair-1")).thenReturn(Optional.empty());

        assertThat(qr.obtenir(partie)).isEqualTo("clair-2");
    }

    @Test
    void neReutilisePasLeTokenDUneAutrePartie() {
        qr.obtenir(partie);
        tokenValideJusqua("clair-1", MAINTENANT.plus(Duration.ofDays(5)));
        Partie autre = partie(6L);
        when(tokens.generer(autre)).thenReturn("autre-1");

        assertThat(qr.obtenir(autre)).isEqualTo("autre-1");
        assertThat(qr.courant(5L)).isEmpty();
        assertThat(qr.courant(6L)).contains("autre-1");
    }

    @Test
    void regenereToujoursALOuverture() {
        qr.obtenir(partie);
        tokenValideJusqua("clair-1", MAINTENANT.plus(Duration.ofDays(5)));

        assertThat(qr.regenerer(partie)).isEqualTo("clair-2");
        assertThat(qr.courant(5L)).contains("clair-2");
    }

    @Test
    void neConnaitAucunTokenAvantLaPremiereGeneration() {
        assertThat(qr.courant(5L)).isEmpty();
        verify(tokens, never()).verifier(any());
    }
}
