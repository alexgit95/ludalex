package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:copiesdesactivees;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "ludalex.images.copies.actif=false"
})
@AutoConfigureMockMvc
class CopiesDesactiveesIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ApplicationContext contexte;

    @Test
    void lePlanificateurNExistePasEtLaPageIndiqueLaDesactivationSansBouton() throws Exception {
        assertThat(contexte.getBeansOfType(PlanificationCopies.class)).isEmpty();

        mvc.perform(get("/admin/images")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Traitement désactivé par la configuration")))
                .andExpect(content().string(not(containsString("/admin/images/activer"))))
                .andExpect(content().string(not(containsString("/admin/images/pause"))));
    }
}
