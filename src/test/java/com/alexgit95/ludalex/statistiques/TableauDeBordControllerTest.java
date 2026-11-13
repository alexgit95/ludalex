package com.alexgit95.ludalex.statistiques;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuSaisie;
import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurService;
import com.alexgit95.ludalex.parties.PartieSaisie;
import com.alexgit95.ludalex.parties.PartieService;

/** Pages et blocs de l'onglet Statistiques rendus par Thymeleaf. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tableaubordweb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class TableauDeBordControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JeuService jeux;
    @Autowired
    private JoueurService joueurs;
    @Autowired
    private PartieService parties;

    private void donnees() {
        Jeu jeu = jeux.creer(new JeuSaisie("Dominion-" + UUID.randomUUID(), null, null, false, null,
                EntreeImage.inchangee()));
        Joueur a = joueurs.creer("Zoe-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
        Joueur b = joueurs.creer("Yan-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
        parties.declarer(new PartieSaisie(jeu.getId(), LocalDate.now().minusDays(2), "Chez <b>Zoe</b>",
                List.of(a.getId(), b.getId()), null, List.of(a.getId()), null, null, null, null));
    }

    @Test
    void lAffichageDeLaPageContientLesPeriodesEtLesPlaceholdersDeBlocs() throws Exception {
        mvc.perform(get("/statistiques")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Statistiques")))
                .andExpect(content().string(containsString("12 derniers mois")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("/statistiques/bloc/kpi?periode=12m")))
                .andExpect(content().string(containsString("/statistiques/bloc/prets?periode=12m")))
                .andExpect(content().string(containsString("hx-trigger=\"load\"")))
                .andExpect(content().string(containsString("href=\"/statistiques\"")));
    }

    @Test
    void lAnneeCivileEstSelectionnable() throws Exception {
        mvc.perform(get("/statistiques").param("periode", "annee")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/statistiques/bloc/jeux?periode=annee")));
    }

    @Test
    void unePeriodeInconnueRevientAuxDouzeMois() throws Exception {
        mvc.perform(get("/statistiques").param("periode", "<script>")).andExpect(status().isOk())
                .andExpect(content().string(containsString("periode=12m")))
                .andExpect(content().string(not(containsString("<script>"))));
    }

    @Test
    void rendChaqueBlocSansErreurEtSansRessourceExterne() throws Exception {
        donnees();
        for (String bloc : TableauDeBordController.BLOCS) {
            for (String periode : List.of("30j", "12m", "annee", "tout")) {
                mvc.perform(get("/statistiques/bloc/" + bloc).param("periode", periode)).andExpect(status().isOk())
                        .andExpect(content().string(containsString("id=\"bloc-" + bloc + "\"")))
                        .andExpect(content().string(not(containsString("http://"))))
                        .andExpect(content().string(not(containsString("https://"))))
                        .andExpect(content().string(not(containsString("<script"))));
            }
        }
    }

    @Test
    void leBlocKpiAfficheLesChiffresEtLHeureDeMiseAJour() throws Exception {
        donnees();

        mvc.perform(get("/statistiques/bloc/kpi")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Parties")))
                .andExpect(content().string(containsString("Ludothèque")))
                .andExpect(content().string(containsString("mis à jour à")));
    }

    @Test
    void leBlocActiviteContientLesDiagrammesSvg() throws Exception {
        donnees();

        mvc.perform(get("/statistiques/bloc/activite").param("periode", "tout")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Parties par mois")))
                .andExpect(content().string(containsString("<svg")))
                .andExpect(content().string(containsString("stroke-dasharray")))
                .andExpect(content().string(containsString("class=\"cellule")));
    }

    @Test
    void lesContenusSaisisSontEchappes() throws Exception {
        donnees();

        mvc.perform(get("/statistiques/bloc/activite").param("periode", "tout")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Chez <b>Zoe</b>"))))
                .andExpect(content().string(containsString("Chez &lt;b&gt;Zoe&lt;/b&gt;")));
    }

    @Test
    void unBlocInconnuEstIntrouvable() throws Exception {
        mvc.perform(get("/statistiques/bloc/inconnu")).andExpect(status().isNotFound());
        mvc.perform(get("/statistiques/bloc/..%2Fadmin")).andExpect(status().is4xxClientError());
    }
}
