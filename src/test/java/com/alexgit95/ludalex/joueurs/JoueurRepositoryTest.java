package com.alexgit95.ludalex.joueurs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.alexgit95.ludalex.images.ImageSource;

@DataJpaTest(properties = "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect")
class JoueurRepositoryTest {

    @Autowired
    private JoueurRepository joueurs;

    @Test
    void enregistreLesDeuxImagesDansDesColonnesDistinctes() {
        Joueur joueur = new Joueur("Alex");
        joueur.setAvatar(ImageSource.upload("11111111-1111-1111-1111-111111111111.png"));
        joueur.setImageVictoire(ImageSource.url("https://exemple.org/v.png"));
        joueurs.saveAndFlush(joueur);

        Joueur relu = joueurs.findById(joueur.getId()).orElseThrow();

        assertThat(relu.getAvatar().getReference()).isEqualTo("11111111-1111-1111-1111-111111111111.png");
        assertThat(relu.getImageVictoire().getReference()).isEqualTo("https://exemple.org/v.png");
    }

    @Test
    void detecteUnNomDejaPrisParmiLesJoueursActifsSeulement() {
        joueurs.save(new Joueur("Marie"));
        Joueur ancien = new Joueur("Paul");
        ancien.anonymiser();
        joueurs.save(ancien);

        assertThat(joueurs.existsByNomNormaliseAndAnonymiseFalse("marie")).isTrue();
        assertThat(joueurs.existsByNomNormaliseAndAnonymiseFalse("paul")).isFalse();
        assertThat(joueurs.existsByNomNormaliseAndAnonymiseFalse("joueur supprimé")).isFalse();
    }

    @Test
    void exclutLeJoueurLuiMemeDeLaVerificationDUnicite() {
        Joueur marie = joueurs.save(new Joueur("Marie"));

        assertThat(joueurs.existsByNomNormaliseAndAnonymiseFalseAndIdNot("marie", marie.getId())).isFalse();
        assertThat(joueurs.existsByNomNormaliseAndAnonymiseFalseAndIdNot("marie", marie.getId() + 1)).isTrue();
    }

    @Test
    void listeParPageLesJoueursActifsTriesParNom() {
        for (String nom : new String[] { "Zoé", "alex", "Marie" }) {
            joueurs.save(new Joueur(nom));
        }
        Joueur anonyme = new Joueur("Paul");
        anonyme.anonymiser();
        joueurs.save(anonyme);

        var page = joueurs.findByAnonymiseFalse(PageRequest.of(0, 2, Sort.by("nomNormalise")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Joueur::getNom).containsExactly("alex", "Marie");
    }

    @Test
    void listeLesJoueursSelectionnablesSansLesAnonymises() {
        joueurs.save(new Joueur("Bob"));
        joueurs.save(new Joueur("Alice"));
        Joueur anonyme = new Joueur("Carl");
        anonyme.anonymiser();
        joueurs.save(anonyme);

        assertThat(joueurs.findByAnonymiseFalseOrderByNomNormaliseAsc()).extracting(Joueur::getNom)
                .containsExactly("Alice", "Bob");
    }

    @Test
    void anonymiseEnConservantLaFiche() {
        Joueur joueur = new Joueur("Alex");
        joueur.setAvatar(ImageSource.url("https://exemple.org/a.png"));
        joueur.anonymiser();
        joueurs.saveAndFlush(joueur);

        Joueur relu = joueurs.findById(joueur.getId()).orElseThrow();

        assertThat(relu.isAnonymise()).isTrue();
        assertThat(relu.getNom()).isEqualTo(Joueur.NOM_ANONYME);
        assertThat(relu.getAvatar()).isNull();
    }
}
