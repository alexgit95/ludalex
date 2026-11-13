package com.alexgit95.ludalex.affichage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuSaisie;
import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurService;
import com.alexgit95.ludalex.parties.FinSaisie;
import com.alexgit95.ludalex.parties.LancementSaisie;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieEnCoursService;
import com.alexgit95.ludalex.parties.PartieRepository;
import com.alexgit95.ludalex.parties.PartieService;
import com.alexgit95.ludalex.parties.ResultatCooperatif;
import com.alexgit95.ludalex.parties.TokenFinService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:affichageweb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class AffichageControllerTest {

    private static final Pattern SIGNATURE = Pattern.compile("name=\"signature\" value=\"([^\"]+)\"");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PartieEnCoursService enCours;

    @Autowired
    private PartieService parties;

    @Autowired
    private TokenFinService tokens;

    @Autowired
    private QrFinPartie qr;

    @Autowired
    private JeuService jeux;

    @Autowired
    private JoueurService joueurs;

    @Autowired
    private PartieRepository repertoire;

    @BeforeEach
    void viderLEcran() {
        enCours.active().ifPresent(vue -> parties.supprimer(vue.partie().getId()));
        repertoire.terminesDepuis(java.time.Instant.now().minusSeconds(60), PageRequest.of(0, 50))
                .forEach(p -> parties.supprimer(p.getId()));
    }

    private Jeu jeu(String nom, boolean cooperatif) {
        return jeux.creer(new JeuSaisie(nom + "-" + UUID.randomUUID(), null, null, cooperatif, null,
                EntreeImage.inchangee()));
    }

    private Joueur joueur(String nom) {
        return joueurs.creer(nom + "-" + UUID.randomUUID(), EntreeImage.inchangee(), EntreeImage.inchangee());
    }

    private Partie lancer(Jeu jeu, Joueur... participants) {
        return enCours.lancer(new LancementSaisie(jeu.getId(),
                java.util.Arrays.stream(participants).map(Joueur::getId).toList(), null, null));
    }

    private String page() throws Exception {
        return mvc.perform(get("/affichage")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
    }

    private String signature(String html) {
        Matcher m = SIGNATURE.matcher(html);
        assertThat(m.find()).as("signature présente").isTrue();
        return m.group(1);
    }

    @Test
    void montreLeDiaporamaQuandAucunePartieNEstActive() throws Exception {
        String html = mvc.perform(get("/affichage")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"diaporama\"")))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("data-images=\"/diaporama/chateau.svg|/diaporama/menhirs.svg|"
                + "/diaporama/noeud-celtique.svg|/diaporama/village.svg\"")
                .contains("/js/htmx.min.js").contains("/js/affichage.js").contains("hx-get=\"/affichage/etat\"")
                .contains("hx-trigger=\"every 3s\"").doesNotContain("<svg");
    }

    @Test
    void montreLaPartieEnCoursAvecUnQrCode() throws Exception {
        Jeu jeu = jeu("Catan", false);
        Joueur anne = joueur("Anne");
        Joueur marc = joueur("Marc");
        lancer(jeu, anne, marc);

        String html = page();

        assertThat(html).contains(jeu.getNom()).contains(anne.getNom()).contains(marc.getNom())
                .contains("<svg").contains("Scannez pour terminer la partie").contains("class=\"vs\"")
                .contains("/icons/defaut.svg").doesNotContain("data-images").doesNotContain("fond-jeu");
    }

    @Test
    void remplaceLeVsParEnsemblePourUnJeuCooperatif() throws Exception {
        lancer(jeu("Pandemic", true), joueur("Anne"), joueur("Marc"));

        assertThat(page()).contains("Ensemble").doesNotContain("class=\"vs\"");
    }

    @Test
    void neMontrePasDeVsPourUnJoueurSeul() throws Exception {
        lancer(jeu("Solitaire", false), joueur("Anne"));

        assertThat(page()).doesNotContain("class=\"vs\"").doesNotContain("Ensemble");
    }

    @Test
    void metUnVsEntreChaquePaireDeJoueursAuDelaDeDeux() throws Exception {
        lancer(jeu("Catan", false), joueur("Anne"), joueur("Marc"), joueur("Léa"));

        assertThat(page().split("class=\"vs\"", -1)).hasSize(3);
    }

    @Test
    void chaqueChargementDeLaPageGenereUnNouveauTokenEtInvalideLAncien() throws Exception {
        Partie partie = lancer(jeu("Catan", false), joueur("Anne"));

        page();
        String premier = qr.courant(partie.getId()).orElseThrow();
        assertThat(tokens.verifier(premier)).isPresent();
        page();
        String second = qr.courant(partie.getId()).orElseThrow();

        assertThat(second).isNotEqualTo(premier);
        assertThat(tokens.verifier(premier)).isEmpty();
        assertThat(tokens.verifier(second)).isPresent();
    }

    @Test
    void repondVideTantQueLEtatNeChangeEtNeRegenerePasLeToken() throws Exception {
        Partie partie = lancer(jeu("Catan", false), joueur("Anne"));
        String signature = signature(page());
        String token = qr.courant(partie.getId()).orElseThrow();

        mvc.perform(get("/affichage/etat").param("signature", signature)).andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/affichage/etat").param("signature", signature)).andExpect(status().isNoContent());

        assertThat(qr.courant(partie.getId())).contains(token);
        assertThat(tokens.verifier(token)).isPresent();
    }

    @Test
    void renvoieLeFragmentQuandLaSignatureEstPerimeeOuAbsente() throws Exception {
        lancer(jeu("Catan", false), joueur("Anne"));

        mvc.perform(get("/affichage/etat").param("signature", "perimee")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"zone-affichage\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<html"))));
        mvc.perform(get("/affichage/etat")).andExpect(status().isOk());
    }

    @Test
    void passeDeLaVeilleALaPartieDesQuElleEstLancee() throws Exception {
        String signatureVeille = signature(page());
        Jeu jeu = jeu("Catan", false);
        lancer(jeu, joueur("Anne"));

        mvc.perform(get("/affichage/etat").param("signature", signatureVeille)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(jeu.getNom())));
    }

    @Test
    void annonceLePauseSansQrCodeEtDesigneLeJoueurSuivant() throws Exception {
        Joueur anne = joueur("Anne");
        Joueur marc = joueur("Marc");
        Partie partie = lancer(jeu("Catan", false), anne, marc);
        String signature = signature(page());
        String token = qr.courant(partie.getId()).orElseThrow();

        enCours.pauser(partie.getId(), marc.getId());

        String fragment = mvc.perform(get("/affichage/etat").param("signature", signature))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(fragment).contains("Partie en pause").contains(marc.getNom()).contains("de jouer")
                .doesNotContain("<svg").doesNotContain("Scannez");
        assertThat(tokens.verifier(token)).isEmpty();
    }

    @Test
    void celebreLeVainqueurApresLaFinParToken() throws Exception {
        Joueur anne = joueur("Anne");
        Joueur marc = joueur("Marc");
        Partie partie = lancer(jeu("Catan", false), anne, marc);
        String signature = signature(page());
        String token = qr.courant(partie.getId()).orElseThrow();

        enCours.terminerViaToken(token, new FinSaisie(List.of(anne.getId()), null, null, null));

        String fragment = mvc.perform(get("/affichage/etat").param("signature", signature))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(fragment).contains("Victoire !").contains("vainqueur anime").contains(anne.getNom())
                .doesNotContain(marc.getNom()).doesNotContain("<svg");
    }

    @Test
    void annonceUneDefaiteCooperative() throws Exception {
        Joueur anne = joueur("Anne");
        Partie partie = lancer(jeu("Pandemic", true), anne);
        String signature = signature(page());
        String token = qr.courant(partie.getId()).orElseThrow();

        enCours.terminerViaToken(token, new FinSaisie(List.of(), null, null, ResultatCooperatif.DEFAITE));

        String fragment = mvc.perform(get("/affichage/etat").param("signature", signature))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(fragment).contains("Défaite collective").doesNotContain("vainqueur anime");
    }
}
