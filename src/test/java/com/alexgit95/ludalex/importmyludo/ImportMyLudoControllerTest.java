package com.alexgit95.ludalex.importmyludo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:importmyludoweb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class ImportMyLudoControllerTest {

    private static final Path EXEMPLES = Path.of("docs/exemple-my-ludo");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager gestionnaire;

    @Autowired
    private ServiceImportMyLudo service;

    @Autowired
    private ImportMyLudoController controleur;

    private MockMultipartFile collection;
    private MockMultipartFile parties;

    @BeforeEach
    void preparer() throws IOException {
        new TransactionTemplate(gestionnaire).executeWithoutResult(s -> {
            for (String entite : List.of("TokenFinPartie", "Participation", "Equipe", "Partie", "Pret", "Jeu",
                    "Joueur")) {
                em.createQuery("delete from " + entite).executeUpdate();
            }
        });
        service.abandonner();
        collection = new MockMultipartFile("collection", "collection.json", "application/json", exemple("collection_"));
        parties = new MockMultipartFile("parties", "plays.json", "application/json", exemple("plays_"));
    }

    private static byte[] exemple(String prefixe) throws IOException {
        try (Stream<Path> fichiers = Files.list(EXEMPLES)) {
            return Files.readAllBytes(fichiers.filter(p -> p.getFileName().toString().startsWith(prefixe))
                    .findFirst().orElseThrow());
        }
    }

    private String attendrePage(String texte) throws Exception {
        long fin = System.currentTimeMillis() + 30_000;
        String page = "";
        while (System.currentTimeMillis() < fin) {
            page = mvc.perform(get("/admin/import-myludo")).andExpect(status().isOk()).andReturn().getResponse()
                    .getContentAsString();
            if (page.contains(texte)) {
                return page;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("« " + texte + " » absent de la page : " + page);
    }

    private long compter(String entite) {
        return new TransactionTemplate(gestionnaire).execute(s -> em
                .createQuery("select count(x) from " + entite + " x", Long.class).getSingleResult());
    }

    @Test
    void afficheLeFormulaireDEnvoiDesDeuxFichiers() throws Exception {
        mvc.perform(get("/admin")).andExpect(content().string(containsString("/admin/import-myludo")));
        mvc.perform(get("/admin/import-myludo")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("Choisir les fichiers")))
                .andExpect(content().string(containsString("name=\"collection\"")))
                .andExpect(content().string(containsString("name=\"parties\"")))
                .andExpect(content().string(containsString("multipart/form-data")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void refuseUnEnvoiSansJetonCsrf() throws Exception {
        mvc.perform(multipart("/admin/import-myludo/analyser").file(parties)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/import-myludo/importer")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/import-myludo/annuler")).andExpect(status().isForbidden());
    }

    @Test
    void refuseUneAnalyseSansFichier() throws Exception {
        mvc.perform(multipart("/admin/import-myludo/analyser").with(csrf())).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/import-myludo"))
                .andExpect(flash().attribute("message", containsString("au moins un fichier")));
    }

    @Test
    void parcoursCompletAnalyseCorrespondanceEtImport() throws Exception {
        mvc.perform(multipart("/admin/import-myludo/analyser").file(collection).file(parties).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/import-myludo"));

        String analyse = attendrePage("Vérifier avant d'importer");
        assertThat(analyse).contains("Rien n'a encore été enregistré").contains("collection.json")
                .contains("plays.json").contains("Correspondance des joueurs").contains("Alice")
                .contains("Créer un nouveau joueur").contains("name=\"joueur-0\"").contains("Importer")
                .doesNotContain("hx-trigger=\"every 1s\"");
        assertThat(compter("Partie")).isZero();

        mvc.perform(post("/admin/import-myludo/importer").param("joueur-0", "").with(csrf()))
                .andExpect(status().is3xxRedirection());

        String rapport = attendrePage("Import réussi");
        assertThat(rapport).contains("L'import s'est bien déroulé").contains("partie(s)")
                .doesNotContain("hx-trigger=\"every 1s\"");
        assertThat(compter("Partie")).isPositive();
        assertThat(compter("Joueur")).isEqualTo(7);
    }

    @Test
    void afficheUneErreurDiagnostiquableSansRienEnregistrer() throws Exception {
        MockMultipartFile invalide = new MockMultipartFile("parties", "plays.json", "application/json",
                "[{\"Titre\":\"Zodianimo\",\"Date\":\"pas une date\",\"Joueurs\":[]}]".getBytes());
        mvc.perform(multipart("/admin/import-myludo/analyser").file(invalide).with(csrf()))
                .andExpect(status().is3xxRedirection());

        String page = attendrePage("L'import a échoué");

        assertThat(page).contains("plays.json").contains("Entrée n°1").contains("Champ : Date")
                .contains("date illisible").contains("Aucune donnée n'a été modifiée");
        assertThat(compter("Partie")).isZero();
        mvc.perform(post("/admin/import-myludo/annuler").with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/import-myludo")).andExpect(content().string(containsString("Choisir les fichiers")));
    }

    @Test
    void indiqueQuUneAnalyseOuUnImportEstDejaEnCoursPuisQueLeSuiviEstAJour() throws Exception {
        mvc.perform(get("/admin/import-myludo/progression").param("signature", "perimee"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("id=\"zone-import\"")))
                .andExpect(content().string(containsString("Choisir les fichiers")))
                .andExpect(header().string("Cache-Control", "no-store"));

        mvc.perform(multipart("/admin/import-myludo/analyser").file(parties).with(csrf()));
        attendrePage("Vérifier avant d'importer");

        String signature = service.vue().signature();
        mvc.perform(get("/admin/import-myludo/progression").param("signature", signature))
                .andExpect(status().isNoContent());
        mvc.perform(get("/admin/import-myludo/progression").param("signature", "autre"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Correspondance des joueurs")));
    }

    @Test
    void repondPar413QuandUnFichierEstTropVolumineux() {
        org.springframework.ui.ConcurrentModel modele = new org.springframework.ui.ConcurrentModel();
        org.springframework.mock.web.MockHttpServletResponse reponse = new org.springframework.mock.web.MockHttpServletResponse();

        String vue = controleur.fichierTropVolumineux(modele, reponse);

        assertThat(vue).isEqualTo("admin/import-myludo");
        assertThat(reponse.getStatus()).isEqualTo(413);
        assertThat(modele.get("message")).asString().contains("2 Mo");
        assertThat(new MaxUploadSizeExceededException(1)).isNotNull();
    }
}
