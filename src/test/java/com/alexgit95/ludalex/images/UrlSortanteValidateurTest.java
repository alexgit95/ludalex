package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UrlSortanteValidateurTest {

    private final UrlSortanteValidateur validateur = new UrlSortanteValidateur();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/a.png",
            "http://[::1]/a.png",
            "http://10.0.0.1/a.png",
            "http://172.16.5.4/a.png",
            "http://192.168.1.10/a.png",
            "http://169.254.169.254/latest/meta-data",
            "http://100.64.0.1/a.png",
            "http://0.0.0.0/a.png",
            "http://[fd00::1]/a.png",
            "http://[fe80::1]/a.png",
            "http://2130706433/a.png",
            "http://[::ffff:127.0.0.1]/a.png",
            "http://240.0.0.1/a.png"
    })
    void refuseLesAdressesInternesAvantToutAppel(String url) {
        assertThatThrownBy(() -> validateur.valider(url)).isInstanceOf(ImageInvalideException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://exemple.org/a.png",
            "file:///etc/passwd",
            "gopher://exemple.org/",
            "javascript:alert(1)",
            "//exemple.org/a.png",
            "http://utilisateur:secret@exemple.org/a.png",
            "http://",
            "pas une url",
            ""
    })
    void refuseLesAdressesMalFormeesOuAuSchemaInterdit(String url) {
        assertThatThrownBy(() -> validateur.validerSyntaxe(url)).isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void refuseUneAdresseNulleOuTropLongue() {
        assertThatThrownBy(() -> validateur.validerSyntaxe(null)).isInstanceOf(ImageInvalideException.class);
        assertThatThrownBy(() -> validateur.validerSyntaxe("https://exemple.org/" + "a".repeat(2100)))
                .isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void accepteLaSaisieDUneAdresseDuReseauLocalSansRequeteSortante() {
        assertThat(validateur.validerSyntaxe("http://192.168.1.10/a.png").getHost()).isEqualTo("192.168.1.10");
    }

    @Test
    void refuseUnNomDeDomaineQuiResoutVersUneAdressePrivee() {
        UrlSortanteValidateur avecDns = new UrlSortanteValidateur(
                hote -> new InetAddress[] { InetAddress.getByAddress(new byte[] { 10, 0, 0, 5 }) },
                UrlSortanteValidateur::estInterdite);

        assertThatThrownBy(() -> avecDns.valider("https://pirate.example.org/a.png"))
                .isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void refuseSiUneSeuleDesAdressesResolueEstInterne() {
        UrlSortanteValidateur avecDns = new UrlSortanteValidateur(
                hote -> new InetAddress[] { InetAddress.getByAddress(new byte[] { (byte) 93, (byte) 184, (byte) 216, 34 }),
                        InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }) },
                UrlSortanteValidateur::estInterdite);

        assertThatThrownBy(() -> avecDns.valider("https://double.example.org/a.png"))
                .isInstanceOf(ImageInvalideException.class);
    }

    @Test
    void accepteUnNomDeDomainePublic() {
        UrlSortanteValidateur avecDns = new UrlSortanteValidateur(
                hote -> new InetAddress[] { InetAddress.getByAddress(new byte[] { (byte) 93, (byte) 184, (byte) 216, 34 }) },
                UrlSortanteValidateur::estInterdite);

        assertThatCode(() -> avecDns.valider("https://exemple.org/a.png")).doesNotThrowAnyException();
    }

    @Test
    void refuseUnHoteInconnu() {
        UrlSortanteValidateur avecDns = new UrlSortanteValidateur(
                hote -> {
                    throw new UnknownHostException(hote);
                },
                UrlSortanteValidateur::estInterdite);

        assertThatThrownBy(() -> avecDns.valider("https://inconnu.example.org/a.png"))
                .isInstanceOf(ImageInvalideException.class);
    }
}
