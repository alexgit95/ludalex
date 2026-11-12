package com.alexgit95.ludalex.acces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

/** Profil par défaut : dev, sans authentification. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dev;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class SecuriteDevTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Environment environnement;

    @Test
    void dev_estLeProfilParDefaut() {
        assertThat(environnement.getActiveProfiles()).isEmpty();
        assertThat(environnement.getDefaultProfiles()).contains("dev");
        assertThat(environnement.getProperty("ludalex.acces.mode")).isEqualTo("aucun");
    }

    @Test
    void autoriseTousLesAccesSansAuthentification() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ludothèque")));
    }

    @Test
    void ignoreUnJwtInvalideEnDev() throws Exception {
        mvc.perform(get("/").header(CloudflareAccessFilter.HEADER_JWT, "invalide")).andExpect(status().isOk());
    }

    @Test
    void protegeParCsrfLesRequetesModifiantes() throws Exception {
        mvc.perform(post("/")).andExpect(status().isForbidden());
    }

    @Test
    void accepteUneRequeteHtmxAvecJetonCsrfEnEntete() throws Exception {
        mvc.perform(post("/").with(csrf().asHeader()).header("HX-Request", "true"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void exposeLeJetonCsrfAuxPages() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void sertLeManifesteEtLeServiceWorker() throws Exception {
        mvc.perform(get("/manifest.webmanifest")).andExpect(status().isOk())
                .andExpect(content().contentType("application/manifest+json"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"display\": \"standalone\"")));
        mvc.perform(get("/sw.js")).andExpect(status().isOk());
        mvc.perform(get("/js/htmx.min.js")).andExpect(status().isOk());
        mvc.perform(get("/icons/icon-512.png")).andExpect(status().isOk());
    }
}
