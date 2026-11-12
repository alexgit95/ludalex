package com.alexgit95.ludalex.acces;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class DemarrageProdTest {

    @Configuration
    @EnableConfigurationProperties(AccesProperties.class)
    static class Proprietes {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Proprietes.class, SecurityConfig.DecodeurCloudflare.class)
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    void demarreAvecLaConfigurationComplete() {
        runner.withPropertyValues(
                "ludalex.acces.mode=cloudflare",
                "ludalex.acces.team-domain=equipe.cloudflareaccess.com",
                "ludalex.acces.audience=aud")
                .run(contexte -> assertThat(contexte).hasSingleBean(JwtDecoder.class));
    }

    @Test
    void echoueSansAudience() {
        runner.withPropertyValues(
                "ludalex.acces.mode=cloudflare",
                "ludalex.acces.team-domain=equipe.cloudflareaccess.com",
                "ludalex.acces.audience=")
                .run(contexte -> {
                    assertThat(contexte).hasFailed();
                    assertThat(contexte.getStartupFailure()).hasRootCauseMessage(
                            "Variable d'environnement obligatoire manquante : CF_ACCESS_AUD");
                });
    }

    @Test
    void echoueSiLeModeNEstPasCloudflare() {
        runner.withPropertyValues(
                "ludalex.acces.mode=aucun",
                "ludalex.acces.team-domain=equipe.cloudflareaccess.com",
                "ludalex.acces.audience=aud")
                .run(contexte -> assertThat(contexte).hasFailed());
    }
}
