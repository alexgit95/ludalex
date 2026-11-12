package com.alexgit95.ludalex.parties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Réglages du lien de fin de partie. {@code enTeteIp} nomme l'en-tête portant l'adresse du client
 * (CF-Connecting-IP derrière Cloudflare) ; vide, l'adresse de la connexion est utilisée.
 */
@ConfigurationProperties(prefix = "ludalex.fin")
public record FinPartieProperties(
        @DefaultValue("7d") Duration validite,
        @DefaultValue("30") int limiteParMinute,
        @DefaultValue("") String enTeteIp) {
}
