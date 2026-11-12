package com.alexgit95.ludalex.affichage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

class DiaporamaImagesTest {

    private static Resource fichier(String nom) {
        return new ByteArrayResource(new byte[0]) {
            @Override
            public String getFilename() {
                return nom;
            }
        };
    }

    @Test
    void enumereLesImagesEmbarqueesParOrdreAlphabetique() {
        List<String> urls = new DiaporamaImages().urls();

        assertThat(urls).contains("/diaporama/chateau.svg", "/diaporama/menhirs.svg", "/diaporama/noeud-celtique.svg",
                "/diaporama/village.svg").isSorted();
        assertThat(urls).allMatch(url -> url.startsWith("/diaporama/"));
    }

    @Test
    void ignoreLesFichiersQuiNeSontPasDesImages() throws IOException {
        ResourcePatternResolver resolveur = mock(ResourcePatternResolver.class);
        when(resolveur.getResources("classpath*:static/diaporama/*")).thenReturn(new Resource[] {
                fichier("b.PNG"), fichier("a.jpg"), fichier("LISEZMOI.md"), fichier(".gitkeep"), fichier("c.webp"),
                fichier("sans-extension"), fichier("d.exe") });

        assertThat(new DiaporamaImages(resolveur).urls())
                .containsExactly("/diaporama/a.jpg", "/diaporama/b.PNG", "/diaporama/c.webp");
    }

    @Test
    void neRenvoieRienQuandLeDossierEstVideOuIllisible() throws IOException {
        ResourcePatternResolver vide = mock(ResourcePatternResolver.class);
        when(vide.getResources("classpath*:static/diaporama/*")).thenReturn(new Resource[0]);
        ResourcePatternResolver casse = mock(ResourcePatternResolver.class);
        when(casse.getResources("classpath*:static/diaporama/*")).thenThrow(new IOException("illisible"));

        assertThat(new DiaporamaImages(vide).urls()).isEmpty();
        assertThat(new DiaporamaImages(casse).urls()).isEmpty();
    }

    @Test
    void lesImagesFourniesRestentSobres() throws Exception {
        Path dossier = Paths.get("src/main/resources/static/diaporama");
        try (Stream<Path> fichiers = Files.list(dossier)) {
            for (Path image : fichiers.toList()) {
                assertThat(Files.size(image)).as(image.toString()).isLessThanOrEqualTo(2L * 1024 * 1024);
                if (image.toString().endsWith(".svg")) {
                    DocumentBuilderFactory usine = DocumentBuilderFactory.newInstance();
                    usine.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    usine.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    assertThat(usine.newDocumentBuilder().parse(image.toFile()).getDocumentElement().getTagName())
                            .as(image.toString()).isEqualTo("svg");
                    assertThat(Files.readString(image)).doesNotContain("<script");
                }
            }
        }
    }
}
