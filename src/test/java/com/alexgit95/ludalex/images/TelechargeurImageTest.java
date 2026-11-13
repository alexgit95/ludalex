package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import com.sun.net.httpserver.HttpServer;

class TelechargeurImageTest {

    private HttpServer serveur;
    private int port;
    private TelechargeurImage telechargeur;
    @TempDir Path dossier;

    @BeforeEach
    void demarrer() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        port = serveur.getAddress().getPort();

        serveur.createContext("/ok", echange -> repondre(echange, 200, new byte[1024]));
        serveur.createContext("/absent", echange -> repondre(echange, 404, new byte[0]));
        serveur.createContext("/gros", echange -> {
            echange.sendResponseHeaders(200, 3_000_000);
            try (OutputStream sortie = echange.getResponseBody()) {
                sortie.write(new byte[8192]);
            } catch (IOException ignore) {
                // le client coupe la connexion
            }
        });
        serveur.createContext("/gros-sans-longueur", echange -> {
            echange.sendResponseHeaders(200, 0);
            try (OutputStream sortie = echange.getResponseBody()) {
                for (int i = 0; i < 400; i++) {
                    sortie.write(new byte[8192]);
                }
            } catch (IOException ignore) {
                // le client coupe la connexion
            }
        });
        serveur.createContext("/relatif", echange -> rediriger(echange, "/ok"));
        serveur.createContext("/vers-interne", echange -> rediriger(echange, "http://interne.test/secret"));
        serveur.createContext("/boucle", echange -> rediriger(echange, "/boucle"));
        serveur.createContext("/lent", echange -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            repondre(echange, 200, new byte[10]);
        });
        serveur.start();

        // « public.test » vise le serveur local ; « interne.test » une adresse privée
        UrlSortanteValidateur validateur = new UrlSortanteValidateur(
                hote -> hote.equals("interne.test")
                        ? new InetAddress[] { InetAddress.getByAddress(new byte[] { 10, 0, 0, 5 }) }
                        : new InetAddress[] { InetAddress.getLoopbackAddress() },
                adresse -> adresse.isSiteLocalAddress());
        telechargeur = new TelechargeurImage(new ImagesProperties("unused", DataSize.ofMegabytes(2), 50_000_000,
                Duration.ofMillis(500), 3), validateur);
    }

    @AfterEach
    void arreter() throws IOException {
        serveur.stop(0);
        telechargeur.fermer();
    }

    @Test
    void telechargeUneImage() {
        assertThat(telechargeur.telecharger(url("/ok"))).hasSize(1024);
    }

        @Test
        void telechargeEnFluxEtRevalideLesRedirections() throws IOException {
        Path cible = dossier.resolve("image.tmp");
        telechargeur.telecharger(url("/relatif"), cible);
        assertThat(Files.size(cible)).isEqualTo(1024);
        assertThatThrownBy(() -> telechargeur.telecharger(url("/vers-interne"), cible))
            .isInstanceOf(CopieRefuseeException.class);
        assertThatThrownBy(() -> telechargeur.telecharger(url("/gros-sans-longueur"), cible))
            .isInstanceOf(CopieRefuseeException.class);
        assertThatThrownBy(() -> telechargeur.telecharger(url("/gros"), cible))
            .isInstanceOf(CopieRefuseeException.class);
        assertThatThrownBy(() -> telechargeur.telecharger(url("/absent"), cible))
            .isInstanceOf(ImageInvalideException.class);
        assertThatThrownBy(() -> telechargeur.telecharger(url("/lent"), cible))
            .isInstanceOf(ImageInvalideException.class);
        }

    @Test
    void suitUneRedirectionRelative() {
        assertThat(telechargeur.telecharger(url("/relatif"))).hasSize(1024);
    }

    @Test
    void refuseUneReponseAnnonceeTropVolumineuse() {
        assertThatThrownBy(() -> telechargeur.telecharger(url("/gros")))
                .isInstanceOf(ImageInvalideException.class).hasMessageContaining("volumineuse");
    }

    @Test
    void interrompUnTelechargementSansLongueurAuDelaDeLaLimite() {
        assertThatThrownBy(() -> telechargeur.telecharger(url("/gros-sans-longueur")))
                .isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void revalideChaqueRedirection() {
        assertThatThrownBy(() -> telechargeur.telecharger(url("/vers-interne")))
                .isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void limiteLeNombreDeRedirections() {
        assertThatThrownBy(() -> telechargeur.telecharger(url("/boucle")))
                .isInstanceOf(ImageInvalideException.class).hasMessageContaining("redirections");
    }

    @Test
    void abandonneQuandLeServeurNeRepondPasDansLeDelai() {
        assertThatThrownBy(() -> telechargeur.telecharger(url("/lent"))).isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void refuseUneReponseEnErreur() {
        assertThatThrownBy(() -> telechargeur.telecharger(url("/absent"))).isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void neContacteJamaisUneAdresseDeBoucleAvecLaPolitiqueParDefaut() throws IOException {
        TelechargeurImage strict = new TelechargeurImage(
                new ImagesProperties("unused", DataSize.ofMegabytes(2), 50_000_000, Duration.ofSeconds(1), 3),
                new UrlSortanteValidateur());
        try {
            assertThatThrownBy(() -> strict.telecharger("http://127.0.0.1:" + port + "/ok"))
                    .isInstanceOf(ImageInvalideException.class);
        } finally {
            strict.fermer();
        }
    }

    private String url(String chemin) {
        return "http://public.test:" + port + chemin;
    }

    private static void repondre(com.sun.net.httpserver.HttpExchange echange, int code, byte[] corps) throws IOException {
        echange.sendResponseHeaders(code, corps.length == 0 ? -1 : corps.length);
        if (corps.length > 0) {
            try (OutputStream sortie = echange.getResponseBody()) {
                sortie.write(corps);
            }
        }
        echange.close();
    }

    private static void rediriger(com.sun.net.httpserver.HttpExchange echange, String cible) throws IOException {
        echange.getResponseHeaders().putAll(Map.of("Location", java.util.List.of(cible)));
        echange.sendResponseHeaders(302, -1);
        echange.close();
    }
}
