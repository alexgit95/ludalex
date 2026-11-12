package com.alexgit95.ludalex.acces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class CloudflareJwtValidateursTest {

    private final JwtDeTest emetteur = new JwtDeTest();
    private final JwtDecoder decodeur = emetteur.decodeur();

    @Test
    void accepteUnJwtValide() {
        assertThat(decodeur.decode(emetteur.jetonValide()).getAudience()).contains(JwtDeTest.AUDIENCE);
    }

    @Test
    void refuseUnJwtExpire() {
        String jeton = emetteur.jeton(JwtDeTest.EMETTEUR, JwtDeTest.AUDIENCE, Instant.now().minusSeconds(600));

        assertThatThrownBy(() -> decodeur.decode(jeton)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void refuseUneMauvaiseAudienceAvecLeCodeDedie() {
        String jeton = emetteur.jeton(JwtDeTest.EMETTEUR, "autre-application", Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> decodeur.decode(jeton))
                .isInstanceOfSatisfying(JwtValidationException.class, e -> assertThat(e.getErrors())
                        .extracting(erreur -> erreur.getErrorCode())
                        .containsExactly(CloudflareJwtValidateurs.CODE_AUDIENCE));
    }

    @Test
    void refuseUnMauvaisEmetteur() {
        String jeton = emetteur.jeton("https://pirate.example.org", JwtDeTest.AUDIENCE, Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> decodeur.decode(jeton)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void refuseUneSignatureDUneAutreCle() {
        String jeton = new JwtDeTest().jetonValide();

        assertThatThrownBy(() -> decodeur.decode(jeton)).isInstanceOf(BadJwtException.class);
    }
}
