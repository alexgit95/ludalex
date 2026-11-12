package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class FormatImageTest {

    @Test
    void reconnaitLesQuatreFormatsParLeurSignature() {
        assertThat(FormatImage.detecter(ImagesDeTest.png(2, 2, Color.RED))).contains(FormatImage.PNG);
        assertThat(FormatImage.detecter(ImagesDeTest.jpeg(2, 2, Color.RED))).contains(FormatImage.JPEG);
        assertThat(FormatImage.detecter("GIF89a-donnees".getBytes())).contains(FormatImage.GIF);
        assertThat(FormatImage.detecter("GIF87a-donnees".getBytes())).contains(FormatImage.GIF);
        assertThat(FormatImage.detecter(ImagesDeTest.WEBP_SANS_PERTE_1X1)).contains(FormatImage.WEBP);
    }

    @Test
    void refuseTout_ce_QuiNEstPasUneImageAutorisee() {
        assertThat(FormatImage.detecter("<svg></svg>".getBytes())).isEmpty();
        assertThat(FormatImage.detecter("MZ executable".getBytes())).isEmpty();
        assertThat(FormatImage.detecter("RIFF....WAVE".getBytes())).isEmpty();
        assertThat(FormatImage.detecter(new byte[0])).isEmpty();
        assertThat(FormatImage.detecter(null)).isEmpty();
    }

    @Test
    void deduitLeFormatDuNomDeFichier() {
        assertThat(FormatImage.depuisNom("a.png")).contains(FormatImage.PNG);
        assertThat(FormatImage.depuisNom("a.jpg")).contains(FormatImage.JPEG);
        assertThat(FormatImage.depuisNom("a.exe")).isEmpty();
    }
}
