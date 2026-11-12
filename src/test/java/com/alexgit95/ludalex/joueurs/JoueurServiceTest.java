package com.alexgit95.ludalex.joueurs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.alexgit95.ludalex.images.ImageInvalideException;
import com.alexgit95.ludalex.images.ImageSource;
import com.alexgit95.ludalex.images.ServiceImages;

class JoueurServiceTest {

    private JoueurRepository joueurs;
    private ServiceImages images;
    private ParticipationsJoueur participations;
    private JoueurService service;

    @BeforeEach
    void preparer() {
        joueurs = mock(JoueurRepository.class);
        images = mock(ServiceImages.class);
        participations = mock(ParticipationsJoueur.class);
        when(joueurs.save(any(Joueur.class))).thenAnswer(appel -> appel.getArgument(0));
        service = new JoueurService(joueurs, images, participations);
    }

    private static EntreeImage fichier() {
        return new EntreeImage(new MockMultipartFile("f", "a.png", "image/png", new byte[] { 1 }), null, false);
    }

    @Test
    void creeUnJoueurAvecSesImages() {
        ImageSource avatar = ImageSource.upload("11111111-1111-1111-1111-111111111111.png");
        ImageSource victoire = ImageSource.url("https://exemple.org/victoire.png");
        when(images.enregistrerUpload(any())).thenReturn(avatar);
        when(images.enregistrerUrl("https://exemple.org/victoire.png")).thenReturn(victoire);

        Joueur joueur = service.creer("  Alex ", fichier(),
                new EntreeImage(null, "https://exemple.org/victoire.png", false));

        assertThat(joueur.getNom()).isEqualTo("Alex");
        assertThat(joueur.getAvatar()).isEqualTo(avatar);
        assertThat(joueur.getImageVictoire()).isEqualTo(victoire);
        assertThat(joueur.isAnonymise()).isFalse();
    }

    @Test
    void creeUnJoueurSansImage() {
        Joueur joueur = service.creer("Alex", EntreeImage.inchangee(), EntreeImage.inchangee());

        assertThat(joueur.getAvatar()).isNull();
        assertThat(joueur.getImageVictoire()).isNull();
    }

    @Test
    void refuseUnNomManquantOuTropLong() {
        assertThatThrownBy(() -> service.creer(" ", null, null)).isInstanceOf(JoueurInvalideException.class)
                .hasMessageContaining("obligatoire");
        assertThatThrownBy(() -> service.creer(null, null, null)).isInstanceOf(JoueurInvalideException.class);
        assertThatThrownBy(() -> service.creer("a".repeat(101), null, null))
                .isInstanceOf(JoueurInvalideException.class).hasMessageContaining("100");
        verify(joueurs, never()).save(any());
    }

    @Test
    void refuseUnNomDejaPrisSansTenirCompteDeLaCasse() {
        when(joueurs.existsByNomNormaliseAndAnonymiseFalse("alex")).thenReturn(true);

        assertThatThrownBy(() -> service.creer("ALEX", null, null))
                .isInstanceOfSatisfying(JoueurInvalideException.class, e -> {
                    assertThat(e.getChamp()).isEqualTo("nom");
                    assertThat(e.getMessage()).contains("déjà");
                });
    }

    @Test
    void signaleUneImageInvalideSurLeBonChamp() {
        when(images.enregistrerUpload(any())).thenThrow(new ImageInvalideException("Format non autorisé"));

        assertThatThrownBy(() -> service.creer("Alex", fichier(), null))
                .isInstanceOfSatisfying(JoueurInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("avatar"));
        assertThatThrownBy(() -> service.creer("Alex", null, fichier()))
                .isInstanceOfSatisfying(JoueurInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("victoire"));
    }

    @Test
    void modifieLeNomEtRemplaceUnAvatar() {
        Joueur existant = new Joueur("Alex");
        ImageSource ancien = ImageSource.upload("22222222-2222-2222-2222-222222222222.png");
        existant.setAvatar(ancien);
        when(joueurs.findById(5L)).thenReturn(Optional.of(existant));
        ImageSource nouveau = ImageSource.upload("33333333-3333-3333-3333-333333333333.png");
        when(images.enregistrerUpload(any())).thenReturn(nouveau);

        Joueur modifie = service.modifier(5L, "Alexandre", fichier(), EntreeImage.inchangee());

        assertThat(modifie.getNom()).isEqualTo("Alexandre");
        assertThat(modifie.getAvatar()).isEqualTo(nouveau);
        verify(images).remplacer(ancien, nouveau);
    }

    @Test
    void conserveLesImagesQuandRienNEstSaisi() {
        Joueur existant = new Joueur("Alex");
        ImageSource avatar = ImageSource.url("https://exemple.org/a.png");
        existant.setAvatar(avatar);
        when(joueurs.findById(5L)).thenReturn(Optional.of(existant));

        Joueur modifie = service.modifier(5L, "Alex", EntreeImage.inchangee(), null);

        assertThat(modifie.getAvatar()).isEqualTo(avatar);
        verify(images, never()).remplacer(any(), any());
    }

    @Test
    void retireUneImage() {
        Joueur existant = new Joueur("Alex");
        ImageSource avatar = ImageSource.upload("22222222-2222-2222-2222-222222222222.png");
        existant.setAvatar(avatar);
        when(joueurs.findById(5L)).thenReturn(Optional.of(existant));

        Joueur modifie = service.modifier(5L, "Alex", new EntreeImage(null, null, true), null);

        assertThat(modifie.getAvatar()).isNull();
        verify(images).remplacer(avatar, null);
    }

    @Test
    void autoriseLeNomDuJoueurLuiMemeLorsDeLaModification() {
        Joueur existant = new Joueur("Alex");
        when(joueurs.findById(5L)).thenReturn(Optional.of(existant));
        when(joueurs.existsByNomNormaliseAndAnonymiseFalseAndIdNot("alex", 5L)).thenReturn(false);

        assertThat(service.modifier(5L, "ALEX", null, null).getNom()).isEqualTo("ALEX");
    }

    @Test
    void refuseUnNomPrisParUnAutreJoueurLorsDeLaModification() {
        when(joueurs.findById(5L)).thenReturn(Optional.of(new Joueur("Alex")));
        when(joueurs.existsByNomNormaliseAndAnonymiseFalseAndIdNot("marie", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.modifier(5L, "Marie", null, null)).isInstanceOf(JoueurInvalideException.class);
    }

    @Test
    void neModifiePasUnJoueurAnonymiseOuInconnu() {
        Joueur anonyme = new Joueur("Alex");
        anonyme.anonymiser();
        when(joueurs.findById(5L)).thenReturn(Optional.of(anonyme));
        when(joueurs.findById(6L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifier(5L, "Autre", null, null)).isInstanceOf(JoueurIntrouvableException.class);
        assertThatThrownBy(() -> service.modifier(6L, "Autre", null, null)).isInstanceOf(JoueurIntrouvableException.class);
    }

    @Test
    void anonymiseUnJoueurAyantDesParties() {
        Joueur existant = new Joueur("Alex");
        ImageSource avatar = ImageSource.upload("22222222-2222-2222-2222-222222222222.png");
        ImageSource victoire = ImageSource.url("https://exemple.org/v.png");
        existant.setAvatar(avatar);
        existant.setImageVictoire(victoire);
        when(joueurs.findById(5L)).thenReturn(Optional.of(existant));
        when(participations.aDesParticipations(5L)).thenReturn(true);

        service.supprimer(5L);

        assertThat(existant.isAnonymise()).isTrue();
        assertThat(existant.getNom()).isEqualTo(Joueur.NOM_ANONYME);
        assertThat(existant.getAvatar()).isNull();
        assertThat(existant.getImageVictoire()).isNull();
        verify(joueurs).save(existant);
        verify(joueurs, never()).delete(any());
        verify(images).remplacer(avatar, null);
        verify(images).remplacer(victoire, null);
    }

    @Test
    void supprimeDefinitivementUnJoueurSansPartie() {
        Joueur existant = new Joueur("Alex");
        when(joueurs.findById(5L)).thenReturn(Optional.of(existant));
        when(participations.aDesParticipations(5L)).thenReturn(false);

        service.supprimer(5L);

        verify(joueurs).delete(existant);
        verify(joueurs, never()).save(any());
    }

    @Test
    void neSupprimePasUnJoueurDejaAnonymise() {
        Joueur anonyme = new Joueur("Alex");
        anonyme.anonymiser();
        when(joueurs.findById(5L)).thenReturn(Optional.of(anonyme));

        assertThatThrownBy(() -> service.supprimer(5L)).isInstanceOf(JoueurIntrouvableException.class);
        verify(participations, never()).aDesParticipations(anyLong());
    }

    @Test
    void neProposePourUnePartieQueLesJoueursActifs() {
        Joueur alex = new Joueur("Alex");
        when(joueurs.findByAnonymiseFalseOrderByNomNormaliseAsc()).thenReturn(List.of(alex));

        assertThat(service.selectionnables()).containsExactly(alex);
    }

    @Test
    void neFaitRienQuandLAdresseWebEstBlanche() {
        service.creer("Alex", new EntreeImage(null, "  ", false), null);

        verify(images, never()).enregistrerUrl(anyString());
        verify(images, never()).enregistrerUpload(any());
        verify(images, never()).remplacer(any(), eq(null));
    }
}
