package com.alexgit95.ludalex.sauvegarde;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Limites de la restauration. {@code repertoireTemporaire} vide : répertoire temporaire du système.
 * Le ratio ne s'applique qu'aux entrées décompressées de plus de 1 Mo.
 */
@ConfigurationProperties(prefix = "ludalex.sauvegarde")
public record SauvegardeProperties(
        @DefaultValue("512MB") DataSize tailleMax,
        @DefaultValue("1GB") DataSize tailleDecompresseeMax,
        @DefaultValue("10000") int entreesMax,
        @DefaultValue("200") int ratioMax,
        @DefaultValue("") String repertoireTemporaire) {
}
