package com.alexgit95.ludalex.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

class AdminControllerTest {

    @Test
    void ajouteLesSeptPremiersCaracteresDuSha() {
        BuildProperties build = mock(BuildProperties.class);
        when(build.getVersion()).thenReturn("0.13.2");
        when(build.get("git.commit")).thenReturn("abcdef0123456789");

        assertThat(AdminController.versionApplication(build)).isEqualTo("0.13.2 - abcdef0");
    }

    @Test
    void afficheLaVersionSeuleQuandLeShaEstIndisponible() {
        BuildProperties build = mock(BuildProperties.class);
        when(build.getVersion()).thenReturn("0.13.2");

        assertThat(AdminController.versionApplication(build)).isEqualTo("0.13.2");
        when(build.get("git.commit")).thenReturn("unknown");
        assertThat(AdminController.versionApplication(build)).isEqualTo("0.13.2");
        when(build.get("git.commit")).thenReturn("   ");
        assertThat(AdminController.versionApplication(build)).isEqualTo("0.13.2");
    }

    @Test
    void afficheUnShaPlusCourtSansErreur() {
        BuildProperties build = mock(BuildProperties.class);
        when(build.getVersion()).thenReturn("0.13.2");
        when(build.get("git.commit")).thenReturn("abc12");

        assertThat(AdminController.versionApplication(build)).isEqualTo("0.13.2 - abc12");
    }

    @Test
    void gereLAbsenceDesMetadonneesDeBuild() {
        assertThat(AdminController.versionApplication(null)).isEqualTo("inconnue");
    }
}