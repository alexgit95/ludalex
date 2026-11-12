package com.alexgit95.ludalex.affichage;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/** Images du diaporama de veille : celles du dossier {@code static/diaporama}, énumérées au démarrage. */
@Component
public class DiaporamaImages {

    private static final Logger LOG = LoggerFactory.getLogger(DiaporamaImages.class);
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    private final List<String> urls;

    public DiaporamaImages() {
        this(new PathMatchingResourcePatternResolver());
    }

    DiaporamaImages(ResourcePatternResolver resolveur) {
        this.urls = enumerer(resolveur);
    }

    public List<String> urls() {
        return urls;
    }

    private static List<String> enumerer(ResourcePatternResolver resolveur) {
        try {
            return List.of(resolveur.getResources("classpath*:static/diaporama/*")).stream()
                    .map(Resource::getFilename)
                    .filter(nom -> nom != null && EXTENSIONS.contains(extension(nom)))
                    .sorted()
                    .map(nom -> "/diaporama/" + nom)
                    .toList();
        } catch (IOException e) {
            LOG.warn("Diaporama : dossier illisible ({})", e.getClass().getSimpleName());
            return List.of();
        }
    }

    private static String extension(String nom) {
        int point = nom.lastIndexOf('.');
        return point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT);
    }
}
