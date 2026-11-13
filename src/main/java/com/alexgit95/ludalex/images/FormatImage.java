package com.alexgit95.ludalex.images;

import java.util.Optional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Formats d'image autorisés, reconnus à leur signature et non à l'extension. */
public enum FormatImage {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg"),
    GIF("gif", "image/gif"),
    WEBP("webp", "image/webp");

    private final String extension;
    private final String typeMime;

    FormatImage(String extension, String typeMime) {
        this.extension = extension;
        this.typeMime = typeMime;
    }

    public String extension() {
        return extension;
    }

    public String typeMime() {
        return typeMime;
    }

    public static Optional<FormatImage> detecter(byte[] octets) {
        if (octets == null) {
            return Optional.empty();
        }
        if (commence(octets, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        if (commence(octets, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        if (commence(octets, 'G', 'I', 'F', '8') && octets.length > 5
                && (octets[4] == '7' || octets[4] == '9') && octets[5] == 'a') {
            return Optional.of(GIF);
        }
        if (octets.length >= 12 && commence(octets, 'R', 'I', 'F', 'F')
                && octets[8] == 'W' && octets[9] == 'E' && octets[10] == 'B' && octets[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    public static Optional<FormatImage> depuisNom(String nomFichier) {
        for (FormatImage format : values()) {
            if (nomFichier.endsWith("." + format.extension)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    public static FormatImage validerFichier(Path fichier) {
        try (var flux = Files.newInputStream(fichier)) {
            FormatImage format = detecter(flux.readNBytes(12))
                    .orElseThrow(() -> new CopieRefuseeException("Format d'image non autorise"));
            try (var image = ImageIO.createImageInputStream(fichier.toFile())) {
                var lecteurs = ImageIO.getImageReaders(image);
                if (!lecteurs.hasNext()) {
                    throw new CopieRefuseeException("Image indecodable");
                }
                var lecteur = lecteurs.next();
                try {
                    lecteur.setInput(image, true, true);
                    int largeur = lecteur.getWidth(0);
                    int hauteur = lecteur.getHeight(0);
                    if (largeur <= 0 || hauteur <= 0 || (long) largeur * hauteur > 50_000_000) {
                        throw new CopieRefuseeException("Dimensions d'image non autorisees");
                    }
                    var parametres = lecteur.getDefaultReadParam();
                    int facteur = Math.max(1, (Math.max(largeur, hauteur) + 255) / 256);
                    parametres.setSourceSubsampling(facteur, facteur, 0, 0);
                    var miniature = lecteur.read(0, parametres);
                    if (miniature == null) {
                        throw new CopieRefuseeException("Image indecodable");
                    }
                    miniature.flush();
                } finally {
                    lecteur.dispose();
                }
            }
            return format;
        } catch (IOException | IllegalArgumentException exception) {
            throw new CopieRefuseeException("Image indecodable");
        }
    }

    private static boolean commence(byte[] octets, int... signature) {
        if (octets.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((octets[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
