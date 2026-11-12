package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuRepository;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurRepository;

/** Règles de la partie en direct portées par {@link PartieService} : création en cours et clôture. */
class PartieServiceDirectTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-12T10:00:00Z");
    private static final Clock HORLOGE = Clock.fixed(MAINTENANT, ZoneOffset.UTC);

    private PartieRepository parties;
    private TokenFinPartieRepository tokens;
    private JeuRepository jeux;
    private JoueurRepository joueurs;
    private PartieService service;
    private Jeu catan;
    private Jeu pandemic;
    private final Map<Long, Joueur> annuaire = new HashMap<>();

    @BeforeEach
    void preparer() {
        parties = mock(PartieRepository.class);
        tokens = mock(TokenFinPartieRepository.class);
        jeux = mock(JeuRepository.class);
        joueurs = mock(JoueurRepository.class);
        when(parties.save(any(Partie.class))).thenAnswer(appel -> appel.getArgument(0));
        catan = jeu(1, "Catan", false);
        pandemic = jeu(2, "Pandemic", true);
        when(jeux.findById(1L)).thenReturn(Optional.of(catan));
        when(jeux.findById(2L)).thenReturn(Optional.of(pandemic));
        for (long id = 10; id < 13; id++) {
            Joueur joueur = new Joueur("Joueur" + id);
            ReflectionTestUtils.setField(joueur, "id", id);
            annuaire.put(id, joueur);
        }
        when(joueurs.findAllById(anyCollection())).thenAnswer(appel -> {
            List<Joueur> trouves = new ArrayList<>();
            for (Object id : (Iterable<?>) appel.getArgument(0)) {
                if (annuaire.containsKey(id)) {
                    trouves.add(annuaire.get(id));
                }
            }
            return trouves;
        });
        service = new PartieService(parties, jeux, joueurs, tokens, HORLOGE);
    }

    private static Jeu jeu(long id, String nom, boolean cooperatif) {
        Jeu jeu = new Jeu(nom);
        jeu.setCooperatif(cooperatif);
        ReflectionTestUtils.setField(jeu, "id", id);
        return jeu;
    }

    private Partie enCours(Jeu jeu, Map<Long, Equipe> equipes) {
        Partie partie = new Partie(jeu, LocalDate.of(2026, 11, 12));
        partie.setStatut(StatutPartie.EN_COURS);
        for (long id = 10; id < 13; id++) {
            Participation participation = new Participation(partie, annuaire.get(id));
            participation.setEquipe(equipes.get(id));
            partie.getParticipations().add(participation);
        }
        return partie;
    }

    private static Participation de(Partie partie, long joueurId) {
        return partie.getParticipations().stream()
                .filter(participation -> participation.getJoueur().getId() == joueurId).findFirst().orElseThrow();
    }

    @Test
    void creeUnePartieEnCoursSansResultatAvecLaDateDuJour() {
        Partie partie = service.creerEnCours(new LancementSaisie(1L, List.of(10L, 11L), null, null));

        assertThat(partie.getStatut()).isEqualTo(StatutPartie.EN_COURS);
        assertThat(partie.getDatePartie()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(partie.getTermineeLe()).isNull();
        assertThat(partie.getParticipations()).hasSize(2).allMatch(p -> !p.isGagnant() && p.getScore() == null);
    }

    @Test
    void creeLesEquipesDeLaPartieEnDirect() {
        Partie partie = service.creerEnCours(new LancementSaisie(1L, List.of(10L, 11L, 12L),
                Map.of(10L, 1, 11L, 1, 12L, 2), Map.of(1, "Les Rouges")));

        assertThat(partie.getEquipes()).extracting(Equipe::getNom).containsExactly("Les Rouges", "Équipe 2");
        assertThat(de(partie, 10).getEquipe()).isSameAs(de(partie, 11).getEquipe());
    }

    @Test
    void refuseUnLancementInvalide() {
        assertThatThrownBy(() -> service.creerEnCours(new LancementSaisie(null, List.of(10L), null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.creerEnCours(new LancementSaisie(99L, List.of(10L), null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.creerEnCours(new LancementSaisie(1L, List.of(), null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.creerEnCours(new LancementSaisie(1L, List.of(10L, 10L), null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.creerEnCours(new LancementSaisie(1L, List.of(10L, 99L), null, null)))
                .isInstanceOf(PartieInvalideException.class);
        annuaire.get(11L).anonymiser();
        assertThatThrownBy(() -> service.creerEnCours(new LancementSaisie(1L, List.of(10L, 11L), null, null)))
                .isInstanceOf(PartieInvalideException.class).hasMessageContaining("indisponible");
        assertThatThrownBy(() -> service.creerEnCours(new LancementSaisie(1L, List.of(10L), Map.of(10L, 1),
                Map.of(3, "Fantômes")))).isInstanceOf(PartieInvalideException.class);
    }

    @Test
    void clotureUnePartieCompetitiveAvecGagnantsScoresEtHorodatage() {
        Partie partie = enCours(catan, Map.of());
        partie.setJoueurSuivant(annuaire.get(10L));

        service.cloturer(partie, new FinSaisie(List.of(10L, 12L), null, Map.of(10L, 9, 12L, 11), null));

        assertThat(partie.getStatut()).isEqualTo(StatutPartie.TERMINEE);
        assertThat(partie.getTermineeLe()).isEqualTo(MAINTENANT);
        assertThat(partie.getJoueurSuivant()).isNull();
        assertThat(de(partie, 10).isGagnant()).isTrue();
        assertThat(de(partie, 11).isGagnant()).isFalse();
        assertThat(de(partie, 12).getScore()).isEqualTo(11);
        assertThat(de(partie, 11).getScore()).isNull();
        verify(parties).save(partie);
    }

    @Test
    void clotureAvecUneEquipeGagnanteDesignee() {
        Equipe rouges = new Equipe(null, "Les Rouges");
        ReflectionTestUtils.setField(rouges, "id", 77L);
        Partie partie = enCours(catan, Map.of(10L, rouges, 11L, rouges));

        service.cloturer(partie, new FinSaisie(null, List.of(77L), null, null));

        assertThat(de(partie, 10).isGagnant()).isTrue();
        assertThat(de(partie, 11).isGagnant()).isTrue();
        assertThat(de(partie, 12).isGagnant()).isFalse();
    }

    @Test
    void refuseUneEquipeGagnanteEtrangere() {
        Partie partie = enCours(catan, Map.of());

        assertThatThrownBy(() -> service.cloturer(partie, new FinSaisie(null, List.of(999L), null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThat(partie.getStatut()).isEqualTo(StatutPartie.EN_COURS);
    }

    @Test
    void refuseUneClotureSansGagnantAvecUnGagnantEtrangerOuUnScoreEtranger() {
        Partie partie = enCours(catan, Map.of());

        assertThatThrownBy(() -> service.cloturer(partie, new FinSaisie(List.of(), null, null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.cloturer(partie, new FinSaisie(null, null, null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.cloturer(partie, new FinSaisie(List.of(99L), null, null, null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThatThrownBy(() -> service.cloturer(partie, new FinSaisie(List.of(10L), null, Map.of(99L, 1), null)))
                .isInstanceOf(PartieInvalideException.class);
        assertThat(partie.getStatut()).isEqualTo(StatutPartie.EN_COURS);
        assertThat(partie.getTermineeLe()).isNull();
    }

    @Test
    void clotureUnePartieCooperative() {
        Partie victoire = enCours(pandemic, Map.of());
        Partie defaite = enCours(pandemic, Map.of());

        service.cloturer(victoire, new FinSaisie(List.of(10L), null, null, ResultatCooperatif.VICTOIRE));
        service.cloturer(defaite, new FinSaisie(null, null, null, ResultatCooperatif.DEFAITE));

        assertThat(victoire.getResultatCooperatif()).isEqualTo(ResultatCooperatif.VICTOIRE);
        assertThat(victoire.getParticipations()).allMatch(Participation::isGagnant);
        assertThat(defaite.getResultatCooperatif()).isEqualTo(ResultatCooperatif.DEFAITE);
        assertThat(defaite.getParticipations()).noneMatch(Participation::isGagnant);
        assertThatThrownBy(() -> service.cloturer(enCours(pandemic, Map.of()), new FinSaisie(null, null, null, null)))
                .isInstanceOf(PartieInvalideException.class);
    }

    @Test
    void supprimerUnePartieSupprimeAussiSesTokensAvantLaPartie() {
        Partie partie = enCours(catan, Map.of());
        ReflectionTestUtils.setField(partie, "id", 5L);
        when(parties.findById(5L)).thenReturn(Optional.of(partie));

        service.supprimer(5L);

        InOrder ordre = Mockito.inOrder(parties, tokens);
        ordre.verify(parties).flush();
        ordre.verify(tokens).supprimerParPartie(5L);
        ordre.verify(parties).delete(partie);
    }
}
