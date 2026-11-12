package com.alexgit95.ludalex.jeux;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.alexgit95.ludalex.images.EntreeImage;

/** Saisie d'un jeu ; {@code couleur} n'est renseignée que si l'utilisateur la choisit à la main. */
public record JeuSaisie(String nom, BigDecimal prix, LocalDate dateAchat, boolean cooperatif, String couleur,
        EntreeImage image) {
}
