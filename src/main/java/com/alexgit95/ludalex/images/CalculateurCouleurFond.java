package com.alexgit95.ludalex.images;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Couleur de fond d'une image : classe de couleur la plus fréquente d'une version réduite.
 * Calculée une seule fois à l'enregistrement, jamais à l'affichage.
 */
@Component
public class CalculateurCouleurFond {

    public static final String NEUTRE = "#3a3a3a";

    private static final Logger LOG = LoggerFactory.getLogger(CalculateurCouleurFond.class);
    private static final Pattern FORMAT = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final int COTE_CIBLE = 64;
    private static final int ALPHA_MIN = 128;
    private static final int QUASI_BLANC = 235;
    private static final int QUASI_NOIR = 20;

    private final long pixelsMax;

    public CalculateurCouleurFond(ImagesProperties proprietes) {
        this.pixelsMax = proprietes.pixelsMax();
    }

    public static boolean estCouleurValide(String couleur) {
        return couleur != null && FORMAT.matcher(couleur).matches();
    }

    /** Couleur au format #RRGGBB, ou la couleur neutre si l'image est illisible. */
    public String calculer(byte[] octets) {
        try {
            BufferedImage reduite = lireReduite(octets);
            return dominante(reduite);
        } catch (Exception | OutOfMemoryError e) {
            LOG.warn("Calcul de la couleur de fond impossible : {}", e.getClass().getSimpleName());
            return NEUTRE;
        }
    }

    private BufferedImage lireReduite(byte[] octets) throws Exception {
        try (ImageInputStream flux = new MemoryCacheImageInputStream(new ByteArrayInputStream(octets))) {
            Iterator<ImageReader> lecteurs = ImageIO.getImageReaders(flux);
            if (!lecteurs.hasNext()) {
                throw new IllegalArgumentException("Format non lisible");
            }
            ImageReader lecteur = lecteurs.next();
            try {
                lecteur.setInput(flux, true, true);
                int largeur = lecteur.getWidth(0);
                int hauteur = lecteur.getHeight(0);
                if ((long) largeur * hauteur > pixelsMax) {
                    throw new IllegalArgumentException("Image trop grande");
                }
                int pas = Math.max(1, Math.max(largeur, hauteur) / COTE_CIBLE);
                ImageReadParam parametres = lecteur.getDefaultReadParam();
                parametres.setSourceSubsampling(pas, pas, 0, 0);
                return lecteur.read(0, parametres);
            } finally {
                lecteur.dispose();
            }
        }
    }

    private String dominante(BufferedImage image) {
        int[] classes = new int[4096];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                if (((argb >>> 24) & 0xFF) < ALPHA_MIN
                        || (r > QUASI_BLANC && g > QUASI_BLANC && b > QUASI_BLANC)
                        || (r < QUASI_NOIR && g < QUASI_NOIR && b < QUASI_NOIR)) {
                    continue;
                }
                classes[((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4)]++;
            }
        }
        int meilleure = -1;
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] > 0 && (meilleure < 0 || classes[i] > classes[meilleure])) {
                meilleure = i;
            }
        }
        if (meilleure < 0) {
            return NEUTRE;
        }
        return String.format("#%02x%02x%02x",
                ((meilleure >> 8) << 4) | 8, (((meilleure >> 4) & 0xF) << 4) | 8, ((meilleure & 0xF) << 4) | 8);
    }
}
