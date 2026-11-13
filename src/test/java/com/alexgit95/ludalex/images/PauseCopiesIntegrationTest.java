package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.alexgit95.ludalex.jeux.Jeu;

import jakarta.persistence.EntityManager;

/** Démarrage en pause, commandes de l'administration et inscription des copies pendant la pause. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pausecopies;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "ludalex.images.copies.actif=true"
})
@AutoConfigureMockMvc
class PauseCopiesIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EtatCopies etat;
    @Autowired
    private PlanificationCopies planification;
    @Autowired
    private CopiesImages copies;
    @Autowired
    private CopieImageRepository depot;
    @Autowired
    private EntityManager em;
    @Autowired
    private PlatformTransactionManager gestionnaire;

    @AfterEach
    void remettreEnPause() {
        etat.mettreEnPause();
    }

    @Test
    void lApplicationDemarreEnPause() throws Exception {
        assertThat(etat.actif()).isFalse();
        mvc.perform(get("/admin/images")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Récupération en pause")))
                .andExpect(content().string(containsString("/admin/images/activer")))
                .andExpect(content().string(not(containsString("/admin/images/pause"))));
    }

    @Test
    void activerPuisMettreEnPauseDepuisLAdministration() throws Exception {
        mvc.perform(post("/admin/images/activer").with(csrf())).andExpect(status().isOk())
                .andExpect(content().string(containsString("suivi-images")))
                .andExpect(content().string(containsString("Récupération active")))
                .andExpect(content().string(containsString("/admin/images/pause")));
        assertThat(etat.actif()).isTrue();

        mvc.perform(post("/admin/images/pause").with(csrf())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Récupération en pause")));
        assertThat(etat.actif()).isFalse();
    }

    @Test
    void lesCommandesRepeteesNeProduisentPasDErreur() throws Exception {
        mvc.perform(post("/admin/images/activer").with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/admin/images/activer").with(csrf())).andExpect(status().isOk());
        assertThat(etat.actif()).isTrue();
        mvc.perform(post("/admin/images/pause").with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/admin/images/pause").with(csrf())).andExpect(status().isOk());
        assertThat(etat.actif()).isFalse();
    }

    @Test
    void refuseLesCommandesSansJetonCsrfSansChangerLEtat() throws Exception {
        mvc.perform(post("/admin/images/activer")).andExpect(status().isForbidden());
        assertThat(etat.actif()).isFalse();
        etat.activer();
        mvc.perform(post("/admin/images/pause")).andExpect(status().isForbidden());
        assertThat(etat.actif()).isTrue();
    }

    @Test
    void uneNouvelleReferencePendantLaPauseRestEnAttenteSansTentative() {
        TransactionTemplate tx = new TransactionTemplate(gestionnaire);
        String url = "https://example.org/pause-integration.png";
        tx.executeWithoutResult(s -> {
            Jeu jeu = new Jeu("Jeu en pause");
            jeu.setImage(ImageSource.url(url));
            em.persist(jeu);
        });
        try {
            copies.reconcilier();
            planification.traiter();
            planification.traiter();

            CopieImage copie = depot.findById(CopieImage.cle(url)).orElseThrow();
            assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.EN_ATTENTE);
            assertThat(copie.getTentatives()).isZero();
        } finally {
            tx.executeWithoutResult(s -> {
                em.createQuery("delete from Jeu").executeUpdate();
                em.createQuery("delete from CopieImage").executeUpdate();
            });
        }
    }
}
