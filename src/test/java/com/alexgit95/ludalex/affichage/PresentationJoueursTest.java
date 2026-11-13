package com.alexgit95.ludalex.affichage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.alexgit95.ludalex.affichage.PresentationJoueurs.Groupe;
import com.alexgit95.ludalex.affichage.PresentationJoueurs.Mode;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Equipe;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.VuePartie;

class PresentationJoueursTest {

    private static final Joueur ANNE = joueur(1, "Anne");
    private static final Joueur MARC = joueur(2, "Marc");
    private static final Joueur LEA = joueur(3, "Léa");
    private static final Joueur PAUL = joueur(4, "Paul");

    private static Joueur joueur(long id, String nom) {
        Joueur joueur = new Joueur(nom);
        ReflectionTestUtils.setField(joueur, "id", id);
        return joueur;
    }

    private static Partie partie(boolean cooperatif) {
        Jeu jeu = new Jeu("Catan");
        jeu.setCooperatif(cooperatif);
        return new Partie(jeu, LocalDate.of(2026, 11, 13));
    }

    private static Equipe equipe(Partie partie, long id, String nom) {
        Equipe equipe = new Equipe(partie, nom);
        ReflectionTestUtils.setField(equipe, "id", id);
        return equipe;
    }

    private static Participation participation(Partie partie, Joueur joueur, Equipe equipe) {
        Participation participation = new Participation(partie, joueur);
        ReflectionTestUtils.setField(participation, "equipe", equipe);
        return participation;
    }

    private static VuePartie vue(Partie partie, List<Equipe> equipes, Participation... participations) {
        return new VuePartie(partie, partie.getJeu(), List.of(participations), equipes, null);
    }

    @Test
    void duelPourDeuxJoueursSansEquipe() {
        Partie partie = partie(false);

        PresentationJoueurs presentation = PresentationJoueurs.de(vue(partie, List.of(),
                participation(partie, ANNE, null), participation(partie, MARC, null)));

        assertThat(presentation.mode()).isEqualTo(Mode.DUEL);
        assertThat(presentation.versus()).isTrue();
        assertThat(presentation.ensemble()).isFalse();
        assertThat(presentation.groupes()).extracting(Groupe::joueurs)
                .containsExactly(List.of(ANNE), List.of(MARC));
        assertThat(presentation.groupes()).extracting(Groupe::nom).containsOnlyNulls();
    }

    @Test
    void multiPourTroisJoueursOuPlusSansEquipe() {
        Partie partie = partie(false);

        PresentationJoueurs presentation = PresentationJoueurs.de(vue(partie, List.of(),
                participation(partie, ANNE, null), participation(partie, MARC, null),
                participation(partie, LEA, null)));

        assertThat(presentation.mode()).isEqualTo(Mode.MULTI);
        assertThat(presentation.versus()).isTrue();
        assertThat(presentation.groupes()).hasSize(3);
    }

    @Test
    void soloSansVsPourUnSeulJoueur() {
        Partie partie = partie(false);

        PresentationJoueurs presentation = PresentationJoueurs.de(vue(partie, List.of(),
                participation(partie, ANNE, null)));

        assertThat(presentation.mode()).isEqualTo(Mode.SOLO);
        assertThat(presentation.versus()).isFalse();
        assertThat(presentation.ensemble()).isFalse();
        assertThat(presentation.groupes()).hasSize(1);
    }

    @Test
    void cooperatifRegroupeTousLesJoueursSansVs() {
        Partie partie = partie(true);

        PresentationJoueurs presentation = PresentationJoueurs.de(vue(partie, List.of(),
                participation(partie, ANNE, null), participation(partie, MARC, null)));

        assertThat(presentation.mode()).isEqualTo(Mode.COOPERATIF);
        assertThat(presentation.versus()).isFalse();
        assertThat(presentation.ensemble()).isTrue();
        assertThat(presentation.groupes()).singleElement().extracting(Groupe::joueurs)
                .isEqualTo(List.of(ANNE, MARC));
    }

    @Test
    void cooperatifPrimeSurLesEquipes() {
        Partie partie = partie(true);
        Equipe rouge = equipe(partie, 10, "Rouge");

        PresentationJoueurs presentation = PresentationJoueurs.de(vue(partie, List.of(rouge),
                participation(partie, ANNE, rouge), participation(partie, MARC, rouge)));

        assertThat(presentation.mode()).isEqualTo(Mode.COOPERATIF);
    }

    @Test
    void equipesDansLOrdreDeLaPartieAvecLeurNom() {
        Partie partie = partie(false);
        Equipe rouge = equipe(partie, 10, "Rouge");
        Equipe bleue = equipe(partie, 11, "Bleue");

        PresentationJoueurs presentation = PresentationJoueurs.de(vue(partie, List.of(rouge, bleue),
                participation(partie, MARC, bleue), participation(partie, ANNE, rouge),
                participation(partie, LEA, bleue), participation(partie, PAUL, rouge)));

        assertThat(presentation.mode()).isEqualTo(Mode.EQUIPES);
        assertThat(presentation.versus()).isTrue();
        assertThat(presentation.groupes()).containsExactly(
                new Groupe("Rouge", List.of(ANNE, PAUL)), new Groupe("Bleue", List.of(MARC, LEA)));
    }

    @Test
    void joueurSansEquipeFormeUnGroupeSansNomALaFin() {
        Partie partie = partie(false);
        Equipe rouge = equipe(partie, 10, "Rouge");

        PresentationJoueurs presentation = PresentationJoueurs.de(vue(partie, List.of(rouge),
                participation(partie, ANNE, rouge), participation(partie, MARC, null),
                participation(partie, LEA, rouge)));

        assertThat(presentation.mode()).isEqualTo(Mode.EQUIPES);
        assertThat(presentation.groupes()).containsExactly(
                new Groupe("Rouge", List.of(ANNE, LEA)), new Groupe(null, List.of(MARC)));
    }

    @Test
    void ometLesEquipesSansMembre() {
        Partie partie = partie(false);
        Equipe rouge = equipe(partie, 10, "Rouge");
        Equipe vide = equipe(partie, 11, "Vide");
        Equipe bleue = equipe(partie, 12, "Bleue");

        PresentationJoueurs presentation = PresentationJoueurs.de(vue(partie, List.of(rouge, vide, bleue),
                participation(partie, ANNE, rouge), participation(partie, MARC, bleue)));

        assertThat(presentation.groupes()).extracting(Groupe::nom).containsExactly("Rouge", "Bleue");
    }

    @Test
    void troisEquipesDonnentTroisGroupes() {
        Partie partie = partie(false);
        Equipe a = equipe(partie, 10, "A");
        Equipe b = equipe(partie, 11, "B");
        Equipe c = equipe(partie, 12, "C");

        PresentationJoueurs presentation = PresentationJoueurs.de(vue(partie, List.of(a, b, c),
                participation(partie, ANNE, a), participation(partie, MARC, b), participation(partie, LEA, c)));

        assertThat(presentation.groupes()).hasSize(3);
    }

    @Test
    void etatSansVueNAPasDePresentation() {
        EtatAffichage veille = new EtatAffichage(EtatAffichage.Type.VEILLE, null, null, List.of(), false,
                List.of(), "VEILLE");

        assertThat(veille.presentation()).isNull();
    }
}
