package com.alexgit95.ludalex.affichage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

class CodeQrTest {

    private static final String ADRESSE = "https://ludalex.example.org/fin/" + "Ab3_".repeat(10) + "Ab3";

    /** Redessine la matrice avec sa zone de silence, à l'échelle 6, pour la relire avec un vrai décodeur. */
    private static String decoder(BitMatrix matrice) throws Exception {
        int echelle = 6;
        int marge = 4;
        int largeur = (matrice.getWidth() + 2 * marge) * echelle;
        int[] pixels = new int[largeur * largeur];
        java.util.Arrays.fill(pixels, 0xFFFFFFFF);
        for (int y = 0; y < matrice.getHeight(); y++) {
            for (int x = 0; x < matrice.getWidth(); x++) {
                if (matrice.get(x, y)) {
                    for (int dy = 0; dy < echelle; dy++) {
                        for (int dx = 0; dx < echelle; dx++) {
                            pixels[((y + marge) * echelle + dy) * largeur + (x + marge) * echelle + dx] = 0xFF000000;
                        }
                    }
                }
            }
        }
        BinaryBitmap image = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(largeur, largeur, pixels)));
        return new QRCodeReader().decode(image).getText();
    }

    @Test
    void encodeLAdresseDansUnQrCodeLisible() throws Exception {
        assertThat(decoder(CodeQr.encoder(ADRESSE))).isEqualTo(ADRESSE);
    }

    @Test
    void produitUnSvgFidele() throws Exception {
        BitMatrix matrice = CodeQr.encoder(ADRESSE);
        long modulesNoirs = 0;
        for (int y = 0; y < matrice.getHeight(); y++) {
            for (int x = 0; x < matrice.getWidth(); x++) {
                modulesNoirs += matrice.get(x, y) ? 1 : 0;
            }
        }

        String svg = new CodeQr().svg(ADRESSE);

        int total = matrice.getWidth() + 8;
        assertThat(svg).startsWith("<svg ").endsWith("</svg>").contains("viewBox=\"0 0 " + total + " " + total + "\"");
        Matcher segments = Pattern.compile("M(\\d+) (\\d+)h(\\d+)v1h-\\3z").matcher(svg);
        long noirsDansLeSvg = 0;
        while (segments.find()) {
            int x = Integer.parseInt(segments.group(1)) - 4;
            int y = Integer.parseInt(segments.group(2)) - 4;
            int longueur = Integer.parseInt(segments.group(3));
            for (int i = 0; i < longueur; i++) {
                assertThat(matrice.get(x + i, y)).isTrue();
            }
            noirsDansLeSvg += longueur;
        }
        assertThat(noirsDansLeSvg).isEqualTo(modulesNoirs);
    }

    @Test
    void neContientNiScriptNiLeTexteEncode() {
        String svg = new CodeQr().svg(ADRESSE);

        assertThat(svg).doesNotContain("<script").doesNotContain("onload").doesNotContain("/fin/")
                .doesNotContain("ludalex.example.org");
    }

    @Test
    void genereDesCodesDeTailleCroissanteAvecLeContenu() throws Exception {
        assertThat(CodeQr.encoder(ADRESSE + "x".repeat(60)).getWidth()).isGreaterThan(CodeQr.encoder("a").getWidth());
    }
}
