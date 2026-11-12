package com.alexgit95.ludalex.jeux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.images.ImageInvalideException;
import com.alexgit95.ludalex.images.ImagePreparee;
import com.alexgit95.ludalex.images.ImageSource;
import com.alexgit95.ludalex.images.ServiceImages;

class JeuServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC);

    private JeuRepository jeux;
    private PretRepository prets;
    private ServiceImages images;
    private PartiesJeu parties;
    private JeuService service;

    @BeforeEach
    void preparer() {
        jeux = mock(JeuRepository.class);
        prets = mock(PretRepository.class);
        images = mock(ServiceImages.class);
        parties = mock(PartiesJeu.class);
        when(jeux.save(any(Jeu.class))).thenAnswer(appel -> appel.getArgument(0));
        when(images.resoudreCouleur(any(), any(), any())).thenCallRealMethod();
        service = new JeuService(jeux, prets, images, parties, HORLOGE);
    }

    private static Jeu jeu(long id, String nom) {
        Jeu jeu = new Jeu(nom);
        ReflectionTestUtils.setField(jeu, "id", id);
        return jeu;
    }

    private static JeuSaisie saisie(String nom) {
        return new JeuSaisie(nom, null, null, false, null, EntreeImage.inchangee());
    }

    @Test
    void creeUnJeuAvecImageUploadeeEtCouleurCalculee() {
        ImageSource source = ImageSource.upload("11111111-1111-1111-1111-111111111111.png");
        when(images.preparerUpload(any())).thenReturn(new ImagePreparee(source, "#c82828"));
        EntreeImage image = new EntreeImage(new MockMultipartFile("f", "a.png", "image/png", new byte[] { 1 }), null, false);

        Jeu jeu = service.creer(new JeuSaisie("  Catan ", new BigDecimal("45"), LocalDate.of(2025, 3, 12), false, null, image));

        assertThat(jeu.getNom()).isEqualTo("Catan");
        assertThat(jeu.getPrix()).isEqualByComparingTo("45.00");
        assertThat(jeu.getDateAchat()).isEqualTo(LocalDate.of(2025, 3, 12));
        assertThat(jeu.isCooperatif()).isFalse();
        assertThat(jeu.getImage()).isEqualTo(source);
        assertThat(jeu.getCouleurFond()).isEqualTo("#c82828");
    }

    @Test
    void creeUnJeuCooperatifAvecUneImageParUrl() {
        ImageSource source = ImageSource.url("https://exemple.org/pandemic.png");
        when(images.preparerUrl("https://exemple.org/pandemic.png")).thenReturn(new ImagePreparee(source, "#112233"));

        Jeu jeu = service.creer(new JeuSaisie("Pandemic", null, null, true,
                null, new EntreeImage(null, "https://exemple.org/pandemic.png", false)));

        assertThat(jeu.isCooperatif()).isTrue();
        assertThat(jeu.getImage()).isEqualTo(source);
        assertThat(jeu.getCouleurFond()).isEqualTo("#112233");
    }

    @Test
    void utiliseLaCouleurChoisieAvantLaCouleurCalculee() {
        when(images.preparerUrl(any())).thenReturn(new ImagePreparee(ImageSource.url("https://a.b/c.png"), "#112233"));

        Jeu jeu = service.creer(new JeuSaisie("Jeu", null, null, false, "#336699",
                new EntreeImage(null, "https://a.b/c.png", false)));

        assertThat(jeu.getCouleurFond()).isEqualTo("#336699");
    }

    @Test
    void creeUnJeuSansImageAvecLaCouleurNeutre() {
        Jeu jeu = service.creer(saisie("Jeu"));

        assertThat(jeu.getImage()).isNull();
        assertThat(jeu.getCouleurFond()).isEqualTo("#3a3a3a");
    }

    @Test
    void refuseUnNomManquantOuTropLong() {
        assertThatThrownBy(() -> service.creer(saisie(" "))).isInstanceOf(JeuInvalideException.class)
                .hasMessageContaining("obligatoire");
        assertThatThrownBy(() -> service.creer(saisie(null))).isInstanceOf(JeuInvalideException.class);
        assertThatThrownBy(() -> service.creer(saisie("a".repeat(201)))).isInstanceOf(JeuInvalideException.class);
        verify(jeux, never()).save(any());
    }

    @Test
    void refuseUnPrixInvalide() {
        assertThatThrownBy(() -> service.creer(new JeuSaisie("Jeu", new BigDecimal("-1"), null, false, null, null)))
                .isInstanceOfSatisfying(JeuInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("prix"));
        assertThatThrownBy(() -> service.creer(new JeuSaisie("Jeu", new BigDecimal("1.234"), null, false, null, null)))
                .isInstanceOf(JeuInvalideException.class);
        assertThatThrownBy(() -> service.creer(new JeuSaisie("Jeu", new BigDecimal("100000000"), null, false, null, null)))
                .isInstanceOf(JeuInvalideException.class);
    }

    @Test
    void accepteUnPrixNulOuAvecDecimalesNulles() {
        assertThat(service.creer(new JeuSaisie("Jeu", BigDecimal.ZERO, null, false, null, null)).getPrix())
                .isEqualByComparingTo("0");
        assertThat(service.creer(new JeuSaisie("Jeu", new BigDecimal("12.500"), null, false, null, null)).getPrix())
                .isEqualByComparingTo("12.50");
    }

    @Test
    void refuseUneDateDAchatFutureMaisAccepteAujourdhui() {
        assertThatThrownBy(() -> service.creer(new JeuSaisie("Jeu", null, LocalDate.of(2026, 11, 13), false, null, null)))
                .isInstanceOfSatisfying(JeuInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("dateAchat"));

        assertThat(service.creer(new JeuSaisie("Jeu", null, LocalDate.of(2026, 11, 12), false, null, null)).getDateAchat())
                .isEqualTo(LocalDate.of(2026, 11, 12));
    }

    @Test
    void signaleUneImageInvalideSurLeChampImage() {
        when(images.preparerUpload(any())).thenThrow(new ImageInvalideException("Format non autorisé"));
        EntreeImage image = new EntreeImage(new MockMultipartFile("f", "a.png", "image/png", new byte[] { 1 }), null, false);

        assertThatThrownBy(() -> service.creer(new JeuSaisie("Jeu", null, null, false, null, image)))
                .isInstanceOfSatisfying(JeuInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("image"));
    }

    @Test
    void refuseUneCouleurManuelleInvalide() {
        assertThatThrownBy(() -> service.creer(new JeuSaisie("Jeu", null, null, false, "rouge", null)))
                .isInstanceOfSatisfying(JeuInvalideException.class, e -> assertThat(e.getChamp()).isEqualTo("couleur"));
    }

    @Test
    void conserveImageEtCouleurQuandOnModifieSeulementLeNom() {
        Jeu existant = jeu(1, "Catan");
        ImageSource image = ImageSource.upload("22222222-2222-2222-2222-222222222222.png");
        existant.setImage(image);
        existant.setCouleurFond("#aabbcc");
        when(jeux.findById(1L)).thenReturn(Optional.of(existant));

        Jeu modifie = service.modifier(1L, saisie("Catan Junior"));

        assertThat(modifie.getNom()).isEqualTo("Catan Junior");
        assertThat(modifie.getImage()).isEqualTo(image);
        assertThat(modifie.getCouleurFond()).isEqualTo("#aabbcc");
    }

    @Test
    void recalculeLaCouleurEtRemplaceLImageQuandElleChange() {
        Jeu existant = jeu(1, "Catan");
        ImageSource ancienne = ImageSource.upload("22222222-2222-2222-2222-222222222222.png");
        existant.setImage(ancienne);
        existant.setCouleurFond("#aabbcc");
        when(jeux.findById(1L)).thenReturn(Optional.of(existant));
        ImageSource nouvelle = ImageSource.url("https://exemple.org/n.png");
        when(images.preparerUrl("https://exemple.org/n.png")).thenReturn(new ImagePreparee(nouvelle, "#445566"));

        Jeu modifie = service.modifier(1L, new JeuSaisie("Catan", null, null, false, null,
                new EntreeImage(null, "https://exemple.org/n.png", false)));

        assertThat(modifie.getImage()).isEqualTo(nouvelle);
        assertThat(modifie.getCouleurFond()).isEqualTo("#445566");
        verify(images).remplacer(ancienne, nouvelle);
    }

    @Test
    void retireUneImageEnConservantLaCouleur() {
        Jeu existant = jeu(1, "Catan");
        ImageSource ancienne = ImageSource.upload("22222222-2222-2222-2222-222222222222.png");
        existant.setImage(ancienne);
        existant.setCouleurFond("#aabbcc");
        when(jeux.findById(1L)).thenReturn(Optional.of(existant));

        Jeu modifie = service.modifier(1L, new JeuSaisie("Catan", null, null, false, null,
                new EntreeImage(null, null, true)));

        assertThat(modifie.getImage()).isNull();
        assertThat(modifie.getCouleurFond()).isEqualTo("#aabbcc");
        verify(images).remplacer(ancienne, null);
    }

    @Test
    void remplaceLaCouleurParLaValeurManuelle() {
        Jeu existant = jeu(1, "Catan");
        existant.setCouleurFond("#aabbcc");
        when(jeux.findById(1L)).thenReturn(Optional.of(existant));

        assertThat(service.modifier(1L, new JeuSaisie("Catan", null, null, false, "#336699", null)).getCouleurFond())
                .isEqualTo("#336699");
    }

    @Test
    void repond404PourUnJeuInconnu() {
        when(jeux.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifier(9L, saisie("X"))).isInstanceOf(JeuIntrouvableException.class);
        assertThatThrownBy(() -> service.supprimer(9L)).isInstanceOf(JeuIntrouvableException.class);
        assertThatThrownBy(() -> service.fiche(9L)).isInstanceOf(JeuIntrouvableException.class);
    }

    @Test
    void refuseLaSuppressionDUnJeuAvecDesParties() {
        when(jeux.findById(1L)).thenReturn(Optional.of(jeu(1, "Catan")));
        when(parties.aDesParties(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.supprimer(1L)).isInstanceOf(JeuInvalideException.class)
                .hasMessageContaining("parties");
        verify(jeux, never()).delete(any());
        verify(prets, never()).supprimerParJeu(any());
    }

    @Test
    void supprimeLeJeuSesPretsEtSonImage() {
        Jeu existant = jeu(1, "Catan");
        ImageSource image = ImageSource.upload("22222222-2222-2222-2222-222222222222.png");
        existant.setImage(image);
        when(jeux.findById(1L)).thenReturn(Optional.of(existant));
        when(parties.aDesParties(1L)).thenReturn(false);

        service.supprimer(1L);

        verify(prets).supprimerParJeu(1L);
        verify(jeux).delete(existant);
        verify(images).remplacer(image, null);
    }

    @Test
    void listeLesJeuxAvecLEmprunteurCourantSansRequeteParJeu() {
        Jeu catan = jeu(1, "Catan");
        Jeu azul = jeu(2, "Azul");
        when(jeux.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(azul, catan)));
        when(prets.emprunteursEnCours(anyCollection())).thenReturn(List.of(new EmprunteurJeu(1L, "Marie")));

        var page = service.lister(0);

        assertThat(page.getContent()).extracting(VignetteJeu::emprunteur).containsExactly(null, "Marie");
        verify(prets).emprunteursEnCours(List.of(2L, 1L));
    }

    @Test
    void listeVideSansInterrogerLesPrets() {
        when(jeux.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThat(service.lister(-3).getContent()).isEmpty();
        verify(prets, never()).emprunteursEnCours(any());
    }

    @Test
    void composeLaFicheAvecLePretEnCoursEtLHistorique() {
        Jeu existant = jeu(1, "Catan");
        Pret enCours = new Pret(existant, "Marie", LocalDate.of(2026, 5, 2));
        when(jeux.findById(1L)).thenReturn(Optional.of(existant));
        when(prets.trouverEnCours(1L)).thenReturn(Optional.of(enCours));
        when(prets.historique(1L)).thenReturn(List.of(enCours));

        FicheJeu fiche = service.fiche(1L);

        assertThat(fiche.enCours()).isSameAs(enCours);
        assertThat(fiche.historique()).containsExactly(enCours);
    }
}
