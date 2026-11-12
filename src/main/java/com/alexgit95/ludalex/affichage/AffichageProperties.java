package com.alexgit95.ludalex.affichage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Réglages de l'écran Affichage. {@code urlPublique} remplace l'origine déduite de la requête pour le QR code
 * (par exemple en développement, quand le téléphone ne voit pas localhost).
 */
@ConfigurationProperties(prefix = "ludalex.affichage")
public record AffichageProperties(
        @DefaultValue("15s") Duration dureeVictoire,
        @DefaultValue("24h") Duration renouvellementToken,
        @DefaultValue("") String urlPublique) {
}
