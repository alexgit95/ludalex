package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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

/** Page publique du lien de fin de partie : le token est la seule autorisation. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finweb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "ludalex.fin.limite-par-minute=100000"
})
@AutoConfigureMockMvc
class FinPartieControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PartieEnCoursService enCours;

    @Autowired
    private TokenFinService tokens;

    @Autowired
    private TokenFinPartieRepository depotTokens;

    @Autowired
    private PartieRepository depotParties;

    @Autowired
    private PartieService parties;

    @Autowired
    private JeuService jeux;

    @Autowired
    private JoueurService joueurs;

    private Jeu jeu;
    private Joueur marie;
    private Joueur paul;
    private Partie partie;
    private String lien;

    @BeforeEach
    void preparer() {
        enCours.active().ifPresent(vue -> parties.supprimer(vue.partie().getId()));
        jeu = jeux.creer(new JeuSaisie("Jeu-" + UUID.randomUUID(), null, null, false, null, EntreeImage.inchangee()));
        marie = joueurs.creer("Marie-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
        paul = joueurs.creer("Paul-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
        partie = enCours.lancer(new LancementSaisie(jeu.getId(), List.of(marie.getId(), paul.getId()), null, null));
        lien = tokens.generer(partie);
    }

    private String corps(String chemin) throws Exception {
        return mvc.perform(get(chemin)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void afficheLeJeuLesJoueursEtLesActionsSansConsommerLeToken() throws Exception {
        mvc.perform(get("/fin/" + lien)).andExpect(status().isOk())
                .andExpect(content().string(containsString(jeu.getNom())))
                .andExpect(content().string(containsString(marie.getNom())))
                .andExpect(content().string(containsString("Valider la fin de la partie")))
                .andExpect(content().string(containsString("Mettre en pause")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/fin/" + lien)).andExpect(status().isOk());
    }

    @Test
    void neContientAucunLienNiRessourceVersLeResteDeLApplication() throws Exception {
        String page = corps("/fin/" + lien);

        assertThat(page).doesNotContain("href=\"/jeux\"").doesNotContain("href=\"/admin\"")
                .doesNotContain("/css/ludalex.css").doesNotContain("/js/").doesNotContain("/images/");
    }

    @Test
    void posteLesEnTetesDeProtection() throws Exception {
        mvc.perform(get("/fin/" + lien))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Robots-Tag", "noindex"));
    }

    @Test
    void repondDeLaMemeFaconPourUnTokenInconnuMalFormeExpireUtiliseOuDUnePartieTerminee() throws Exception {
        String modele = corps("/fin/" + "Z".repeat(43));
        String expire = "E".repeat(43);
        depotTokens.save(new TokenFinPartie(depotParties.findById(partie.getId()).orElseThrow(),
                TokenFinService.empreinte(expire), Instant.now().minusSeconds(60)));
        String utilise = tokens.generer(partie);
        enCours.pauserViaToken(utilise, marie.getId());
        enCours.reprendre(partie.getId());
        String terminee = tokens.generer(partie);
        enCours.terminer(partie.getId(), new FinSaisie(List.of(marie.getId()), null, null, null));

        for (String candidat : List.of("Z".repeat(43), "abc", expire, utilise, terminee, lien)) {
            mvc.perform(get("/fin/" + candidat)).andExpect(status().isNotFound());
            assertThat(corps("/fin/" + candidat)).isEqualTo(modele);
        }
        assertThat(modele).contains("Lien invalide").doesNotContain(jeu.getNom());
    }

    @Test
    void termineLaPartieAvecLeGagnantEtLesScoresFacultatifs() throws Exception {
        mvc.perform(post("/fin/" + lien + "/terminer").with(csrf())
                .param("gagnantIds", String.valueOf(marie.getId()))
                .param("scores[" + marie.getId() + "]", "12").param("scores[" + paul.getId() + "]", ""))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Partie terminée")));

        DetailPartie detail = parties.detail(partie.getId());
        assertThat(detail.partie().getStatut()).isEqualTo(StatutPartie.TERMINEE);
        assertThat(detail.participations()).filteredOn(Participation::isGagnant)
                .extracting(p -> p.getJoueur().getId()).containsExactly(marie.getId());
        mvc.perform(post("/fin/" + lien + "/terminer").with(csrf()).param("gagnantIds", String.valueOf(paul.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void termineSansScoreAvecPlusieursGagnants() throws Exception {
        mvc.perform(post("/fin/" + lien + "/terminer").with(csrf())
                .param("gagnantIds", String.valueOf(marie.getId()), String.valueOf(paul.getId())))
                .andExpect(status().isOk());

        assertThat(parties.detail(partie.getId()).participations()).allMatch(Participation::isGagnant);
    }

    @Test
    void termineUnePartieCooperative() throws Exception {
        parties.supprimer(partie.getId());
        Jeu cooperatif = jeux.creer(new JeuSaisie("Coop-" + UUID.randomUUID(), null, null, true, null,
                EntreeImage.inchangee()));
        Partie coop = enCours.lancer(new LancementSaisie(cooperatif.getId(), List.of(marie.getId(), paul.getId()), null,
                null));
        String clair = tokens.generer(coop);

        mvc.perform(get("/fin/" + clair)).andExpect(status().isOk()).andExpect(content().string(containsString("Victoire")));
        mvc.perform(post("/fin/" + clair + "/terminer").with(csrf()).param("resultat", "VICTOIRE"))
                .andExpect(status().isOk());

        assertThat(parties.detail(coop.getId()).partie().getResultatCooperatif()).isEqualTo(ResultatCooperatif.VICTOIRE);
        assertThat(parties.detail(coop.getId()).participations()).allMatch(Participation::isGagnant);
    }

    @Test
    void conserveLeTokenQuandLaSaisieEstInvalide() throws Exception {
        Joueur etranger = joueurs.creer("Etranger-" + UUID.randomUUID(), EntreeImage.inchangee(),
                EntreeImage.inchangee());

        mvc.perform(post("/fin/" + lien + "/terminer").with(csrf())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Désignez au moins un gagnant")));
        mvc.perform(post("/fin/" + lien + "/terminer").with(csrf()).param("gagnantIds", String.valueOf(etranger.getId())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("ne participe pas")));
        mvc.perform(post("/fin/" + lien + "/terminer").with(csrf()).param("gagnantIds", String.valueOf(marie.getId()))
                .param("scores[abc]", "1")).andExpect(status().isOk());
        mvc.perform(post("/fin/" + lien + "/terminer").with(csrf()).param("gagnantIds", String.valueOf(marie.getId()))
                .param("scores[" + marie.getId() + "]", "abc")).andExpect(status().isOk())
                .andExpect(content().string(containsString("invalide")));

        assertThat(parties.detail(partie.getId()).partie().getStatut()).isEqualTo(StatutPartie.EN_COURS);
        mvc.perform(get("/fin/" + lien)).andExpect(status().isOk());
    }

    @Test
    void metLaPartieEnPauseEtConsommeLeToken() throws Exception {
        mvc.perform(post("/fin/" + lien + "/pause").with(csrf()).param("joueurSuivantId", String.valueOf(paul.getId())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Partie en pause")));

        VuePartie vue = enCours.vue(partie.getId());
        assertThat(vue.partie().getStatut()).isEqualTo(StatutPartie.EN_PAUSE);
        assertThat(vue.joueurSuivant().getId()).isEqualTo(paul.getId());
        mvc.perform(get("/fin/" + lien)).andExpect(status().isNotFound());
    }

    @Test
    void refuseUnePauseSansJoueurOuAvecUnJoueurEtranger() throws Exception {
        Joueur etranger = joueurs.creer("Etranger-" + UUID.randomUUID(), EntreeImage.inchangee(),
                EntreeImage.inchangee());

        mvc.perform(post("/fin/" + lien + "/pause").with(csrf())).andExpect(status().isOk())
                .andExpect(content().string(containsString("à quel joueur")));
        mvc.perform(post("/fin/" + lien + "/pause").with(csrf()).param("joueurSuivantId", String.valueOf(etranger.getId())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("à quel joueur")));

        mvc.perform(get("/fin/" + lien)).andExpect(status().isOk());
    }

    @Test
    void refuseUneRequeteSansJetonCsrfEtNeConsommeRien() throws Exception {
        mvc.perform(post("/fin/" + lien + "/terminer").param("gagnantIds", String.valueOf(marie.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/fin/" + lien + "/pause").param("joueurSuivantId", String.valueOf(paul.getId())))
                .andExpect(status().isForbidden());

        assertThat(enCours.vue(partie.getId()).partie().getStatut()).isEqualTo(StatutPartie.EN_COURS);
        mvc.perform(get("/fin/" + lien)).andExpect(status().isOk());
    }

    @Test
    void unTokenNePermetPasDAgirSurUneAutrePartie() throws Exception {
        enCours.terminer(partie.getId(), new FinSaisie(List.of(marie.getId()), null, null, null));
        Partie suivante = enCours.lancer(new LancementSaisie(jeu.getId(), List.of(paul.getId()), null, null));

        mvc.perform(post("/fin/" + lien + "/terminer").with(csrf()).param("gagnantIds", String.valueOf(paul.getId())))
                .andExpect(status().isNotFound());

        assertThat(enCours.vue(suivante.getId()).partie().getStatut()).isEqualTo(StatutPartie.EN_COURS);
        assertThat(corps("/fin/" + lien)).doesNotContain(paul.getNom());
    }

    @Test
    void nExposeAucuneAutreDonneeQueCelleDeLaPartie() throws Exception {
        Joueur horsPartie = joueurs.creer("HorsPartie-" + UUID.randomUUID(), EntreeImage.inchangee(),
                EntreeImage.inchangee());

        mvc.perform(get("/fin/" + lien)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(horsPartie.getNom()))));
    }
}
