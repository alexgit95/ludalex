package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuInvalideException;
import com.alexgit95.ludalex.jeux.JeuSaisie;
import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurService;

/** Persistance réelle (H2) : clés étrangères, unicité et ordre des suppressions. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:parties;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class PartiePersistanceTest {

    @Autowired
    private PartieService parties;

    @Autowired
    private JeuService jeux;

    @Autowired
    private JoueurService joueurs;

    @Autowired
    private ParticipationRepository participations;

    private static String unique(String prefixe) {
        return prefixe + "-" + UUID.randomUUID();
    }

    private Jeu jeu(boolean cooperatif) {
        return jeux.creer(new JeuSaisie(unique("Jeu"), null, null, cooperatif, null, EntreeImage.inchangee()));
    }

    private Joueur joueur() {
        return joueurs.creer(unique("Joueur"), EntreeImage.inchangee(), EntreeImage.inchangee());
    }

    private static PartieSaisie saisie(Jeu jeu, String lieu, List<Long> ids, List<Long> gagnants) {
        return new PartieSaisie(jeu.getId(), LocalDate.of(2026, 5, 2), lieu, ids, null, gagnants, null, null, null, null);
    }

    @Test
    void enregistreEtRelitUnePartieAvecEquipesEtScores() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        Joueur b = joueur();
        Joueur c = joueur();
        Map<Long, Integer> scores = new LinkedHashMap<>();
        scores.put(a.getId(), 10);
        scores.put(c.getId(), 4);
        Map<Long, Integer> equipeDe = new LinkedHashMap<>();
        equipeDe.put(a.getId(), 1);
        equipeDe.put(b.getId(), 1);
        equipeDe.put(c.getId(), 2);

        Partie partie = parties.declarer(new PartieSaisie(catan.getId(), LocalDate.of(2026, 5, 2), "Chez Marie",
                List.of(a.getId(), b.getId(), c.getId()), scores, null, equipeDe, Map.of(1, "Les Rouges"), List.of(1), null));

        DetailPartie detail = parties.detail(partie.getId());
        assertThat(detail.partie().getJeu().getId()).isEqualTo(catan.getId());
        assertThat(detail.partie().getLieu()).isEqualTo("Chez Marie");
        assertThat(detail.equipes()).extracting(Equipe::getNom).containsExactlyInAnyOrder("Les Rouges", "Équipe 2");
        assertThat(detail.participations()).hasSize(3);
        assertThat(detail.participations()).filteredOn(Participation::isGagnant)
                .extracting(p -> p.getJoueur().getId()).containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(detail.participations()).filteredOn(p -> p.getJoueur().getId().equals(a.getId()))
                .singleElement().satisfies(p -> {
                    assertThat(p.getScore()).isEqualTo(10);
                    assertThat(p.getEquipe().getNom()).isEqualTo("Les Rouges");
                });
    }

    @Test
    void modifieUnePartieSansViolerLUniciteNiLesCles() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        Joueur b = joueur();
        Joueur c = joueur();
        Partie partie = parties.declarer(new PartieSaisie(catan.getId(), null, null, List.of(a.getId(), b.getId()), null,
                List.of(a.getId()), Map.of(a.getId(), 1, b.getId(), 2), null, null, null));

        parties.modifier(partie.getId(), new PartieSaisie(catan.getId(), null, "Nouveau lieu",
                List.of(a.getId(), c.getId()), Map.of(c.getId(), 12), List.of(c.getId()),
                Map.of(a.getId(), 1, c.getId(), 1), Map.of(1, "Duo"), null, null));

        DetailPartie detail = parties.detail(partie.getId());
        assertThat(detail.partie().getLieu()).isEqualTo("Nouveau lieu");
        assertThat(detail.equipes()).extracting(Equipe::getNom).containsExactly("Duo");
        assertThat(detail.participations()).extracting(p -> p.getJoueur().getId())
                .containsExactlyInAnyOrder(a.getId(), c.getId());
        assertThat(detail.participations()).filteredOn(Participation::isGagnant)
                .extracting(p -> p.getJoueur().getId()).containsExactly(c.getId());
        assertThat(participations.existsByJoueurId(b.getId())).isFalse();
    }

    @Test
    void supprimeUnePartieAvecEquipesSansViolerLesCles() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        Joueur b = joueur();
        Partie partie = parties.declarer(new PartieSaisie(catan.getId(), null, null, List.of(a.getId(), b.getId()), null,
                List.of(a.getId()), Map.of(a.getId(), 1, b.getId(), 2), null, null, null));

        parties.supprimer(partie.getId());

        assertThatThrownBy(() -> parties.detail(partie.getId())).isInstanceOf(PartieIntrouvableException.class);
        assertThat(participations.existsByJoueurId(a.getId())).isFalse();
    }

    @Test
    void anonymiseUnJoueurAyantDesPartiesEtConserveLHistorique() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        Joueur b = joueur();
        Partie partie = parties.declarer(saisie(catan, null, List.of(a.getId(), b.getId()), List.of(a.getId())));

        joueurs.supprimer(a.getId());

        assertThat(joueurs.selectionnables()).extracting(Joueur::getId).doesNotContain(a.getId()).contains(b.getId());
        DetailPartie detail = parties.detail(partie.getId());
        assertThat(detail.participations()).hasSize(2);
        assertThat(detail.participations()).filteredOn(p -> p.getJoueur().getId().equals(a.getId()))
                .singleElement().satisfies(p -> {
                    assertThat(p.getJoueur().getNom()).isEqualTo(Joueur.NOM_ANONYME);
                    assertThat(p.isGagnant()).isTrue();
                });
        parties.modifier(partie.getId(), saisie(catan, "Salon", List.of(a.getId(), b.getId()), List.of(a.getId())));
        assertThatThrownBy(() -> parties.declarer(saisie(catan, null, List.of(a.getId()), List.of(a.getId()))))
                .isInstanceOf(PartieInvalideException.class);
    }

    @Test
    void supprimeDefinitivementUnJoueurSansPartie() {
        Joueur a = joueur();

        joueurs.supprimer(a.getId());

        assertThat(joueurs.selectionnables()).extracting(Joueur::getId).doesNotContain(a.getId());
    }

    @Test
    void refuseLaSuppressionDUnJeuAvecDesParties() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        parties.declarer(saisie(catan, null, List.of(a.getId()), List.of(a.getId())));

        assertThatThrownBy(() -> jeux.supprimer(catan.getId())).isInstanceOf(JeuInvalideException.class);
        Jeu sansPartie = jeu(false);
        jeux.supprimer(sansPartie.getId());
    }

    @Test
    void listeLesPartiesParJeuAvecLeursParticipantsSansChargementParPartie() {
        Jeu catan = jeu(false);
        Jeu azul = jeu(false);
        Joueur a = joueur();
        Joueur b = joueur();
        parties.declarer(saisie(catan, null, List.of(a.getId(), b.getId()), List.of(a.getId())));
        parties.declarer(saisie(azul, null, List.of(b.getId()), List.of(b.getId())));

        var page = parties.lister(catan.getId(), 0);

        assertThat(page.getTotalElements()).isEqualTo(1);
        LignePartie ligne = page.getContent().get(0);
        assertThat(ligne.partie().getJeu().getNom()).isEqualTo(catan.getNom());
        assertThat(ligne.participations()).extracting(p -> p.getJoueur().getNom()).hasSize(2);
        assertThat(parties.lister(null, 0).getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void suggereLesLieuxDejaSaisisSansDoublonEnIgnorantLaCasse() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        String base = "Lz" + UUID.randomUUID().toString().substring(0, 6);
        for (String lieu : new String[] { base + " Maison", base + " Maison", base + " Bureau", "Autre" + base, base + "%" }) {
            parties.declarer(saisie(catan, lieu, List.of(a.getId()), List.of(a.getId())));
        }

        assertThat(parties.suggererLieux(base.toUpperCase())).containsExactly(base + " Bureau", base + " Maison", base + "%");
        assertThat(parties.suggererLieux(base + "%")).containsExactly(base + "%");
        assertThat(parties.suggererLieux("zzz-inconnu")).isEmpty();
    }

    @Test
    void limiteLesSuggestionsADix() {
        Jeu catan = jeu(false);
        Joueur a = joueur();
        String base = "Lim" + UUID.randomUUID().toString().substring(0, 6);
        for (int i = 0; i < 12; i++) {
            parties.declarer(saisie(catan, base + " " + i, List.of(a.getId()), List.of(a.getId())));
        }

        assertThat(parties.suggererLieux(base)).hasSize(PartieService.LIEUX_MAX);
    }

    @Test
    void enregistreUnePartieCooperative() {
        Jeu pandemic = jeu(true);
        Joueur a = joueur();
        Joueur b = joueur();

        Partie partie = parties.declarer(new PartieSaisie(pandemic.getId(), null, null, List.of(a.getId(), b.getId()),
                null, null, null, null, null, ResultatCooperatif.VICTOIRE));

        DetailPartie detail = parties.detail(partie.getId());
        assertThat(detail.partie().getResultatCooperatif()).isEqualTo(ResultatCooperatif.VICTOIRE);
        assertThat(detail.participations()).allMatch(Participation::isGagnant);
    }
}
