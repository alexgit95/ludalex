package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:copies;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=SET TIME ZONE 'UTC'",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"
})
@AutoConfigureMockMvc
class CopieImagePersistanceTest {
    @Autowired private CopieImageRepository copies;
    @Autowired private EntityManager em;
        @Autowired private MockMvc mvc;
        @Autowired private CopiesImages service;
        @Autowired private ReferencesImages references;
        @Autowired private org.springframework.transaction.PlatformTransactionManager gestionnaire;

        @Test
        void collecteParLotsEtDeduplicationSansReinitialiserLesEchecs() {
            var transaction = new org.springframework.transaction.support.TransactionTemplate(gestionnaire);
            transaction.executeWithoutResult(statut -> {
                for (int numero = 0; numero < 205; numero++) {
                    var jeu = new com.alexgit95.ludalex.jeux.Jeu("Lot " + numero);
                    jeu.setImage(ImageSource.url("https://example.org/lot-" + (numero % 2) + ".png"));
                    em.persist(jeu);
                }
            });
            try {
                java.util.List<String> urls = new java.util.ArrayList<>();
                references.parcourir(urls::add);
                assertThat(urls).hasSize(205);
                service.reconcilier();
                service.reconcilier();
                assertThat(copies.count()).isEqualTo(2);
            } finally {
                transaction.executeWithoutResult(statut -> {
                    em.createQuery("delete from Jeu").executeUpdate();
                    em.createQuery("delete from CopieImage").executeUpdate();
                });
            }
        }

        @Test
        void afficheLeSuiviEtProtegeLaRelance() throws Exception {
        CopieImage copie = new CopieImage("https://example.org/admin.png");
        copie.commencer(Instant.now());
        copie.echouer(Instant.now(), true, "<script>alert(1)</script>");
        copies.saveAndFlush(copie);
        try {
            mvc.perform(get("/admin/images")).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")));
            mvc.perform(get("/admin/images/etat")).andExpect(status().isOk())
                .andExpect(content().string(containsString("suivi-images")));
            mvc.perform(post("/admin/images/relancer").param("cle", copie.getCle())).andExpect(status().isForbidden());
            mvc.perform(post("/admin/images/relancer").param("cle", copie.getCle()).with(csrf()))
                .andExpect(status().isOk());
            assertThat(copies.findById(copie.getCle()).orElseThrow().getEtat()).isEqualTo(CopieImage.Etat.EN_ATTENTE);
        } finally { copies.deleteById(copie.getCle()); }
        }

    @Test
    @Transactional
    void persisteEtSelectionneLesTravauxAvecEcheance() {
        Instant maintenant = Instant.parse("2026-11-13T10:00:00Z");
        CopieImage copie = new CopieImage("https://example.org/a.png");
        copie.commencer(maintenant);
        copie.echouer(maintenant, false, "Indisponible");
        copies.saveAndFlush(copie);
        em.clear();
        CopieImage relue = copies.findById(copie.getCle()).orElseThrow();
        assertThat(relue.getUrl()).isEqualTo(copie.getUrl());
        assertThat(relue.getDerniereTentative()).isEqualTo(maintenant);
        assertThat(copies.eligibles(CopieImage.Etat.EN_ATTENTE, maintenant, PageRequest.of(0, 1))).isEmpty();
        assertThat(copies.eligibles(CopieImage.Etat.EN_ATTENTE, maintenant.plusSeconds(60), PageRequest.of(0, 1)))
                .extracting(CopieImage::getCle).containsExactly(copie.getCle());
        assertThat(copies.compteurs()).hasSize(1);
    }
}