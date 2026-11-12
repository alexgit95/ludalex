package com.alexgit95.ludalex.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Vérifie la charte graphique : contrastes lisibles, aucune ressource externe, aucune couleur en dur. */
class CharteGraphiqueTest {

    private static final Path STATIC = Path.of("src/main/resources/static");
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Pattern VARIABLE = Pattern.compile("--([a-z-]+):\\s*(#[0-9a-fA-F]{6})");

    private static String lire(Path chemin) throws IOException {
        return Files.readString(chemin, StandardCharsets.UTF_8);
    }

    private static Map<String, String> palette() throws IOException {
        String css = lire(STATIC.resolve("css/ludalex.css"));
        String racine = css.substring(css.indexOf(":root"), css.indexOf('}', css.indexOf(":root")));
        Map<String, String> couleurs = new HashMap<>();
        Matcher m = VARIABLE.matcher(racine);
        while (m.find()) {
            couleurs.put(m.group(1), m.group(2));
        }
        return couleurs;
    }

    private static double luminance(String hex) {
        double[] canaux = new double[3];
        for (int i = 0; i < 3; i++) {
            double c = Integer.parseInt(hex.substring(1 + 2 * i, 3 + 2 * i), 16) / 255.0;
            canaux[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * canaux[0] + 0.7152 * canaux[1] + 0.0722 * canaux[2];
    }

    private static double contraste(String a, String b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    @Test
    void lesCouplesTexteFondRespectentLeContrasteAA() throws IOException {
        Map<String, String> p = palette();
        String[][] couples = {
            {"encre", "fond"}, {"encre", "surface"}, {"encre", "parchemin"}, {"encre", "parchemin-fonce"},
            {"encre-douce", "fond"}, {"encre-douce", "surface"}, {"encre-douce", "parchemin"},
            {"lierre", "fond"}, {"lierre", "surface"}, {"lierre-fonce", "parchemin-fonce"},
            {"fond", "lierre"}, {"fond", "lierre-fonce"},
            {"erreur", "fond"}, {"erreur", "surface"}, {"erreur", "parchemin"},
        };
        for (String[] couple : couples) {
            assertThat(p).containsKeys(couple[0], couple[1]);
            assertThat(contraste(p.get(couple[0]), p.get(couple[1])))
                    .as("contraste %s sur %s", couple[0], couple[1])
                    .isGreaterThanOrEqualTo(4.5);
        }
    }

    @Test
    void lesElementsGraphiquesDeFocusRestentVisibles() throws IOException {
        Map<String, String> p = palette();
        assertThat(contraste(p.get("or"), p.get("fond"))).isGreaterThanOrEqualTo(3.0);
        assertThat(contraste(p.get("or-clair"), p.get("nuit"))).isGreaterThanOrEqualTo(3.0);
    }

    @Test
    void aucuneRessourceExterneDansLesStylesEtGabarits() throws IOException {
        Pattern externe = Pattern.compile("(@import|url\\(\\s*['\"]?(https?:)?//|(href|src)=\"(https?:)?//)", Pattern.CASE_INSENSITIVE);
        try (Stream<Path> fichiers = Stream.concat(Files.walk(TEMPLATES), Files.walk(STATIC.resolve("css")))) {
            for (Path f : fichiers.filter(Files::isRegularFile).toList()) {
                assertThat(externe.matcher(lire(f)).find()).as("ressource externe dans %s", f).isFalse();
            }
        }
    }

    @Test
    void lesFeuillesNeContiennentAucuneCouleurEnDurHorsVariables() throws IOException {
        Pattern hex = Pattern.compile("#[0-9a-fA-F]{3,8}\\b");
        String ludalex = lire(STATIC.resolve("css/ludalex.css"));
        String horsRacine = ludalex.substring(ludalex.indexOf('}', ludalex.indexOf(":root")));
        assertThat(hex.matcher(horsRacine).find()).as("couleur en dur dans ludalex.css").isFalse();
        assertThat(hex.matcher(lire(STATIC.resolve("css/affichage.css"))).find())
                .as("couleur en dur dans affichage.css").isFalse();
    }

    @Test
    void lesDecorsExistentEtSontLegers() throws IOException {
        for (String nom : new String[] {"parchemin.svg", "lierre-coin.svg", "lierre-separateur.svg"}) {
            Path decor = STATIC.resolve("decor").resolve(nom);
            assertThat(decor).exists();
            assertThat(Files.size(decor)).as(nom).isLessThan(4096);
            assertThat(lire(STATIC.resolve("css/ludalex.css"))).contains("/decor/" + nom);
        }
    }

    @Test
    void laPwaUtiliseLesCouleursDeLaCharte() throws IOException {
        Map<String, String> p = palette();
        String manifeste = lire(STATIC.resolve("manifest.webmanifest"));
        assertThat(manifeste).contains(p.get("fond")).contains(p.get("parchemin"));
        assertThat(lire(STATIC.resolve("sw.js"))).contains("/decor/");
        assertThat(lire(TEMPLATES.resolve("fragments/base.html"))).contains(p.get("parchemin"));
    }
}
