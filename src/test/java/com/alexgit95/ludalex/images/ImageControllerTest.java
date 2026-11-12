package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:images;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class ImageControllerTest {

    private static final Path VOLUME = creerVolume();

    @DynamicPropertySource
    static void volume(DynamicPropertyRegistry registre) {
        registre.add("ludalex.images.repertoire", VOLUME::toString);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StockageImages stockage;

    @Test
    void serviUneImageDuVolumeAvecSonTypeEtUnCache() throws Exception {
        ImageSource source = stockage.enregistrer(ImagesDeTest.png(4, 4, Color.RED));

        mvc.perform(get("/images/" + source.getReference()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=31536000")));
        assertThat(Files.exists(VOLUME.resolve(source.getReference()))).isTrue();
    }

    @Test
    void repond404PourUneImageInconnue() throws Exception {
        mvc.perform(get("/images/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
        mvc.perform(get("/images/inconnu.png")).andExpect(status().isNotFound());
    }

    @Test
    void neSertAucunFichierHorsDuVolume() throws Exception {
        Files.writeString(VOLUME.getParent().resolve("secret.png"), "secret");

        mvc.perform(get("/images/..%2Fsecret.png")).andExpect(status().is4xxClientError());
        mvc.perform(get("/images/..%2F..%2Fapplication.properties")).andExpect(status().is4xxClientError());
        mvc.perform(get("/images/secret.png")).andExpect(status().isNotFound());
    }

    private static Path creerVolume() {
        try {
            return Files.createTempDirectory("ludalex-images-test").resolve("volume");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
