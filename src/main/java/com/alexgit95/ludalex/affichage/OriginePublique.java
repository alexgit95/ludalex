package com.alexgit95.ludalex.affichage;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Origine publique de l'application pour le QR code : réglage explicite, sinon schéma et hôte de la requête. */
@Component
public class OriginePublique {

    private final AffichageProperties proprietes;

    public OriginePublique(AffichageProperties proprietes) {
        this.proprietes = proprietes;
    }

    /**
     * Derrière Cloudflare, les en-têtes {@code X-Forwarded-*} sont déjà appliqués à la requête
     * (stratégie « framework » du profil prod) : le schéma et le domaine sont ceux vus par l'utilisateur.
     */
    public String depuis(HttpServletRequest requete) {
        if (StringUtils.hasText(proprietes.urlPublique())) {
            return proprietes.urlPublique().trim().replaceAll("/+$", "");
        }
        String schema = requete.getScheme();
        String hote = requete.getServerName();
        int port = requete.getServerPort();
        boolean portParDefaut = port <= 0 || ("http".equals(schema) && port == 80)
                || ("https".equals(schema) && port == 443);
        if (hote.contains(":") && !hote.startsWith("[")) {
            hote = "[" + hote + "]";
        }
        return schema + "://" + hote + (portParDefaut ? "" : ":" + port);
    }
}
