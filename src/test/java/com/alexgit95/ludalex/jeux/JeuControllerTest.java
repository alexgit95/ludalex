package com.alexgit95.ludalex.jeux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.alexgit95.ludalex.images.EntreeImage;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jeux;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class JeuControllerTest {

    private static final Path VOLUME = creerVolume();

    @DynamicPropertySource
    static void volume(DynamicPropertyRegistry registre) {
        registre.add("ludalex.images.repertoire", VOLUME::toString);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JeuService jeux;

    @Autowired
    private PretService prets;

    private static String nomUnique() {
        return "Jeu-" + UUID.randomUUID();
    }

    private Jeu jeuExistant() {
        return jeux.creer(new JeuSaisie(nomUnique(), null, null, false, null, EntreeImage.inchangee()));
    }

    private MockMultipartHttpServletRequestBuilder creation(String nom) {
        MockMultipartHttpServletRequestBuilder requete = multipart("/jeux");
        requete.param("nom", nom).with(csrf());
        return requete;
    }

    @Test
    void afficheLaListeEnVignettesAvecLEmprunteur() throws Exception {
        Jeu jeu = jeuExistant();
        prets.preter(jeu.getId(), "Marie", null);

        mvc.perform(get("/jeux")).andExpect(status().isOk())
                .andExpect(content().string(containsString(jeu.getNom())))
                .andExpect(content().string(containsString("Prêté à Marie")))
                .andExpect(content().string(containsString("background-color:#3a3a3a")));
    }

    @Test
    void reserveLaLigneChampionEtExposeLeBurgerAccessible() throws Exception {
        jeuExistant();

        mvc.perform(get("/jeux")).andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"entete-ludotheque\"")))
                .andExpect(content().string(containsString("class=\"champions-vignette\"")))
                .andExpect(content().string(containsString("aria-expanded=\"false\"")))
                .andExpect(content().string(containsString("aria-controls=\"navigation\"")))
                .andExpect(content().string(containsString("id=\"navigation\"")))
                .andExpect(content().string(containsString("href=\"/parties\"")));
    }

    @Test
    void creeUnJeuAvecImageUploadeeEtCouleurCalculee() throws Exception {
        String nom = nomUnique();

        var resultat = mvc.perform(creation(nom)
                .file(new MockMultipartFile("imageFichier", "j.png", "image/png", png(new Color(200, 30, 30))))
                .param("imageMode", "UPLOAD").param("prix", "45.00").param("dateAchat", "2025-03-12")
                .param("cooperatif", "true"))
                .andExpect(status().is3xxRedirection()).andReturn();

        String cible = resultat.getResponse().getRedirectedUrl();
        mvc.perform(get(cible)).andExpect(status().isOk())
                .andExpect(content().string(containsString(nom)))
                .andExpect(content().string(containsString("Coopératif")))
                .andExpect(content().string(containsString("12/03/2025")))
                .andExpect(content().string(containsString("45,00")))
                .andExpect(content().string(containsString("/images/")))
                .andExpect(content().string(containsString("Aucun prêt enregistré")));
        Long id = Long.valueOf(cible.substring(cible.lastIndexOf('/') + 1));
        assertThat(Integer.parseInt(jeux.trouver(id).getCouleurFond().substring(1, 3), 16)).isGreaterThan(150);
    }

    @Test
    void utiliseLaCouleurChoisieManuellement() throws Exception {
        var resultat = mvc.perform(creation(nomUnique()).param("couleurManuelle", "true").param("couleur", "#336699"))
                .andExpect(status().is3xxRedirection()).andReturn();

        String cible = resultat.getResponse().getRedirectedUrl();
        Long id = Long.valueOf(cible.substring(cible.lastIndexOf('/') + 1));
        assertThat(jeux.trouver(id).getCouleurFond()).isEqualTo("#336699");
    }

    @Test
    void ignoreLaCouleurDuFormulaireSiLaCaseNEstPasCochee() throws Exception {
        var resultat = mvc.perform(creation(nomUnique()).param("couleur", "#336699"))
                .andExpect(status().is3xxRedirection()).andReturn();

        String cible = resultat.getResponse().getRedirectedUrl();
        Long id = Long.valueOf(cible.substring(cible.lastIndexOf('/') + 1));
        assertThat(jeux.trouver(id).getCouleurFond()).isEqualTo("#3a3a3a");
    }

    @Test
    void refuseLesSaisiesInvalides() throws Exception {
        mvc.perform(creation(" ")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Le nom est obligatoire")));
        mvc.perform(creation(nomUnique()).param("prix", "-5")).andExpect(status().isOk())
                .andExpect(content().string(containsString("négatif")));
        mvc.perform(creation(nomUnique()).param("prix", "abc")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Prix invalide")));
        mvc.perform(creation(nomUnique()).param("dateAchat", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("futur")));
        mvc.perform(creation(nomUnique())
                .file(new MockMultipartFile("imageFichier", "x.png", "image/png", "texte".getBytes())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Format d&#39;image non autorisé")));
    }

    @Test
    void refuseUneRequeteModifianteSansJetonCsrf() throws Exception {
        mvc.perform(multipart("/jeux").param("nom", nomUnique())).andExpect(status().isForbidden());
        mvc.perform(post("/jeux/1/preter").param("emprunteur", "Marie")).andExpect(status().isForbidden());
    }

    @Test
    void modifieUnJeuEnConservantSaCouleur() throws Exception {
        Jeu jeu = jeux.creer(new JeuSaisie(nomUnique(), null, null, false, "#336699", EntreeImage.inchangee()));
        String nouveauNom = nomUnique();
        MockMultipartHttpServletRequestBuilder requete = multipart("/jeux/" + jeu.getId());
        requete.param("nom", nouveauNom).param("couleur", "#336699").with(csrf());

        mvc.perform(get("/jeux/" + jeu.getId() + "/modifier")).andExpect(status().isOk())
                .andExpect(content().string(containsString(jeu.getNom())));
        mvc.perform(requete).andExpect(status().is3xxRedirection());

        assertThat(jeux.trouver(jeu.getId()).getNom()).isEqualTo(nouveauNom);
        assertThat(jeux.trouver(jeu.getId()).getCouleurFond()).isEqualTo("#336699");
    }

    @Test
    void enregistreEtModifieLIndicateurDeVariantes() throws Exception {
        var creation = mvc.perform(creation(nomUnique()).param("plusieursVariantes", "true"))
                .andExpect(status().is3xxRedirection()).andReturn();
        Long id = Long.valueOf(creation.getResponse().getRedirectedUrl().substring(
                creation.getResponse().getRedirectedUrl().lastIndexOf('/') + 1));

        assertThat(jeux.trouver(id).isPlusieursVariantes()).isTrue();
        mvc.perform(get("/jeux/" + id + "/modifier")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Plusieurs campagnes, enquêtes ou versions")));
        mvc.perform(multipart("/jeux/" + id).param("nom", "Jeu modifié").with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(jeux.trouver(id).isPlusieursVariantes()).isFalse();
    }

    @Test
    void repond404PourUnJeuInconnu() throws Exception {
        mvc.perform(get("/jeux/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/jeux/999999/modifier")).andExpect(status().isNotFound());
        mvc.perform(get("/jeux/999999/pret")).andExpect(status().isNotFound());
    }

    @Test
    void demandeUneConfirmationPuisSupprimeLeJeuEtSonImage() throws Exception {
        Jeu jeu = jeux.creer(new JeuSaisie(nomUnique(), null, null, false, null,
                new EntreeImage(new MockMultipartFile("f", "a.png", "image/png", png(Color.RED)), null, false)));
        String fichier = jeu.getImage().getReference();
        assertThat(Files.exists(VOLUME.resolve(fichier))).isTrue();

        mvc.perform(get("/jeux/" + jeu.getId() + "/suppression")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmer la suppression")));
        mvc.perform(post("/jeux/" + jeu.getId() + "/supprimer").with(csrf())).andExpect(status().is3xxRedirection());

        mvc.perform(get("/jeux/" + jeu.getId())).andExpect(status().isNotFound());
        assertThat(Files.exists(VOLUME.resolve(fichier))).isFalse();
    }

    @Test
    void enregistreUnPretPuisSonRetourEtGardeLHistorique() throws Exception {
        Jeu jeu = jeuExistant();

        mvc.perform(get("/jeux/" + jeu.getId() + "/pret")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Enregistrer le prêt")));
        mvc.perform(post("/jeux/" + jeu.getId() + "/preter").param("emprunteur", "Marie")
                .param("datePret", "2026-05-02").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message", "Prêt enregistré"));
        mvc.perform(get("/jeux/" + jeu.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Prêté à")))
                .andExpect(content().string(containsString("02/05/2026")));

        mvc.perform(get("/jeux/" + jeu.getId() + "/retour")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Date de retour")));
        mvc.perform(post("/jeux/" + jeu.getId() + "/retour").param("dateRetour", "2026-05-20").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("message", "Retour enregistré"));
        mvc.perform(get("/jeux/" + jeu.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ce jeu est disponible")))
                .andExpect(content().string(containsString("20/05/2026")));
    }

    @Test
    void signaleLesErreursDePret() throws Exception {
        Jeu jeu = jeuExistant();
        prets.preter(jeu.getId(), "Paul", null);

        mvc.perform(post("/jeux/" + jeu.getId() + "/preter").param("emprunteur", "Marie").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("erreur", "Ce jeu est déjà prêté"));
        mvc.perform(post("/jeux/" + jeu.getId() + "/retour").param("dateRetour", "2001-01-01").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erreur", "La date de retour ne peut pas précéder la date de prêt"));
    }

    private static byte[] png(Color couleur) throws IOException {
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(couleur);
        g.fillRect(0, 0, 40, 40);
        g.dispose();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, "png", sortie);
        return sortie.toByteArray();
    }

    private static Path creerVolume() {
        try {
            return Files.createTempDirectory("ludalex-jeux-test").resolve("volume");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
