package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

class StockageCopiesTest {
    @TempDir Path dossier;

    private StockageImages stockage() {
        return new StockageImages(new ImagesProperties(dossier.toString(), DataSize.ofMegabytes(2),
                50_000_000, Duration.ofSeconds(1), 3));
    }

    @Test
    void validePublieEtRestitueUneCopie() throws Exception {
        StockageImages stockage = stockage();
        Path temporaire = stockage.temporaireCopie();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", temporaire.toFile());
        ImageSource copie = stockage.publierCopie(temporaire);
        assertThat(Files.exists(temporaire)).isFalse();
        assertThat(stockage.charger(copie.getReference())).isPresent();
        assertThat(FormatImage.depuisNom(copie.getReference())).contains(FormatImage.PNG);
        assertThat(stockage.charger("../secret.png")).isEmpty();
        assertThat(stockage.charger("00000000-0000-0000-0000-000000000000.png")).isEmpty();
    }

    @Test
    void refuseLesFauxFormatsEtNettoieLesRestes() throws Exception {
        StockageImages stockage = stockage();
        Path temporaire = stockage.temporaireCopie();
        Files.writeString(temporaire, "<html>Erreur</html>");
        assertThatThrownBy(() -> stockage.publierCopie(temporaire)).isInstanceOf(CopieRefuseeException.class);
        Files.write(temporaire, new byte[] { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10 });
        assertThatThrownBy(() -> stockage.publierCopie(temporaire)).isInstanceOf(CopieRefuseeException.class);
        stockage.nettoyerTemporairesCopies();
        assertThat(Files.exists(temporaire)).isFalse();
    }

    @Test
    void signaleUneErreurDeDisqueSansPublier() throws Exception {
        Path fichier = dossier.resolve("pas-un-dossier");
        Files.writeString(fichier, "x");
        StockageImages stockage = new StockageImages(new ImagesProperties(fichier.toString(), DataSize.ofMegabytes(2),
                50_000_000, Duration.ofSeconds(1), 3));
        assertThatThrownBy(stockage::temporaireCopie).isInstanceOf(ImageInvalideException.class);
    }
}