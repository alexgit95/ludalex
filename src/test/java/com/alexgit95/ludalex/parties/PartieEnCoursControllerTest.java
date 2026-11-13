package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuSaisie;
import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:directweb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class PartieEnCoursControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PartieEnCoursService enCours;

    @Autowired
    private PartieService parties;

    @Autowired
    private JeuService jeux;

    @Autowired
    private JoueurService joueurs;

    private Jeu jeu;
    private Joueur marie;
    private Joueur paul;

    @BeforeEach
    void preparer() {
        enCours.active().ifPresent(vue -> parties.supprimer(vue.partie().getId()));
        jeu = jeux.creer(new JeuSaisie("Jeu-" + UUID.randomUUID(), null, null, false, null, EntreeImage.inchangee()));
        marie = joueurs.creer("Marie-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
        paul = joueurs.creer("Paul-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
    }

    private MockHttpServletRequestBuilder lancement(Jeu choisi, Joueur... participants) {
        MockHttpServletRequestBuilder requete = post("/parties/direct/lancer").with(csrf())
                .param("jeuId", String.valueOf(choisi.getId()));
        for (Joueur participant : participants) {
            requete.param("joueurIds", String.valueOf(participant.getId()));
        }
        return requete;
    }

    private Partie lancer() {
        return enCours.lancer(new LancementSaisie(jeu.getId(), List.of(marie.getId(), paul.getId()), null, null));
    }

    @Test
    void proposeLeLancementQuandAucunePartieNEstActive() throws Exception {
        mvc.perform(get("/parties/direct")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lancer une partie")))
                .andExpect(content().string(containsString(jeu.getNom())))
                .andExpect(content().string(containsString(marie.getNom())));
    }

    @Test
    void saisitEtConserveLaVariantePendantUnePartieEnDirect() throws Exception {
        jeu = jeux.creer(new JeuSaisie("Arkham-" + UUID.randomUUID(), null, null, false, null,
                EntreeImage.inchangee(), true));
        mvc.perform(get("/parties/direct/variante?jeuId=" + jeu.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"variante\"")));
        mvc.perform(get("/parties/direct/variante?jeuId=" + jeu.getId() + "&variante=Ancienne"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"Ancienne\""))));
        mvc.perform(get("/parties/direct/variante?jeuId=" + (jeu.getId() - 1))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"variante\""))));

        String variante = "Le cycle de Dunwich";
        mvc.perform(lancement(jeu, marie, paul).param("variante", variante))
                .andExpect(status().is3xxRedirection());
        Partie partie = enCours.active().orElseThrow().partie();
        assertThat(partie.getVariante()).isEqualTo(variante);
        mvc.perform(get("/parties/direct")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Variante : " + variante)));

        mvc.perform(post("/parties/direct/" + partie.getId() + "/pause").with(csrf())
                .param("joueurSuivantId", marie.getId().toString())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/parties/direct/" + partie.getId() + "/reprendre").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(enCours.active().orElseThrow().partie().getVariante()).isEqualTo(variante);
        mvc.perform(post("/parties/direct/" + partie.getId() + "/terminer").with(csrf())
                .param("gagnantIds", marie.getId().toString())).andExpect(status().is3xxRedirection());
        assertThat(parties.detail(partie.getId()).partie().getVariante()).isEqualTo(variante);
    }

    @Test
    void lanceUnePartiePuisAfficheLEtatEnCours() throws Exception {
        mvc.perform(lancement(jeu, marie, paul)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/parties/direct"));

        mvc.perform(get("/parties/direct")).andExpect(status().isOk())
                .andExpect(content().string(containsString("En cours")))
                .andExpect(content().string(containsString("Terminer la partie")))
                .andExpect(content().string(containsString("Mettre en pause")))
                .andExpect(content().string(containsString(paul.getNom())));
    }

    @Test
    void refuseUnLancementSansJoueurOuSansJeu() throws Exception {
        mvc.perform(lancement(jeu)).andExpect(status().isOk())
                .andExpect(content().string(containsString("au moins un joueur")));
        mvc.perform(post("/parties/direct/lancer").with(csrf()).param("joueurIds", String.valueOf(marie.getId())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Choisissez un jeu")));
        assertThat(enCours.active()).isEmpty();
    }

    @Test
    void imposeDeTerminerLaPartiePrecedenteAvantDeLancerUneSeconde() throws Exception {
        Partie premiere = lancer();
        Jeu autre = jeux.creer(new JeuSaisie("Autre-" + UUID.randomUUID(), null, null, false, null,
                EntreeImage.inchangee(), true));

        mvc.perform(lancement(autre, marie).param("variante", "Nouvelle campagne")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Une partie est déjà en cours")))
                .andExpect(content().string(containsString("Terminer et lancer la nouvelle partie")))
                .andExpect(content().string(containsString("name=\"jeuId\" value=\"" + autre.getId() + "\"")))
                .andExpect(content().string(containsString("name=\"variante\" value=\"Nouvelle campagne\"")))
                .andExpect(content().string(containsString("name=\"joueurIds\" value=\"" + marie.getId() + "\"")));
        assertThat(enCours.active()).get().extracting(v -> v.partie().getId()).isEqualTo(premiere.getId());

        mvc.perform(post("/parties/direct/" + premiere.getId() + "/terminer-et-lancer").with(csrf())
                .param("gagnantIds", String.valueOf(marie.getId()))
                .param("jeuId", String.valueOf(autre.getId())).param("joueurIds", String.valueOf(marie.getId()))
                .param("variante", "Nouvelle campagne"))
                .andExpect(status().is3xxRedirection());

        assertThat(parties.detail(premiere.getId()).partie().getStatut()).isEqualTo(StatutPartie.TERMINEE);
        assertThat(enCours.active()).get().satisfies(v -> {
            assertThat(v.jeu().getId()).isEqualTo(autre.getId());
            assertThat(v.partie().getVariante()).isEqualTo("Nouvelle campagne");
        });
    }

    @Test
    void neLancePasLaNouvellePartieSiLaFinDeLaPrecedenteEstIncomplete() throws Exception {
        Partie premiere = lancer();

        mvc.perform(post("/parties/direct/" + premiere.getId() + "/terminer-et-lancer").with(csrf())
                .param("jeuId", String.valueOf(jeu.getId())).param("joueurIds", String.valueOf(marie.getId())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Désignez au moins un gagnant")));

        assertThat(enCours.active()).get().extracting(v -> v.partie().getId()).isEqualTo(premiere.getId());
        assertThat(enCours.active()).get().extracting(v -> v.partie().getStatut()).isEqualTo(StatutPartie.EN_COURS);
    }

    @Test
    void metEnPauseAvecLeJoueurSuivantPuisReprend() throws Exception {
        Partie partie = lancer();

        mvc.perform(post("/parties/direct/" + partie.getId() + "/pause").with(csrf())
                .param("joueurSuivantId", String.valueOf(paul.getId())))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message", "Partie mise en pause"));
        mvc.perform(get("/parties/direct")).andExpect(status().isOk())
                .andExpect(content().string(containsString("En pause")))
                .andExpect(content().string(containsString("Reprendre la partie")))
                .andExpect(content().string(containsString("de jouer à la reprise")));

        mvc.perform(post("/parties/direct/" + partie.getId() + "/reprendre").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message", "Partie reprise"));
        assertThat(enCours.active()).get().extracting(v -> v.partie().getStatut()).isEqualTo(StatutPartie.EN_COURS);
    }

    @Test
    void signaleUnePauseOuUneRepriseImpossible() throws Exception {
        Partie partie = lancer();

        mvc.perform(post("/parties/direct/" + partie.getId() + "/pause").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erreur", "Indiquez à quel joueur c'était de jouer"));
        mvc.perform(post("/parties/direct/" + partie.getId() + "/reprendre").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erreur", "La partie n'est pas en pause"));
    }

    @Test
    void termineLaPartieDepuisLEcranDeFin() throws Exception {
        Partie partie = lancer();

        mvc.perform(get("/parties/direct/" + partie.getId() + "/terminer")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Gagnants")))
                .andExpect(content().string(containsString("Scores (facultatif)")));
        mvc.perform(post("/parties/direct/" + partie.getId() + "/terminer").with(csrf())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Désignez au moins un gagnant")));
        mvc.perform(post("/parties/direct/" + partie.getId() + "/terminer").with(csrf())
                .param("gagnantIds", String.valueOf(paul.getId())).param("scores[" + paul.getId() + "]", "7"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message", "Partie terminée"));

        assertThat(parties.detail(partie.getId()).partie().getStatut()).isEqualTo(StatutPartie.TERMINEE);
        assertThat(enCours.active()).isEmpty();
        mvc.perform(get("/parties/direct/" + partie.getId() + "/terminer")).andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erreur"));
    }

    @Test
    void repond404PourUnePartieInconnue() throws Exception {
        mvc.perform(get("/parties/direct/999999/terminer")).andExpect(status().isNotFound());
    }

    @Test
    void refuseLesRequetesSansJetonCsrf() throws Exception {
        Partie partie = lancer();

        mvc.perform(post("/parties/direct/lancer").param("jeuId", "1")).andExpect(status().isForbidden());
        mvc.perform(post("/parties/direct/" + partie.getId() + "/pause")).andExpect(status().isForbidden());
        mvc.perform(post("/parties/direct/" + partie.getId() + "/reprendre")).andExpect(status().isForbidden());
        mvc.perform(post("/parties/direct/" + partie.getId() + "/terminer")).andExpect(status().isForbidden());
        mvc.perform(post("/parties/direct/" + partie.getId() + "/terminer-et-lancer")).andExpect(status().isForbidden());
    }

    @Test
    void lAfficheLesPartiesEnCoursDansLaListeSansPossibiliteDeLesModifier() throws Exception {
        Partie partie = lancer();

        mvc.perform(get("/parties?jeu=" + jeu.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("En cours")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        containsString("/parties/" + partie.getId() + "/modifier"))));
    }
}
