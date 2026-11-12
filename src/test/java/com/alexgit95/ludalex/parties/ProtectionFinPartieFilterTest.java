package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ProtectionFinPartieFilterTest {

    /** Horloge réglable pour changer de minute. */
    private static final class HorlogeReglable extends Clock {
        private Instant instant = Instant.parse("2026-11-12T10:00:10Z");

        void avancer(Duration duree) {
            instant = instant.plus(duree);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private HorlogeReglable horloge;
    private ProtectionFinPartieFilter filtre;

    @BeforeEach
    void preparer() {
        horloge = new HorlogeReglable();
        filtre = new ProtectionFinPartieFilter(new FinPartieProperties(Duration.ofDays(7), 3, ""), horloge);
    }

    private MockHttpServletResponse appeler(String chemin, String adresse) throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", chemin);
        requete.setRemoteAddr(adresse);
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        filtre.doFilter(requete, reponse, new MockFilterChain());
        return reponse;
    }

    @Test
    void poseLesEnTetesDeProtectionSurLesLiensDeFin() throws Exception {
        MockHttpServletResponse reponse = appeler("/fin/abc", "10.0.0.1");

        assertThat(reponse.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(reponse.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(reponse.getHeader("X-Robots-Tag")).isEqualTo("noindex");
        assertThat(reponse.getStatus()).isEqualTo(200);
    }

    @Test
    void repond429AuDelaDuSeuilSansTransmettreLaRequete() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(appeler("/fin/abc", "10.0.0.1").getStatus()).isEqualTo(200);
        }

        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/fin/abc");
        requete.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        MockFilterChain chaine = new MockFilterChain();
        filtre.doFilter(requete, reponse, chaine);

        assertThat(reponse.getStatus()).isEqualTo(429);
        assertThat(reponse.getHeader("Retry-After")).isEqualTo("60");
        assertThat(chaine.getRequest()).isNull();
    }

    @Test
    void compteLesAdressesSeparement() throws Exception {
        for (int i = 0; i < 4; i++) {
            appeler("/fin/abc", "10.0.0.1");
        }

        assertThat(appeler("/fin/abc", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void repartDeZeroALaMinuteSuivante() throws Exception {
        for (int i = 0; i < 4; i++) {
            appeler("/fin/abc", "10.0.0.1");
        }
        assertThat(appeler("/fin/abc", "10.0.0.1").getStatus()).isEqualTo(429);

        horloge.avancer(Duration.ofMinutes(1));

        assertThat(appeler("/fin/abc", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void neTouchePasLesAutresChemins() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse reponse = appeler("/jeux", "10.0.0.1");
            assertThat(reponse.getStatus()).isEqualTo(200);
            assertThat(reponse.getHeader("X-Robots-Tag")).isNull();
        }
        assertThat(appeler("/finance", "10.0.0.1").getHeader("X-Robots-Tag")).isNull();
    }

    @Test
    void utiliseLEnTeteDeAdresseConfigureQuandIlEstPresent() throws Exception {
        filtre = new ProtectionFinPartieFilter(new FinPartieProperties(Duration.ofDays(7), 2, "CF-Connecting-IP"),
                horloge);
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/fin/abc");
            requete.setRemoteAddr("172.16.0.1");
            requete.addHeader("CF-Connecting-IP", "203.0.113.7, 10.0.0.1");
            filtre.doFilter(requete, new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletRequest autre = new MockHttpServletRequest("GET", "/fin/abc");
        autre.setRemoteAddr("172.16.0.1");
        autre.addHeader("CF-Connecting-IP", "203.0.113.8");
        MockHttpServletResponse reponseAutre = new MockHttpServletResponse();
        filtre.doFilter(autre, reponseAutre, new MockFilterChain());
        MockHttpServletRequest meme = new MockHttpServletRequest("GET", "/fin/abc");
        meme.setRemoteAddr("172.16.0.9");
        meme.addHeader("CF-Connecting-IP", "203.0.113.7");
        MockHttpServletResponse reponseMeme = new MockHttpServletResponse();
        filtre.doFilter(meme, reponseMeme, new MockFilterChain());

        assertThat(reponseAutre.getStatus()).isEqualTo(200);
        assertThat(reponseMeme.getStatus()).isEqualTo(429);
    }

    @Test
    void retombeSurLAdresseDeLaConnexionSiLEnTeteEstAbsent() throws Exception {
        filtre = new ProtectionFinPartieFilter(new FinPartieProperties(Duration.ofDays(7), 1, "CF-Connecting-IP"),
                horloge);
        appeler("/fin/abc", "10.0.0.1");

        assertThat(appeler("/fin/abc", "10.0.0.1").getStatus()).isEqualTo(429);
    }
}
