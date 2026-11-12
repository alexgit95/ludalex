package com.alexgit95.ludalex.jeux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PretServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC);

    private JeuRepository jeux;
    private PretRepository prets;
    private PretService service;
    private Jeu catan;

    @BeforeEach
    void preparer() {
        jeux = mock(JeuRepository.class);
        prets = mock(PretRepository.class);
        catan = new Jeu("Catan");
        when(jeux.trouverPourMiseAJour(1L)).thenReturn(Optional.of(catan));
        when(prets.save(any(Pret.class))).thenAnswer(appel -> appel.getArgument(0));
        service = new PretService(jeux, prets, HORLOGE);
    }

    @Test
    void enregistreUnPretEnCours() {
        Pret pret = service.preter(1L, "  Marie ", LocalDate.of(2026, 5, 2));

        assertThat(pret.getEmprunteur()).isEqualTo("Marie");
        assertThat(pret.getDatePret()).isEqualTo(LocalDate.of(2026, 5, 2));
        assertThat(pret.estEnCours()).isTrue();
    }

    @Test
    void utiliseLaDateDuJourParDefaut() {
        assertThat(service.preter(1L, "Marie", null).getDatePret()).isEqualTo(LocalDate.of(2026, 11, 12));
    }

    @Test
    void refuseUnEmprunteurVideOuTropLong() {
        assertThatThrownBy(() -> service.preter(1L, " ", null)).isInstanceOf(PretInvalideException.class)
                .hasMessageContaining("obligatoire");
        assertThatThrownBy(() -> service.preter(1L, null, null)).isInstanceOf(PretInvalideException.class);
        assertThatThrownBy(() -> service.preter(1L, "a".repeat(101), null)).isInstanceOf(PretInvalideException.class);
        verify(prets, never()).save(any());
    }

    @Test
    void refuseDePreterUnJeuDejaPrete() {
        when(prets.trouverEnCours(1L)).thenReturn(Optional.of(new Pret(catan, "Paul", LocalDate.of(2026, 5, 1))));

        assertThatThrownBy(() -> service.preter(1L, "Marie", null)).isInstanceOf(PretInvalideException.class)
                .hasMessageContaining("déjà prêté");
        verify(prets, never()).save(any());
    }

    @Test
    void refuseUnJeuInconnu() {
        when(jeux.trouverPourMiseAJour(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preter(9L, "Marie", null)).isInstanceOf(JeuIntrouvableException.class);
        assertThatThrownBy(() -> service.retourner(9L, null)).isInstanceOf(JeuIntrouvableException.class);
    }

    @Test
    void cloturLePretAvecSaDateDeRetour() {
        Pret enCours = new Pret(catan, "Marie", LocalDate.of(2026, 5, 2));
        when(prets.trouverEnCours(1L)).thenReturn(Optional.of(enCours));

        Pret clos = service.retourner(1L, LocalDate.of(2026, 5, 20));

        assertThat(clos.getDateRetour()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(clos.estEnCours()).isFalse();
    }

    @Test
    void utiliseLaDateDuJourPourLeRetourParDefaut() {
        when(prets.trouverEnCours(1L)).thenReturn(Optional.of(new Pret(catan, "Marie", LocalDate.of(2026, 5, 2))));

        assertThat(service.retourner(1L, null).getDateRetour()).isEqualTo(LocalDate.of(2026, 11, 12));
    }

    @Test
    void accepteUnRetourLeJourDuPret() {
        when(prets.trouverEnCours(1L)).thenReturn(Optional.of(new Pret(catan, "Marie", LocalDate.of(2026, 5, 2))));

        assertThat(service.retourner(1L, LocalDate.of(2026, 5, 2)).estEnCours()).isFalse();
    }

    @Test
    void refuseUnRetourAnterieurAuPret() {
        Pret enCours = new Pret(catan, "Marie", LocalDate.of(2026, 5, 2));
        when(prets.trouverEnCours(1L)).thenReturn(Optional.of(enCours));

        assertThatThrownBy(() -> service.retourner(1L, LocalDate.of(2026, 5, 1)))
                .isInstanceOf(PretInvalideException.class).hasMessageContaining("précéder");
        assertThat(enCours.estEnCours()).isTrue();
    }

    @Test
    void refuseLeRetourDUnJeuNonPrete() {
        when(prets.trouverEnCours(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retourner(1L, null)).isInstanceOf(PretInvalideException.class)
                .hasMessageContaining("pas prêté");
    }
}
