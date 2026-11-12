package com.alexgit95.ludalex.importmyludo;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** {@code dureeSession} : durée de conservation de l'analyse ou du rapport ; {@code lignesMax} : lignes de détail affichées. */
@ConfigurationProperties(prefix = "ludalex.import-myludo")
public record ImportMyLudoProperties(
        @DefaultValue("30m") Duration dureeSession,
        @DefaultValue("200") int lignesMax) {
}
