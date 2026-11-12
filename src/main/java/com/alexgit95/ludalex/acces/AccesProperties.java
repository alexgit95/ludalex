package com.alexgit95.ludalex.acces;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

/**
 * Configuration de l'accès : Cloudflare Access en {@code prod}, aucun contrôle en {@code dev}.
 */
@ConfigurationProperties(prefix = "ludalex.acces")
public record AccesProperties(
        @DefaultValue("aucun") Mode mode,
        String teamDomain,
        String audience) {

    public enum Mode {
        CLOUDFLARE, AUCUN
    }

    public String issuer() {
        String domaine = teamDomain.trim();
        domaine = domaine.replaceFirst("^https?://", "").replaceAll("/+$", "");
        return "https://" + domaine;
    }

    public String jwkSetUri() {
        return issuer() + "/cdn-cgi/access/certs";
    }

    /** Refuse un démarrage en prod sans vérification Cloudflare complète. */
    public void verifierPourProd() {
        if (mode != Mode.CLOUDFLARE) {
            throw new IllegalStateException(
                    "Le mode d'accès doit être 'cloudflare' en profil prod (ludalex.acces.mode)");
        }
        if (!StringUtils.hasText(teamDomain)) {
            throw new IllegalStateException("Variable d'environnement obligatoire manquante : CF_ACCESS_TEAM_DOMAIN");
        }
        if (!StringUtils.hasText(audience)) {
            throw new IllegalStateException("Variable d'environnement obligatoire manquante : CF_ACCESS_AUD");
        }
    }
}
