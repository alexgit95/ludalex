package com.alexgit95.ludalex.images;

import java.util.Optional;

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
