package com.alexgit95.ludalex.acces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.alexgit95.ludalex.acces.AccesProperties.Mode;

class AccesPropertiesTest {

    @Test
    void accepteUneConfigurationProdComplete() {
        new AccesProperties(Mode.CLOUDFLARE, "equipe.cloudflareaccess.com", "aud").verifierPourProd();
    }

    @Test
    void refuseUnModeAutreQueCloudflareEnProd() {
        assertThatThrownBy(() -> new AccesProperties(Mode.AUCUN, "equipe.cloudflareaccess.com", "aud")
                .verifierPourProd())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cloudflare");
    }

    @Test
    void refuseUneAudienceManquante() {
        assertThatThrownBy(() -> new AccesProperties(Mode.CLOUDFLARE, "equipe.cloudflareaccess.com", " ")
                .verifierPourProd())
                .hasMessageContaining("CF_ACCESS_AUD");
    }

    @Test
    void refuseUnDomaineManquant() {
        assertThatThrownBy(() -> new AccesProperties(Mode.CLOUDFLARE, null, "aud").verifierPourProd())
                .hasMessageContaining("CF_ACCESS_TEAM_DOMAIN");
    }

    @Test
    void normaliseLeDomaineEnEmetteurEtUrlDesCles() {
        AccesProperties proprietes = new AccesProperties(Mode.CLOUDFLARE, "https://equipe.cloudflareaccess.com/", "aud");

        assertThat(proprietes.issuer()).isEqualTo("https://equipe.cloudflareaccess.com");
        assertThat(proprietes.jwkSetUri()).isEqualTo("https://equipe.cloudflareaccess.com/cdn-cgi/access/certs");
    }
}
