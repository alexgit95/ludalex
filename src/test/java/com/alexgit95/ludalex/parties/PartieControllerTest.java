package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
        "spring.datasource.url=jdbc:h2:mem:partiesweb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class PartieControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PartieService parties;

    @Autowired
    private JeuService jeux;

    @Autowired
    private JoueurService joueurs;

    private static String unique(String prefixe) {
        return prefixe + "-" + UUID.randomUUID();
    }

    private Jeu jeu(boolean cooperatif) {
        return jeux.creer(new JeuSaisie(unique("Jeu"), null, null, cooperatif, null, EntreeImage.inchangee()));
    }

    private Joueur joueur() {
        return joueurs.creer(unique("Joueur"), EntreeImage.inchangee(), EntreeImage.inchangee());
    }

    private MockHttpServletRequestBuilder creation(Jeu jeu, Joueur... participants) {
        MockHttpServletRequestBuilder requete = post("/parties").with(csrf()).param("jeuId", String.valueOf(jeu.getId()));
        for (Joueur participant : participants) {
            requete.param("joueurIds", String.valueOf(participant.getId()));
        }
        return requete;
    }

    @Test
    void afficheLaListeEtLeFormulaireDeDeclaration() throws Exception {
        Jeu catan = jeu(false);
        Joueur marie = joueur();

        mvc.perform(get("/parties")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Déclarer une partie")))
                .andExpect(content().string(containsString(catan.getNom())));
        mvc.perform(get("/parties/nouveau?jeu=" + catan.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString(marie.getNom())))
                .andExpect(content().string(containsString("value=\"" + LocalDate.now() + "\"")))
                .andExpect(content().string(containsString("Gagnants")));
    }

    @Test
    void adapteLaZoneDeResultatAuTypeDuJeu() throws Exception {
        Jeu pandemic = jeu(true);
        Jeu catan = jeu(false);
        Jeu arkham = jeux.creer(new JeuSaisie(unique("Arkham"), null, null, false, null,
                EntreeImage.inchangee(), true));

        mvc.perform(get("/parties/resultat?jeuId=" + pandemic.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Victoire")))
                .andExpect(content().string(containsString("Défaite")));
        mvc.perform(get("/parties/resultat?jeuId=" + catan.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Gagnants")))
                .andExpect(content().string(containsString("Équipe 1 gagnante")));
        mvc.perform(get("/parties/resultat?jeuId=" + arkham.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"variante\"")));
        mvc.perform(get("/parties/resultat?jeuId=" + arkham.getId() + "&variante=Ancienne"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"Ancienne\""))));
        mvc.perform(get("/parties/resultat?jeuId=" + catan.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"variante\""))));
        mvc.perform(get("/parties/resultat")).andExpect(status().isOk());
    }

    @Test
    void enregistreEtAfficheUneVarianteLibre() throws Exception {
        Jeu jeu = jeux.creer(new JeuSaisie(unique("Arkham"), null, null, false, null, EntreeImage.inchangee(), true));
        Joueur marie = joueur();
        String variante = "Enquête du Pharaon noir";

        mvc.perform(creation(jeu, marie).param("gagnantIds", String.valueOf(marie.getId()))
                .param("variante", "  " + variante + "  ")).andExpect(status().is3xxRedirection());

        Partie partie = parties.lister(jeu.getId(), 0).getContent().get(0).partie();
        assertThat(partie.getVariante()).isEqualTo(variante);
        mvc.perform(get("/parties/" + partie.getId() + "/modifier")).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"" + variante + "\"")));
        String autreVariante = "Le cycle de Dunwich";
        mvc.perform(post("/parties/" + partie.getId()).with(csrf()).param("jeuId", jeu.getId().toString())
                .param("joueurIds", marie.getId().toString()).param("gagnantIds", marie.getId().toString())
                .param("variante", autreVariante)).andExpect(status().is3xxRedirection());
        assertThat(parties.detail(partie.getId()).partie().getVariante()).isEqualTo(autreVariante);
        mvc.perform(get("/parties?jeu=" + jeu.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString(autreVariante)));
    }

    @Test
    void conserveLaVarianteHistoriqueSiLeJeuDesactiveSonIndicateur() throws Exception {
        Jeu jeu = jeux.creer(new JeuSaisie(unique("Arkham"), null, null, false, null, EntreeImage.inchangee(), true));
        Joueur marie = joueur();
        Partie partie = parties.declarer(new PartieSaisie(jeu.getId(), null, null, List.of(marie.getId()), null,
                List.of(marie.getId()), null, null, null, null, "Enquête 1"));
        jeux.modifier(jeu.getId(), new JeuSaisie(jeu.getNom(), null, null, false, null, EntreeImage.inchangee(), false));

        mvc.perform(post("/parties/" + partie.getId()).with(csrf()).param("jeuId", jeu.getId().toString())
                .param("joueurIds", marie.getId().toString()).param("gagnantIds", marie.getId().toString()))
                .andExpect(status().is3xxRedirection());

        assertThat(parties.detail(partie.getId()).partie().getVariante()).isEqualTo("Enquête 1");
    }

    @Test
    void refuseUneVarianteDePlusDeDeuxCentsCaracteres() throws Exception {
        Jeu jeu = jeux.creer(new JeuSaisie(unique("Arkham"), null, null, false, null, EntreeImage.inchangee(), true));
        Joueur marie = joueur();

        mvc.perform(creation(jeu, marie).param("gagnantIds", marie.getId().toString())
                .param("variante", "x".repeat(201))).andExpect(status().isOk())
                .andExpect(content().string(containsString("200 caractères")));
    }

    @Test
    void apercuDuJeuChoisiDansLeFormulaireEtLeFragment() throws Exception {
        Jeu catan = jeu(false);

        mvc.perform(get("/parties/resultat?jeuId=" + catan.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"apercu-jeu\"")))
                .andExpect(content().string(containsString("hx-swap-oob=\"true\"")))
                .andExpect(content().string(containsString(catan.getNom())));
        mvc.perform(get("/parties/resultat")).andExpect(status().isOk())
                .andExpect(content().string(containsString("apercu-jeu vide")));
        mvc.perform(get("/parties/nouveau?jeu=" + catan.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"apercu-jeu\"")))
                .andExpect(content().string(containsString("class=\"nom-jeu\">" + catan.getNom())));
        mvc.perform(get("/parties/nouveau")).andExpect(status().isOk())
                .andExpect(content().string(containsString("apercu-jeu vide")));
    }

    @Test
    void proposeLesLieuxDejaSaisis() throws Exception {
        Jeu catan = jeu(false);
        Joueur marie = joueur();
        String lieu = "Lieu" + UUID.randomUUID().toString().substring(0, 6);
        parties.declarer(new PartieSaisie(catan.getId(), null, lieu + " de Marie", List.of(marie.getId()), null,
                List.of(marie.getId()), null, null, null, null));

        mvc.perform(get("/parties/lieux?lieu=" + lieu.substring(0, 8))).andExpect(status().isOk())
                .andExpect(content().string(containsString("<datalist id=\"lieux\"")))
                .andExpect(content().string(containsString(lieu + " de Marie")));
        mvc.perform(get("/parties/lieux?lieu=")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<option"))));
    }

    @Test
    void declareUnePartieEtLaListe() throws Exception {
        Jeu catan = jeu(false);
        Joueur marie = joueur();
        Joueur paul = joueur();

        mvc.perform(creation(catan, marie, paul).param("date", "2026-05-02").param("lieu", "Chez Marie")
                .param("gagnantIds", String.valueOf(marie.getId()))
                .param("scores[" + marie.getId() + "]", "10").param("scores[" + paul.getId() + "]", ""))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/parties"));

        mvc.perform(get("/parties?jeu=" + catan.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Chez Marie")))
                .andExpect(content().string(containsString(marie.getNom())))
                .andExpect(content().string(containsString("02/05/2026")));
    }

    @Test
    void declareUnePartieCooperativeEtDesEquipes() throws Exception {
        Jeu pandemic = jeu(true);
        Jeu catan = jeu(false);
        Joueur marie = joueur();
        Joueur paul = joueur();

        mvc.perform(creation(pandemic, marie, paul).param("resultat", "VICTOIRE"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(creation(catan, marie, paul)
                .param("equipeDe[" + marie.getId() + "]", "1").param("equipeDe[" + paul.getId() + "]", "2")
                .param("equipeNoms[1]", "Les Rouges").param("equipesGagnantes", "1"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/parties?jeu=" + pandemic.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Victoire collective")));
        mvc.perform(get("/parties?jeu=" + catan.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("(Les Rouges)")));
    }

    @Test
    void afficheLesErreursDeValidation() throws Exception {
        Jeu catan = jeu(false);
        Joueur marie = joueur();

        mvc.perform(creation(catan, marie)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Désignez au moins un gagnant")));
        mvc.perform(post("/parties").with(csrf()).param("jeuId", String.valueOf(catan.getId())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("au moins un joueur")));
        mvc.perform(creation(catan, marie).param("gagnantIds", String.valueOf(marie.getId()))
                .param("date", LocalDate.now().plusDays(3).toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("futur")));
        mvc.perform(creation(catan, marie).param("gagnantIds", String.valueOf(marie.getId()))
                .param("scores[" + marie.getId() + "]", "abc"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Score invalide")));
        mvc.perform(post("/parties").with(csrf())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Choisissez un jeu")));
    }

    @Test
    void refuseUneRequeteModifianteSansJetonCsrf() throws Exception {
        mvc.perform(post("/parties").param("jeuId", "1")).andExpect(status().isForbidden());
        mvc.perform(post("/parties/1/supprimer")).andExpect(status().isForbidden());
    }

    @Test
    void modifieUnePartieDepuisLeFormulairePrerempli() throws Exception {
        Jeu catan = jeu(false);
        Joueur marie = joueur();
        Joueur paul = joueur();
        Partie partie = parties.declarer(new PartieSaisie(catan.getId(), LocalDate.of(2026, 5, 2), "Salon",
                List.of(marie.getId(), paul.getId()), null, List.of(marie.getId()), null, null, null, null));

        mvc.perform(get("/parties/" + partie.getId() + "/modifier")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Salon")))
                .andExpect(content().string(containsString("value=\"2026-05-02\"")));
        mvc.perform(post("/parties/" + partie.getId()).with(csrf()).param("jeuId", String.valueOf(catan.getId()))
                .param("date", "2026-05-02").param("lieu", "Terrasse")
                .param("joueurIds", String.valueOf(marie.getId()), String.valueOf(paul.getId()))
                .param("gagnantIds", String.valueOf(paul.getId())))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/parties/" + partie.getId()).with(csrf()).param("jeuId", String.valueOf(catan.getId()))
                .param("joueurIds", String.valueOf(marie.getId())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Désignez au moins un gagnant")));

        assertThat(parties.detail(partie.getId()).partie().getLieu()).isEqualTo("Terrasse");
        assertThat(parties.detail(partie.getId()).participations()).filteredOn(Participation::isGagnant)
                .extracting(p -> p.getJoueur().getId()).containsExactly(paul.getId());
    }

    @Test
    void demandeUneConfirmationPuisSupprimeLaPartie() throws Exception {
        Jeu catan = jeu(false);
        Joueur marie = joueur();
        Partie partie = parties.declarer(new PartieSaisie(catan.getId(), null, null, List.of(marie.getId()), null,
                List.of(marie.getId()), null, null, null, null));

        mvc.perform(get("/parties/" + partie.getId() + "/suppression")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmer la suppression")));
        mvc.perform(post("/parties/" + partie.getId() + "/supprimer").with(csrf()))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/parties/" + partie.getId() + "/modifier")).andExpect(status().isNotFound());
    }

    @Test
    void repond404PourUnePartieInconnue() throws Exception {
        mvc.perform(get("/parties/999999/modifier")).andExpect(status().isNotFound());
        mvc.perform(get("/parties/999999/suppression")).andExpect(status().isNotFound());
        mvc.perform(post("/parties/999999/supprimer").with(csrf())).andExpect(status().isNotFound());
    }
}
