package com.alexgit95.ludalex.jeux;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.alexgit95.ludalex.images.ImageSource;

@DataJpaTest(properties = "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect")
class JeuRepositoryTest {

    @Autowired
    private JeuRepository jeux;

    @Autowired
    private PretRepository prets;

    @Test
    void enregistreUnJeuAvecImageCouleurPrixEtDate() {
        Jeu jeu = new Jeu("Catan");
        jeu.setImage(ImageSource.url("https://exemple.org/c.png"));
        jeu.setCouleurFond("#336699");
        jeu.setPrix(new java.math.BigDecimal("45.50"));
        jeu.setDateAchat(LocalDate.of(2025, 3, 12));
        jeu.setCooperatif(true);
        jeux.saveAndFlush(jeu);

        Jeu relu = jeux.findById(jeu.getId()).orElseThrow();

        assertThat(relu.getImage().getReference()).isEqualTo("https://exemple.org/c.png");
        assertThat(relu.getCouleurFond()).isEqualTo("#336699");
        assertThat(relu.getPrix()).isEqualByComparingTo("45.50");
        assertThat(relu.getDateAchat()).isEqualTo(LocalDate.of(2025, 3, 12));
        assertThat(relu.isCooperatif()).isTrue();
    }

    @Test
    void neConserveQueLesCouleursValides() {
        Jeu jeu = new Jeu("Jeu");

        jeu.setCouleurFond("javascript:alert(1)");

        assertThat(jeu.getCouleurFond()).isEqualTo("#3a3a3a");
    }

    @Test
    void trouveLePretEnCoursEtLHistoriqueDuPlusRecentAuPlusAncien() {
        Jeu jeu = jeux.save(new Jeu("Catan"));
        Pret ancien = new Pret(jeu, "Paul", LocalDate.of(2026, 1, 1));
        ancien.clore(LocalDate.of(2026, 1, 15));
        prets.save(ancien);
        prets.save(new Pret(jeu, "Marie", LocalDate.of(2026, 5, 2)));

        assertThat(prets.trouverEnCours(jeu.getId())).get().extracting(Pret::getEmprunteur).isEqualTo("Marie");
        assertThat(prets.historique(jeu.getId())).extracting(Pret::getEmprunteur).containsExactly("Marie", "Paul");
    }

    @Test
    void neTrouveAucunPretEnCoursSiTousSontRendus() {
        Jeu jeu = jeux.save(new Jeu("Catan"));
        Pret pret = new Pret(jeu, "Paul", LocalDate.of(2026, 1, 1));
        pret.clore(LocalDate.of(2026, 1, 2));
        prets.save(pret);

        assertThat(prets.trouverEnCours(jeu.getId())).isEmpty();
    }

    @Test
    void listeLesEmprunteursCourantsDePlusieursJeuxEnUneRequete() {
        Jeu catan = jeux.save(new Jeu("Catan"));
        Jeu azul = jeux.save(new Jeu("Azul"));
        Jeu libre = jeux.save(new Jeu("Libre"));
        prets.save(new Pret(catan, "Marie", LocalDate.of(2026, 5, 2)));
        Pret rendu = new Pret(azul, "Paul", LocalDate.of(2026, 1, 1));
        rendu.clore(LocalDate.of(2026, 1, 2));
        prets.save(rendu);

        List<EmprunteurJeu> emprunteurs = prets.emprunteursEnCours(List.of(catan.getId(), azul.getId(), libre.getId()));

        assertThat(emprunteurs).containsExactly(new EmprunteurJeu(catan.getId(), "Marie"));
    }

    @Test
    void supprimeTousLesPretsDUnJeuSansToucherLesAutres() {
        Jeu catan = jeux.save(new Jeu("Catan"));
        Jeu azul = jeux.save(new Jeu("Azul"));
        prets.save(new Pret(catan, "Marie", LocalDate.of(2026, 5, 2)));
        prets.save(new Pret(azul, "Paul", LocalDate.of(2026, 5, 3)));

        prets.supprimerParJeu(catan.getId());

        assertThat(prets.historique(catan.getId())).isEmpty();
        assertThat(prets.historique(azul.getId())).hasSize(1);
    }

    @Test
    void listeLesJeuxParNomSansTenirCompteDeLaCasse() {
        for (String nom : new String[] { "zombicide", "Azul", "catan" }) {
            jeux.save(new Jeu(nom));
        }

        var page = jeux.findAll(PageRequest.of(0, 10, Sort.by(Sort.Order.asc("nom").ignoreCase())));

        assertThat(page.getContent()).extracting(Jeu::getNom).containsExactly("Azul", "catan", "zombicide");
    }

    @Test
    void verrouilleUnJeuPourMiseAJour() {
        Jeu jeu = jeux.save(new Jeu("Catan"));

        assertThat(jeux.trouverPourMiseAJour(jeu.getId())).isPresent();
        assertThat(jeux.trouverPourMiseAJour(jeu.getId() + 100)).isEmpty();
    }
}
