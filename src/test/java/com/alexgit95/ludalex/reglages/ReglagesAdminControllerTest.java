package com.alexgit95.ludalex.reglages;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reglagesweb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class ReglagesAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReglagesService reglages;

    @Test
    void afficheLesValeursEtLesDefauts() throws Exception {
        mvc.perform(get("/admin/reglages")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Jeu dormant")))
                .andExpect(content().string(containsString("name=\"PRET_LONG_JOURS\"")));
    }

    @Test
    void enregistreUneValeurValideEtLaRetabli() throws Exception {
        mvc.perform(post("/admin/reglages").with(csrf()).param("JEU_DORMANT_MOIS", "9")
                .param("CLASSEMENT_PARTIES_MIN", "5").param("PRET_LONG_JOURS", "30"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/reglages"));
        assertThat(reglages.valeur(ReglageCle.JEU_DORMANT_MOIS)).isEqualTo(9);

        mvc.perform(post("/admin/reglages/JEU_DORMANT_MOIS/retablir").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(reglages.valeur(ReglageCle.JEU_DORMANT_MOIS)).isEqualTo(6);
    }

    @Test
    void refuseUneValeurInvalideEtReaffichePageSansRienEnregistrer() throws Exception {
        mvc.perform(post("/admin/reglages").with(csrf()).param("JEU_DORMANT_MOIS", "-1")
                .param("CLASSEMENT_PARTIES_MIN", "8").param("PRET_LONG_JOURS", "30"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Saisir un entier entre 1 et 120")));
        assertThat(reglages.valeur(ReglageCle.CLASSEMENT_PARTIES_MIN)).isEqualTo(5);
    }

    @Test
    void refuseUneRequeteSansJetonCsrf() throws Exception {
        mvc.perform(post("/admin/reglages").param("JEU_DORMANT_MOIS", "9")).andExpect(status().isForbidden());
    }
}
