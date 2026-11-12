package com.alexgit95.ludalex.affichage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OriginePubliqueTest {

    private static OriginePublique avec(String urlPublique) {
        return new OriginePublique(new AffichageProperties(Duration.ofSeconds(15), Duration.ofHours(24), urlPublique));
    }

    private static MockHttpServletRequest requete(String schema, String hote, int port) {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.setScheme(schema);
        requete.setServerName(hote);
        requete.setServerPort(port);
        return requete;
    }

    @Test
    void utiliseLeReglageExplicitSansSlashFinal() {
        assertThat(avec("https://ludalex.example.org/").depuis(requete("http", "localhost", 8181)))
                .isEqualTo("https://ludalex.example.org");
        assertThat(avec("  http://192.168.1.20:8181  ").depuis(requete("http", "localhost", 8181)))
                .isEqualTo("http://192.168.1.20:8181");
    }

    @Test
    void deduitLOrigineDeLaRequeteEnConservantUnPortNonStandard() {
        assertThat(avec("").depuis(requete("http", "localhost", 8181))).isEqualTo("http://localhost:8181");
    }

    @Test
    void ometLesPortsParDefaut() {
        assertThat(avec("").depuis(requete("https", "ludalex.example.org", 443))).isEqualTo("https://ludalex.example.org");
        assertThat(avec("").depuis(requete("http", "ludalex.example.org", 80))).isEqualTo("http://ludalex.example.org");
        assertThat(avec("").depuis(requete("https", "ludalex.example.org", 0))).isEqualTo("https://ludalex.example.org");
    }

    @Test
    void gardeLePortHttpsInhabituelEtEncadreLesAdressesIpv6() {
        assertThat(avec("").depuis(requete("https", "ludalex.example.org", 8443)))
                .isEqualTo("https://ludalex.example.org:8443");
        assertThat(avec("").depuis(requete("http", "::1", 8181))).isEqualTo("http://[::1]:8181");
    }
}
