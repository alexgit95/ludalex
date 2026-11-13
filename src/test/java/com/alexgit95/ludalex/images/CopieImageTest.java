package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CopieImageTest {
    private final Instant maintenant = Instant.parse("2026-11-13T10:00:00Z");

    @Test
    void identifieExactementUneUrlSansNormaliserLesParametres() {
        assertThat(CopieImage.cle("https://example.org/a?x=1&y=2")).hasSize(64)
                .isEqualTo(new CopieImage("https://example.org/a?x=1&y=2").getCle())
                .isNotEqualTo(CopieImage.cle("https://example.org/a?y=2&x=1"));
    }

    @Test
    void borneLesTentativesPuisPermetUneRelance() {
        CopieImage copie = new CopieImage("https://example.org/a.png");
        copie.commencer(maintenant);
        copie.echouer(maintenant, false, "x".repeat(300));
        assertThat(copie.getErreur()).hasSize(200);
        assertThat(copie.getProchaineTentative()).isEqualTo(maintenant.plusSeconds(60));
        copie.commencer(maintenant.plusSeconds(60));
        copie.echouer(maintenant.plusSeconds(60), false, "Reseau indisponible");
        assertThat(copie.getProchaineTentative()).isEqualTo(maintenant.plusSeconds(960));
        copie.commencer(maintenant.plusSeconds(960));
        copie.echouer(maintenant.plusSeconds(960), false, null);
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.ECHEC);
        assertThat(copie.getProchaineTentative()).isNull();
        copie.relancer();
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.EN_ATTENTE);
        assertThat(copie.getTentatives()).isZero();
        assertThat(copie.getErreur()).isNull();
    }

    @Test
    void reprendUnTravailEtPublieOuEchoueDefinitivement() {
        CopieImage copie = new CopieImage("https://example.org/a.png");
        copie.commencer(maintenant);
        copie.reprendre();
        assertThat(copie.getTentatives()).isEqualTo(1);
        copie.commencer(maintenant);
        copie.disponible("image.png", 123);
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.DISPONIBLE);
        assertThat(copie.getFichier()).isEqualTo("image.png");
        assertThat(copie.getTaille()).isEqualTo(123);
        copie.relancer();
        copie.commencer(maintenant);
        copie.echouer(maintenant, true, "Format refuse");
        assertThat(copie.getEtat()).isEqualTo(CopieImage.Etat.ECHEC);
    }
}