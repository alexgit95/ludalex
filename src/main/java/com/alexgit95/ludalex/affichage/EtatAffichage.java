package com.alexgit95.ludalex.affichage;

import java.util.List;

import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.VuePartie;

/**
 * État de l'écran. {@code signature} change dès que l'affichage doit changer : l'écran ne remplace son contenu
 * que dans ce cas (sans quoi le diaporama redémarrerait à chaque interrogation).
 */
public record EtatAffichage(Type type, VuePartie vue, String qrSvg, List<Joueur> vainqueurs, boolean defaite,
        List<String> images, String signature) {

    public enum Type {
        EN_COURS, EN_PAUSE, VICTOIRE, VEILLE
    }

    /** Couleur de fond du jeu affiché, nulle en veille. */
    public String fond() {
        return vue == null ? null : vue.jeu().getCouleurFond();
    }

    /** Présentation des joueurs de la partie en cours, nulle en veille. */
    public PresentationJoueurs presentation() {
        return vue == null ? null : PresentationJoueurs.de(vue);
    }
}
