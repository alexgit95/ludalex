package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class CalculateurCouleurFondTest {

    private final CalculateurCouleurFond calculateur = new CalculateurCouleurFond(ImagesDeTest.proprietes("unused"));

    @Test
    void retientLaCouleurLaPlusFrequenteEtNonLaMoyenne() {
        int[] rvb = composantes(calculateur.calculer(ImagesDeTest.pngRougeMajoritaire()));

        assertThat(rvb[0]).isGreaterThan(150);
        assertThat(rvb[1]).isLessThan(80);
        assertThat(rvb[2]).isLessThan(80);
    }

    @Test
    void ignoreLesPixelsTransparents() {
        assertThat(calculateur.calculer(ImagesDeTest.png(20, 20, new Color(0, 0, 0, 0))))
                .isEqualTo(CalculateurCouleurFond.NEUTRE);
    }

    @Test
    void ignoreLeBlancEtLeNoir() {
        assertThat(calculateur.calculer(ImagesDeTest.png(20, 20, Color.WHITE))).isEqualTo(CalculateurCouleurFond.NEUTRE);
        assertThat(calculateur.calculer(ImagesDeTest.png(20, 20, Color.BLACK))).isEqualTo(CalculateurCouleurFond.NEUTRE);
    }

    @Test
    void traiteUneGrandeImageParSousEchantillonnage() {
        byte[] grande = ImagesDeTest.png(3000, 2000, new Color(30, 60, 200));

        int[] rvb = composantes(calculateur.calculer(grande));

        assertThat(rvb[2]).isGreaterThan(150);
        assertThat(rvb[0]).isLessThan(80);
    }

    @Test
    void traiteUnJpeg() {
        int[] rvb = composantes(calculateur.calculer(ImagesDeTest.jpeg(500, 400, new Color(30, 180, 40))));

        assertThat(rvb[1]).isGreaterThan(130);
        assertThat(rvb[0]).isLessThan(90);
    }

    @Test
    void lisUneImageWebp() throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(ImagesDeTest.WEBP_SANS_PERTE_1X1));

        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(1);
        assertThat(CalculateurCouleurFond.estCouleurValide(calculateur.calculer(ImagesDeTest.WEBP_SANS_PERTE_1X1)))
                .isTrue();
    }

    @Test
    void reviensALaCouleurNeutreSiLImageEstIllisible() {
        assertThat(calculateur.calculer("pas une image".getBytes())).isEqualTo(CalculateurCouleurFond.NEUTRE);
        assertThat(calculateur.calculer(new byte[0])).isEqualTo(CalculateurCouleurFond.NEUTRE);
    }

    @Test
    void refuseLesImagesAuxDimensionsExcessives() {
        CalculateurCouleurFond strict = new CalculateurCouleurFond(new ImagesProperties(
                "unused", DataSize.ofMegabytes(2), 100, java.time.Duration.ofSeconds(5), 5));

        assertThat(strict.calculer(ImagesDeTest.png(20, 20, Color.RED))).isEqualTo(CalculateurCouleurFond.NEUTRE);
    }

    @Test
    void valideLeFormatDeCouleur() {
        assertThat(CalculateurCouleurFond.estCouleurValide("#336699")).isTrue();
        assertThat(CalculateurCouleurFond.estCouleurValide("#ABCDEF")).isTrue();
        assertThat(CalculateurCouleurFond.estCouleurValide("336699")).isFalse();
        assertThat(CalculateurCouleurFond.estCouleurValide("#33669")).isFalse();
        assertThat(CalculateurCouleurFond.estCouleurValide("red")).isFalse();
        assertThat(CalculateurCouleurFond.estCouleurValide(null)).isFalse();
    }

    private static int[] composantes(String couleur) {
        assertThat(CalculateurCouleurFond.estCouleurValide(couleur)).isTrue();
        return new int[] { Integer.parseInt(couleur.substring(1, 3), 16), Integer.parseInt(couleur.substring(3, 5), 16),
                Integer.parseInt(couleur.substring(5, 7), 16) };
    }
}
