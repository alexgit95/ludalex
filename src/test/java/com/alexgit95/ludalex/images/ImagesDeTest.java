package com.alexgit95.ludalex.images;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

import javax.imageio.ImageIO;

import org.springframework.util.unit.DataSize;

/** Fabrique d'images et de propriétés pour les tests. */
final class ImagesDeTest {

    static final byte[] WEBP_SANS_PERTE_1X1 = java.util.Base64.getDecoder()
            .decode("UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==");

    private ImagesDeTest() {
    }

    static ImagesProperties proprietes(String repertoire) {
        return new ImagesProperties(repertoire, DataSize.ofMegabytes(2), 50_000_000, Duration.ofSeconds(5), 5);
    }

    /** Image unie. */
    static byte[] png(int largeur, int hauteur, Color couleur) {
        BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(couleur);
        g.fillRect(0, 0, largeur, hauteur);
        g.dispose();
        return ecrire(image, "png");
    }

    /** Image 100x100 : 60 % de rouge, 30 % de blanc, 10 % de bleu. */
    static byte[] pngRougeMajoritaire() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(200, 30, 30));
        g.fillRect(0, 0, 100, 60);
        g.setColor(Color.WHITE);
        g.fillRect(0, 60, 100, 30);
        g.setColor(new Color(30, 30, 200));
        g.fillRect(0, 90, 100, 10);
        g.dispose();
        return ecrire(image, "png");
    }

    static byte[] jpeg(int largeur, int hauteur, Color couleur) {
        BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(couleur);
        g.fillRect(0, 0, largeur, hauteur);
        g.dispose();
        return ecrire(image, "jpg");
    }

    private static byte[] ecrire(BufferedImage image, String format) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
