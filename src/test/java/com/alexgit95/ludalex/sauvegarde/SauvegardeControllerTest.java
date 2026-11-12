package com.alexgit95.ludalex.sauvegarde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuRepository;
import com.alexgit95.ludalex.joueurs.JoueurRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sauvegardeweb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "ludalex.images.repertoire=target/test-sauvegarde-web/volume",
        "ludalex.sauvegarde.taille-max=1MB"
})
@AutoConfigureMockMvc
class SauvegardeControllerTest {

    private static final String CONFIRMATION = "X-Confirmation";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JeuRepository jeux;

    @Autowired
    private JoueurRepository joueurs;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager gestionnaire;

    @BeforeEach
    void preparer() throws Exception {
        new TransactionTemplate(gestionnaire).executeWithoutResult(s -> {
            for (String entite : List.of("TokenFinPartie", "Participation", "Equipe", "Partie", "Pret", "Jeu",
                    "Joueur")) {
                em.createQuery("delete from " + entite).executeUpdate();
            }
        });
        jeux.save(new Jeu("Avant"));
        Files.createDirectories(Path.of("target/test-sauvegarde-web/volume"));
    }

    private List<String> nomsDesJeux() {
        return jeux.findAll().stream().map(Jeu::getNom).toList();
    }

    @Test
    void affichePageDeSauvegardeEtLienDepuisLAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/sauvegarde")));
        mvc.perform(get("/admin/sauvegarde")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Télécharger la sauvegarde")))
                .andExpect(content().string(containsString("RESTAURER")))
                .andExpect(content().string(containsString("remplace")))
                .andExpect(content().string(containsString("1</span> Mo maximum")))
                .andExpect(content().string(containsString("/js/sauvegarde.js")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void telechargeUnZipNomme() throws Exception {
        byte[] octets = mvc.perform(get("/admin/sauvegarde/telecharger")).andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.matchesPattern(
                                "attachment; filename=\"ludalex-sauvegarde-\\d{4}-\\d{2}-\\d{2}\\.zip\"")))
                .andReturn().getResponse().getContentAsByteArray();

        List<String> noms = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(octets))) {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                noms.add(e.getName());
            }
        }
        assertThat(noms).contains("manifest.json", "donnees/jeux.json");
    }

    @Test
    void refuseLaRestaurationSansJetonCsrf() throws Exception {
        mvc.perform(post("/admin/sauvegarde/restaurer").header(CONFIRMATION, "RESTAURER")
                .contentType("application/zip").content(ArchiveDeTest.valide().octets()))
                .andExpect(status().isForbidden());

        assertThat(nomsDesJeux()).containsExactly("Avant");
    }

    @Test
    void refuseLaRestaurationSansLaConfirmationDemandee() throws Exception {
        for (String saisie : new String[] { null, "", "restaurer", "OUI" }) {
            var requete = post("/admin/sauvegarde/restaurer").with(csrf()).contentType("application/zip")
                    .content(ArchiveDeTest.valide().octets());
            if (saisie != null) {
                requete = requete.header(CONFIRMATION, saisie);
            }
            mvc.perform(requete).andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("Confirmation manquante")));
        }

        assertThat(nomsDesJeux()).containsExactly("Avant");
    }

    @Test
    void refuseUnZipInvalideSansToucherAuxDonnees() throws Exception {
        mvc.perform(post("/admin/sauvegarde/restaurer").with(csrf()).header(CONFIRMATION, "RESTAURER")
                .contentType("application/zip").content(ArchiveDeTest.valide().avec("../evil", "x").octets()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Sauvegarde refusée, aucune donnée")))
                .andExpect(content().string(containsString("Chemin dangereux")));
        mvc.perform(post("/admin/sauvegarde/restaurer").with(csrf()).header(CONFIRMATION, "RESTAURER")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).content("pas un zip"))
                .andExpect(status().isBadRequest());

        assertThat(nomsDesJeux()).containsExactly("Avant");
    }

    @Test
    void refuseUnFichierPlusGrosQueLaLimite() throws Exception {
        mvc.perform(post("/admin/sauvegarde/restaurer").with(csrf()).header(CONFIRMATION, "RESTAURER")
                .contentType("application/zip").content(new byte[2 * 1024 * 1024]))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().string(containsString("taille maximale")));

        assertThat(nomsDesJeux()).containsExactly("Avant");
    }

    @Test
    void restaureLaSauvegardeEtRemplaceLesDonnees() throws Exception {
        mvc.perform(post("/admin/sauvegarde/restaurer").with(csrf()).header(CONFIRMATION, " RESTAURER ")
                .contentType("application/zip").content(ArchiveDeTest.valide().octets()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Restauration terminée : 2 joueur(s), 1 jeu(x)")))
                .andExpect(header().string("Cache-Control", "no-store"));

        assertThat(nomsDesJeux()).containsExactly("Catan");
        assertThat(joueurs.count()).isEqualTo(2);
    }
}
