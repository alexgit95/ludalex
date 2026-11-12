package com.alexgit95.ludalex.joueurs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:joueurs;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class JoueurAdminControllerTest {

    private static final Path VOLUME = creerVolume();

    @DynamicPropertySource
    static void volume(DynamicPropertyRegistry registre) {
        registre.add("ludalex.images.repertoire", VOLUME::toString);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JoueurService service;

    private static String nomUnique() {
        return "J-" + UUID.randomUUID();
    }

    private MockMultipartHttpServletRequestBuilder formulaireCreation(String nom) {
        MockMultipartHttpServletRequestBuilder requete = multipart("/admin/joueurs");
        requete.param("nom", nom);
        requete.with(csrf());
        return requete;
    }

    @Test
    void afficheLAdministrationEtLaListe() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk()).andExpect(content().string(containsString("Joueurs")));
        mvc.perform(get("/admin/joueurs")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ajouter un joueur")));
    }

    @Test
    void creeUnJoueurAvecUnAvatarUploade() throws Exception {
        String nom = nomUnique();

        mvc.perform(formulaireCreation(nom)
                .file(new MockMultipartFile("avatarFichier", "a.png", "image/png", png()))
                .param("avatarMode", "UPLOAD"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/joueurs"));

        try (Stream<Path> fichiers = Files.list(VOLUME)) {
            assertThat(fichiers.filter(f -> f.toString().endsWith(".png"))).isNotEmpty();
        }
        assertThat(service.selectionnables()).extracting(Joueur::getNom).contains(nom);
        mvc.perform(get("/admin/joueurs")).andExpect(status().isOk());
    }

    @Test
    void creeUnJoueurAvecUneImageDeVictoireParUrl() throws Exception {
        String nom = nomUnique();

        mvc.perform(formulaireCreation(nom).param("victoireMode", "URL").param("victoireUrl", "https://exemple.org/v.png"))
                .andExpect(status().is3xxRedirection());

        Joueur cree = service.selectionnables().stream().filter(j -> j.getNom().equals(nom)).findFirst().orElseThrow();
        assertThat(cree.getImageVictoire().getReference()).isEqualTo("https://exemple.org/v.png");
    }

    @Test
    void refuseUnJoueurSansNom() throws Exception {
        mvc.perform(formulaireCreation(" ")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Le nom est obligatoire")));
    }

    @Test
    void refuseUnNomEnDoublonSansTenirCompteDeLaCasse() throws Exception {
        String nom = nomUnique();
        service.creer(nom, null, null);

        mvc.perform(formulaireCreation(nom.toUpperCase())).andExpect(status().isOk())
                .andExpect(content().string(containsString("déjà")));
    }

    @Test
    void refuseUnFichierQuiNEstPasUneImage() throws Exception {
        mvc.perform(formulaireCreation(nomUnique())
                .file(new MockMultipartFile("avatarFichier", "faux.png", "image/png", "texte".getBytes())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Format d&#39;image non autorisé")));
    }

    @Test
    void refuseUneAdresseWebAuSchemaInterdit() throws Exception {
        mvc.perform(formulaireCreation(nomUnique()).param("avatarMode", "URL").param("avatarUrl", "file:///etc/passwd"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("http")));
    }

    @Test
    void refuseUneRequeteSansJetonCsrf() throws Exception {
        mvc.perform(multipart("/admin/joueurs").param("nom", nomUnique())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/joueurs/1/supprimer")).andExpect(status().isForbidden());
    }

    @Test
    void modifieUnJoueur() throws Exception {
        Joueur joueur = service.creer(nomUnique(), null, null);
        String nouveauNom = nomUnique();

        mvc.perform(get("/admin/joueurs/" + joueur.getId() + "/modifier")).andExpect(status().isOk())
                .andExpect(content().string(containsString(joueur.getNom())));
        MockMultipartHttpServletRequestBuilder requete = multipart("/admin/joueurs/" + joueur.getId());
        requete.param("nom", nouveauNom).with(csrf());
        mvc.perform(requete).andExpect(status().is3xxRedirection());

        assertThat(service.trouverActif(joueur.getId()).getNom()).isEqualTo(nouveauNom);
    }

    @Test
    void repond404PourUnJoueurInconnu() throws Exception {
        mvc.perform(get("/admin/joueurs/999999/modifier")).andExpect(status().isNotFound());
        mvc.perform(get("/admin/joueurs/999999/suppression")).andExpect(status().isNotFound());
    }

    @Test
    void demandeUneConfirmationPuisSupprimeLeJoueurEtSonImage() throws Exception {
        Joueur joueur = service.creer(nomUnique(),
                new EntreeImage(new MockMultipartFile("f", "a.png", "image/png", png()), null, false), null);
        String fichier = joueur.getAvatar().getReference();
        assertThat(Files.exists(VOLUME.resolve(fichier))).isTrue();

        mvc.perform(get("/admin/joueurs/" + joueur.getId() + "/suppression")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmer la suppression")));
        mvc.perform(post("/admin/joueurs/" + joueur.getId() + "/supprimer").with(csrf()))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/admin/joueurs/" + joueur.getId() + "/modifier")).andExpect(status().isNotFound());
        assertThat(Files.exists(VOLUME.resolve(fichier))).isFalse();
    }

    @Test
    void paginelaListe() throws Exception {
        for (int i = 0; i < JoueurService.TAILLE_PAGE + 1; i++) {
            service.creer(nomUnique(), null, null);
        }

        mvc.perform(get("/admin/joueurs?page=1")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Précédent")));
    }

    private static byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 8, 8);
        g.dispose();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(image, "png", sortie);
        return sortie.toByteArray();
    }

    private static Path creerVolume() {
        try {
            return Files.createTempDirectory("ludalex-joueurs-test").resolve("volume");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
