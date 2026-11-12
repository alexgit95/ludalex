package com.alexgit95.ludalex.importmyludo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.alexgit95.ludalex.importmyludo.PlanImport.JoueurPlan;

/** Rendu des écrans à partir d'états connus de la session, sans dépendre de la vitesse du traitement. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:importmyludorendu;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class ImportMyLudoRenduTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ServiceImportMyLudo service;

    private static Progression progression(Progression.Phase phase, long total, int faits) {
        Progression progression = new Progression();
        progression.demarrer(phase, total);
        for (int i = 0; i < faits; i++) {
            progression.avancer("Toy Battle, 2025-01-0" + (i + 1));
        }
        return progression;
    }

    private static ServiceImportMyLudo.Vue vue(ServiceImportMyLudo.Etat etat, Progression progression,
            PlanImport plan, RapportImport rapport, String erreur) {
        return new ServiceImportMyLudo.Vue(etat, progression, plan, rapport, erreur, List.of("Cause : x"),
                List.of("plays.json"), List.of(), etat + ":" + progression.signature());
    }

    private String page() throws Exception {
        return mvc.perform(get("/admin/import-myludo")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
    }

    @Test
    void afficheLaBarreDeProgressionEtSuitLAvancementParInterrogation() throws Exception {
        when(service.vue()).thenReturn(vue(ServiceImportMyLudo.Etat.IMPORT_EN_COURS,
                progression(Progression.Phase.IMPORT, 10, 3), null, null, null));

        String html = page();

        assertThat(html).contains("Import en cours").contains("<progress").contains("max=\"10\"")
                .contains("value=\"3\"").contains("3 sur 10").contains("Toy Battle, 2025-01-03")
                .contains("hx-get=\"/admin/import-myludo/progression\"").contains("hx-trigger=\"every 1s\"")
                .contains("name=\"signature\"").contains("Ne fermez pas la page");
    }

    @Test
    void laBarreEstIndetermineeQuandLeTotalEstInconnu() throws Exception {
        when(service.vue()).thenReturn(vue(ServiceImportMyLudo.Etat.ANALYSE_EN_COURS,
                progression(Progression.Phase.LECTURE, 0, 0), null, null, null));

        String html = page();

        assertThat(html).contains("Analyse en cours").contains("Lecture des fichiers").contains("<progress")
                .doesNotContainPattern("<progress[^>]*value=").doesNotContain(" sur ");
    }

    @Test
    void lePlanListeLesIgnoreesEtLesAvertissementsEtProposeLesJoueurs() throws Exception {
        PlanImport plan = new PlanImport(List.of(), List.of(new JoueurPlan(0, "alice", "Alice", 7L, 7L, 12)),
                List.of(), 3, 2, Map.of("Aucun joueur", 2), List.of("Entrée n°4 (A, 2025-01-03) : Aucun joueur"),
                List.of("Jeu A : prix trop élevé ignoré"), 1);
        when(service.vue()).thenReturn(new ServiceImportMyLudo.Vue(ServiceImportMyLudo.Etat.ANALYSE_PRETE,
                progression(Progression.Phase.ANALYSE, 5, 5), plan, null, null, List.of(), List.of("plays.json"),
                List.of(new ServiceImportMyLudo.JoueurChoix(7L, "Alice"),
                        new ServiceImportMyLudo.JoueurChoix(8L, "Bruno")), "x"));

        String html = page();

        assertThat(html).contains("Parties ignorées et raisons").contains("2 × Aucun joueur")
                .contains("Entrée n°4 (A, 2025-01-03) : Aucun joueur").contains("1</span> avertissement(s)")
                .contains("Jeu A : prix trop élevé ignoré").contains("name=\"joueur-0\"")
                .containsPattern("<option value=\"7\"\\s+selected=\"selected\">\\s*Alice")
                .contains("<option value=\"8\">Bruno</option>").contains("3</strong> déjà présente(s)")
                .doesNotContain("hx-trigger");
    }

    @Test
    void leRapportEchappeLeContenuVenantDesFichiers() throws Exception {
        RapportImport rapport = new RapportImport(2, 1, 3, 4, 1, 5, 6, 1, Map.of("<b>raison</b>", 1),
                List.of("<script>alert(1)</script>"), List.of(), 0);
        when(service.vue()).thenReturn(vue(ServiceImportMyLudo.Etat.TERMINE,
                progression(Progression.Phase.IMPORT, 5, 5), null, rapport, null));

        String html = page();

        assertThat(html).contains("Import réussi").contains("5</strong> partie(s)")
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;").contains("&lt;b&gt;raison&lt;/b&gt;")
                .doesNotContain("<script>alert(1)</script>").doesNotContain("<b>raison</b>");
    }

    @Test
    void lErreurAfficheLeMessageEtSesDetails() throws Exception {
        when(service.vue()).thenReturn(vue(ServiceImportMyLudo.Etat.ERREUR,
                progression(Progression.Phase.LECTURE, 0, 0), null, null, "plays.json, entrée n°2 : <i>oups</i>"));

        String html = page();

        assertThat(html).contains("L'import a échoué").contains("entrée n°2 : &lt;i&gt;oups&lt;/i&gt;")
                .contains("Cause : x").contains("Recommencer");
    }
}
