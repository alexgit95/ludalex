package com.alexgit95.ludalex.statistiques;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statsweb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class StatistiquesControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JeuService jeux;

    @Autowired
    private JoueurService joueurs;

    @Autowired
    private PartieService parties;

    private Jeu jeu() {
        return jeux.creer(new JeuSaisie("Jeu-" + UUID.randomUUID(), null, null, false, null, EntreeImage.inchangee()));
    }

    private Joueur joueur(String avatar) {
        return joueurs.creer("Joueur-" + UUID.randomUUID(),
                new EntreeImage(null, avatar, false), EntreeImage.inchangee());
    }

    private void partie(Jeu jeu, List<Joueur> participants, Joueur gagnant, String lieu) {
        parties.declarer(new PartieSaisie(jeu.getId(), null, lieu, participants.stream().map(Joueur::getId).toList(),
                null, List.of(gagnant.getId()), null, null, null, null));
    }

    @Test
    void afficheClassementTauxChampionEtDernieresParties() throws Exception {
        Jeu catan = jeu();
        Joueur marie = joueur("https://exemple.org/marie.png");
        Joueur paul = joueur(null);
        partie(catan, List.of(marie, paul), marie, "Chez Marie");
        partie(catan, List.of(marie, paul), marie, "Au bureau");
        partie(catan, List.of(marie, paul), paul, "Au salon");

        mvc.perform(get("/jeux/" + catan.getId() + "/statistiques")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Champion")))
                .andExpect(content().string(containsString(marie.getNom())))
                .andExpect(content().string(containsString("67 %")))
                .andExpect(content().string(containsString("33 %")))
                .andExpect(content().string(containsString("Dernières parties")))
                .andExpect(content().string(containsString("Chez Marie")))
                .andExpect(content().string(containsString("https://exemple.org/marie.png")));
    }

    @Test
    void afficheUnMessageSansPartie() throws Exception {
        mvc.perform(get("/jeux/" + jeu().getId() + "/statistiques")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Aucune partie jouée")))
                .andExpect(content().string(not(containsString("Dernières parties"))));
    }

    @Test
    void affichePluriellementLesChampionsALegalite() throws Exception {
        Jeu catan = jeu();
        Joueur marie = joueur(null);
        Joueur paul = joueur(null);
        partie(catan, List.of(marie, paul), marie, "A");
        partie(catan, List.of(marie, paul), paul, "B");

        mvc.perform(get("/jeux/" + catan.getId() + "/statistiques")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Champions")));
    }

    @Test
    void repond404PourUnJeuInconnu() throws Exception {
        mvc.perform(get("/jeux/999999/statistiques")).andExpect(status().isNotFound());
    }

    @Test
    void chargeLesStatistiquesDepuisLaFicheDuJeu() throws Exception {
        Jeu catan = jeu();

        mvc.perform(get("/jeux/" + catan.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("/jeux/" + catan.getId() + "/statistiques")))
                .andExpect(content().string(containsString("hx-trigger=\"load\"")));
    }

    @Test
    void affichePlusieursAvatarsDeChampionsSurLaVignetteEtAucunSansVictoire() throws Exception {
        Jeu avecEgalite = jeu();
        Jeu sansPartie = jeu();
        String uniq = UUID.randomUUID().toString();
        Joueur marie = joueur("https://exemple.org/" + uniq + "-marie.png");
        Joueur paul = joueur("https://exemple.org/" + uniq + "-paul.png");
        partie(avecEgalite, List.of(marie, paul), marie, "A");
        partie(avecEgalite, List.of(marie, paul), paul, "B");

        mvc.perform(get("/jeux")).andExpect(status().isOk())
                .andExpect(content().string(containsString(uniq + "-marie.png")))
                .andExpect(content().string(containsString(uniq + "-paul.png")))
                .andExpect(content().string(containsString(sansPartie.getNom())));
    }

    @Test
    void ajouteUneDefaiteSansChampionDansLaListe() throws Exception {
        Jeu pandemic = jeux.creer(new JeuSaisie("Coop-" + UUID.randomUUID(), null, null, true, null,
                EntreeImage.inchangee()));
        Joueur marie = joueur(null);
        parties.declarer(new PartieSaisie(pandemic.getId(), null, null, List.of(marie.getId()), null, null, null, null,
                null, com.alexgit95.ludalex.parties.ResultatCooperatif.DEFAITE));

        mvc.perform(get("/jeux/" + pandemic.getId() + "/statistiques")).andExpect(status().isOk())
                .andExpect(content().string(containsString("0 %")))
                .andExpect(content().string(not(containsString("Champion"))));
    }
}
