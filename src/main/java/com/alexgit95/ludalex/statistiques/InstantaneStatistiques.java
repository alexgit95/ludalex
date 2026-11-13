package com.alexgit95.ludalex.statistiques;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Instantané immuable du tableau de bord : valeurs déjà mises en forme pour l'affichage (pourcentages, échelles
 * et coordonnées SVG calculés en Java), afin que les gabarits ne contiennent aucun calcul.
 */
public record InstantaneStatistiques(PeriodeStatistiques periode, LocalDateTime calculeLe, List<Kpi> kpis,
        Activite activite, Jeux jeux, Joueurs joueurs, Ludotheque ludotheque, Prets prets) {

    /** Chiffre clé ; {@code detail} précise l'unité ou la comparaison. */
    public record Kpi(String etiquette, String valeur, String detail) {
    }

    /** Barre d'un diagramme ; {@code hauteur} est en pourcentage de la plus grande barre. */
    public record Barre(String libelle, long valeur, int hauteur) {
    }

    /** Part d'un anneau : {@code longueur} et {@code decalage} sont exprimés sur une circonférence de 100. */
    public record Tranche(String libelle, long valeur, int pourcent, double longueur, double decalage, int couleur) {
    }

    public record Anneau(List<Tranche> tranches, long total) {
        public boolean vide() {
            return total == 0;
        }
    }

    /** Case du calendrier ; {@code niveau} va de 0 (aucune partie) à 4. */
    public record Cellule(int colonne, int ligne, int niveau, String titre) {
    }

    public record Calendrier(List<Cellule> cellules, int colonnes, long total) {
        public boolean vide() {
            return total == 0;
        }
    }

    /** Courbe cumulée ; {@code points} est la liste de coordonnées d'une polyligne SVG de 100 x 30. */
    public record Courbe(String points, long total) {
        public boolean vide() {
            return points.isEmpty();
        }
    }

    /** Ligne d'un tableau : libellé, valeur principale et précision. */
    public record Ligne(String libelle, String valeur, String detail) {
    }

    public record LignePret(String jeu, String emprunteur, long jours, boolean longue) {
    }

    public record Activite(String titreChronologie, List<Barre> chronologie, List<Barre> parJourSemaine,
            Calendrier calendrier, Courbe cumul, Anneau lieux, long parties) {
        public boolean vide() {
            return parties == 0;
        }
    }

    public record Jeux(List<Barre> plusJoues, List<Barre> plusGagnes, long jamaisJoues, List<String> jamaisJouesListe,
            long dormants, List<Ligne> dormantsListe, int moisDormant, Anneau modes, Integer tauxCooperatif,
            Ligne jeuDifficile, List<Barre> variantes, List<Ligne> coutParPartie) {
    }

    public record Joueurs(List<Ligne> classement, List<Barre> partiesParJoueur, List<Ligne> records,
            List<Ligne> duos, List<Barre> polyvalence, int minimumParties) {
    }

    public record Ludotheque(List<Barre> achatsParMois, List<Barre> achatsParAnnee, Courbe investissement,
            List<Ligne> plusChers, long sansPrix, long sansDate) {
    }

    public record Prets(List<Barre> parMois, List<Barre> emprunteurs, String dureeMoyenne, long enCours,
            List<LignePret> enCoursListe, int seuilJours) {
    }
}
