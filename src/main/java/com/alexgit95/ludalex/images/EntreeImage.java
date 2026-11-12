package com.alexgit95.ludalex.images;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** Choix de l'utilisateur pour une image : fichier, adresse web, retrait, ou rien (inchangée). */
public record EntreeImage(MultipartFile fichier, String url, boolean retirer) {

    public static EntreeImage inchangee() {
        return new EntreeImage(null, null, false);
    }

    public boolean aFichier() {
        return fichier != null && !fichier.isEmpty();
    }

    public boolean aUrl() {
        return StringUtils.hasText(url);
    }
}
