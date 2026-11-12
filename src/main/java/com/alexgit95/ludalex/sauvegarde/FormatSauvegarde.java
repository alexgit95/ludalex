package com.alexgit95.ludalex.sauvegarde;

import java.util.List;
import java.util.Set;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.Pret;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Equipe;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.TokenFinPartie;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Format v1 du ZIP : manifeste, un fichier JSON par type d'enregistrement, images uploadées. */
final class FormatSauvegarde {

    static final int VERSION = 1;
    static final List<Integer> VERSIONS_ACCEPTEES = List.of(VERSION);

    static final String MANIFESTE = "manifest.json";
    static final String DOSSIER_DONNEES = "donnees/";
    static final String DOSSIER_IMAGES = "images/";

    static final String JOUEURS = DOSSIER_DONNEES + "joueurs.json";
    static final String JEUX = DOSSIER_DONNEES + "jeux.json";
    static final String PARTIES = DOSSIER_DONNEES + "parties.json";
    static final String EQUIPES = DOSSIER_DONNEES + "equipes.json";
    static final String PARTICIPATIONS = DOSSIER_DONNEES + "participations.json";
    static final String PRETS = DOSSIER_DONNEES + "prets.json";

    /** Ordre de l'import : chaque fichier ne référence que des fichiers qui le précèdent. */
    static final List<String> FICHIERS_DONNEES = List.of(JOUEURS, JEUX, PARTIES, EQUIPES, PARTICIPATIONS, PRETS);

    /** Toute entité JPA doit figurer dans l'une de ces deux listes (vérifié par un test). */
    static final Set<Class<?>> ENTITES_EXPORTEES = Set.of(Joueur.class, Jeu.class, Pret.class, Partie.class,
            Participation.class, Equipe.class);
    static final Set<Class<?>> ENTITES_EXCLUES = Set.of(TokenFinPartie.class);

    static final String COMPTEUR_IMAGES = "images";

    static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** Lecture d'un tableau enregistrement par enregistrement : les jetons suivants sont l'enregistrement suivant. */
    static final JsonMapper MAPPER_FLUX = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    private FormatSauvegarde() {
    }

    static String nomCompteur(String fichier) {
        return fichier.substring(DOSSIER_DONNEES.length(), fichier.length() - ".json".length());
    }
}
