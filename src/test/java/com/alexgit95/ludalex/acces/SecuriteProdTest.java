package com.alexgit95.ludalex.acces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "DB_URL=jdbc:h2:mem:prod;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "CF_ACCESS_TEAM_DOMAIN=equipe.cloudflareaccess.com",
        "CF_ACCESS_AUD=audience-ludalex"
})
@ActiveProfiles("prod")
@AutoConfigureMockMvc
@Import(SecuriteProdTest.DecodeurDeTest.class)
class SecuriteProdTest {

    private static final JwtDeTest EMETTEUR = new JwtDeTest();

    @TestConfiguration
    static class DecodeurDeTest {
        @Bean
        @Primary
        JwtDecoder decodeurDeTest() {
            return EMETTEUR.decodeur();
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Environment environnement;

    @Test
    void refuseSansJwt() throws Exception {
        mvc.perform(get("/")).andExpect(status().isUnauthorized());
    }

    @Test
    void protegeLeSuiviEtLesRelancesDesCopies() throws Exception {
        mvc.perform(get("/admin/images")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/images/etat")).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/images/relancer").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/images").header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().isOk());
        mvc.perform(post("/admin/images/relancer").header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().isForbidden());
    }

    @Test
    void protegeLeTableauDeBordEtLesReglages() throws Exception {
        for (String chemin : new String[] { "/statistiques", "/statistiques/bloc/kpi", "/admin/reglages" }) {
            mvc.perform(get(chemin)).andExpect(status().isUnauthorized());
            mvc.perform(get(chemin).header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                    .andExpect(status().isOk());
            String mauvaiseAudience = EMETTEUR.jeton(JwtDeTest.EMETTEUR, "autre-application",
                    Instant.now().plusSeconds(600));
            mvc.perform(get(chemin).header(CloudflareAccessFilter.HEADER_JWT, mauvaiseAudience))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/admin/reglages").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/reglages").header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().isForbidden());
    }

    @Test
    void accepteUnJwtValide() throws Exception {
        mvc.perform(get("/").header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/jeux"));
    }

    @Test
    void refuseUnJwtExpire() throws Exception {
        String jeton = EMETTEUR.jeton(JwtDeTest.EMETTEUR, JwtDeTest.AUDIENCE, Instant.now().minusSeconds(600));

        mvc.perform(get("/").header(CloudflareAccessFilter.HEADER_JWT, jeton)).andExpect(status().isUnauthorized());
    }

    @Test
    void refuseUneMauvaiseAudienceEn403() throws Exception {
        String jeton = EMETTEUR.jeton(JwtDeTest.EMETTEUR, "autre-application", Instant.now().plusSeconds(600));

        mvc.perform(get("/").header(CloudflareAccessFilter.HEADER_JWT, jeton)).andExpect(status().isForbidden());
    }

    @Test
    void refuseUneSignatureInconnue() throws Exception {
        String jeton = new JwtDeTest().jetonValide();

        mvc.perform(get("/").header(CloudflareAccessFilter.HEADER_JWT, jeton)).andExpect(status().isUnauthorized());
    }

    @Test
    void refuseUnJwtIllisible() throws Exception {
        mvc.perform(get("/").header(CloudflareAccessFilter.HEADER_JWT, "pas-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ignoreUnHeaderDIdentiteForge() throws Exception {
        mvc.perform(get("/").header("Cf-Access-Authenticated-User-Email", "moi@example.org"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exempteSeulementLeCheminDeFinDePartie() throws Exception {
        int statutFin = mvc.perform(get("/fin/un-token")).andReturn().getResponse().getStatus();

        assertThat(statutFin).isNotEqualTo(401).isNotEqualTo(403);
        mvc.perform(get("/jeux")).andExpect(status().isUnauthorized());
        mvc.perform(get("/finance")).andExpect(status().isUnauthorized());
    }

    @Test
    void ouvreLeLienDeFinSansJwtMaisExigeLeJwtPourLesEcransDeLApplication() throws Exception {
        String jeton = "A".repeat(43);

        mvc.perform(get("/fin/" + jeton)).andExpect(status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("X-Robots-Tag", "noindex"));
        mvc.perform(post("/fin/" + jeton + "/terminer").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/parties/direct")).andExpect(status().isUnauthorized());
        mvc.perform(get("/parties")).andExpect(status().isUnauthorized());
        mvc.perform(post("/parties/direct/lancer").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void exigeLeJwtPourLEcranAffichage() throws Exception {
        mvc.perform(get("/affichage")).andExpect(status().isUnauthorized());
        mvc.perform(get("/affichage/etat")).andExpect(status().isUnauthorized());
        mvc.perform(get("/affichage").header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().isOk());
        mvc.perform(get("/affichage/etat").header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().isOk());
    }

    @Test
    void exigeLeJwtPourLaSauvegardeEtLaRestauration() throws Exception {
        mvc.perform(get("/admin/sauvegarde")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/sauvegarde/telecharger")).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/sauvegarde/restaurer").with(csrf()).header("X-Confirmation", "RESTAURER")
                .contentType("application/zip").content(new byte[] { 1 })).andExpect(status().isUnauthorized());
        mvc.perform(get("/fin/admin/sauvegarde/telecharger")).andExpect(status().isNotFound());
        mvc.perform(get("/admin/sauvegarde").header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().isOk());
    }

    @Test
    void exigeLeJwtPourLImportMyLudo() throws Exception {
        mvc.perform(get("/admin/import-myludo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/import-myludo/progression")).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/import-myludo/analyser").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/import-myludo/importer").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/import-myludo").header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().isOk());
    }

    @Test
    void protegeParCsrfLesRequetesModifiantes() throws Exception {
        mvc.perform(post("/").header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/fin/un-token")).andExpect(status().isForbidden());
    }

    @Test
    void accepteUneRequeteModifianteAvecCsrfEtJwt() throws Exception {
        mvc.perform(post("/").with(csrf()).header(CloudflareAccessFilter.HEADER_JWT, EMETTEUR.jetonValide()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void configureLesCookiesDeSessionSecureEtLeProxy() {
        assertThat(environnement.getProperty("server.servlet.session.cookie.secure")).isEqualTo("true");
        assertThat(environnement.getProperty("server.servlet.session.cookie.http-only")).isEqualTo("true");
        assertThat(environnement.getProperty("server.forward-headers-strategy")).isEqualTo("framework");
    }
}
