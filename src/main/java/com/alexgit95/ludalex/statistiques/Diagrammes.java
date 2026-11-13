package com.alexgit95.ludalex.statistiques;

import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Anneau;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Barre;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Calendrier;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Cellule;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Courbe;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Tranche;

/** Calculs d'échelle et de géométrie des diagrammes : les gabarits n'affichent que des valeurs déjà calculées. */
final class Diagrammes {

    static final int COULEURS = 6;
    static final int SEMAINES_MAX = 53;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);

    private Diagrammes() {
    }

    /** Hauteur relative à la plus grande valeur ; une valeur non nulle reste visible (3 % au minimum). */
    static List<Barre> barres(List<String> libelles, List<Long> valeurs) {
        long maximum = valeurs.stream().mapToLong(Long::longValue).max().orElse(0);
        List<Barre> barres = new ArrayList<>();
        for (int i = 0; i < libelles.size(); i++) {
            long valeur = valeurs.get(i);
            int hauteur = maximum == 0 || valeur <= 0 ? 0 : (int) Math.max(3, Math.round(valeur * 100.0 / maximum));
            barres.add(new Barre(libelles.get(i), valeur, hauteur));
        }
        return barres;
    }

    /** Anneau de circonférence 100 démarrant en haut ; au plus {@link #COULEURS} teintes, répétées au-delà. */
    static Anneau anneau(List<String> libelles, List<Long> valeurs) {
        long total = valeurs.stream().mapToLong(Long::longValue).sum();
        List<Tranche> tranches = new ArrayList<>();
        double cumul = 0;
        for (int i = 0; i < libelles.size() && total > 0; i++) {
            long valeur = valeurs.get(i);
            double longueur = valeur * 100.0 / total;
            tranches.add(new Tranche(libelles.get(i), valeur, (int) Math.round(longueur), longueur, 25 - cumul,
                    i % COULEURS));
            cumul += longueur;
        }
        return new Anneau(tranches, total);
    }

    /** Polyligne SVG (100 x 30) d'une série cumulée ; vide s'il y a moins de deux points ou aucune valeur. */
    static Courbe courbe(List<Long> cumuls) {
        long total = cumuls.isEmpty() ? 0 : cumuls.get(cumuls.size() - 1);
        if (cumuls.size() < 2 || total <= 0) {
            return new Courbe("", total);
        }
        StringJoiner points = new StringJoiner(" ");
        for (int i = 0; i < cumuls.size(); i++) {
            double x = i * 100.0 / (cumuls.size() - 1);
            double y = 29 - 28.0 * cumuls.get(i) / total;
            points.add(String.format(Locale.ROOT, "%.1f,%.1f", x, y));
        }
        return new Courbe(points.toString(), total);
    }

    /**
     * Calendrier de chaleur par semaines (colonnes) et jours (lignes, lundi en haut) sur {@code [debut, fin]},
     * limité à {@link #SEMAINES_MAX} semaines.
     */
    static Calendrier calendrier(Map<LocalDate, Long> parJour, LocalDate debut, LocalDate fin) {
        LocalDate premierLundi = fin.with(previousOrSame(MONDAY)).minusWeeks(SEMAINES_MAX - 1L);
        LocalDate depart = debut.isBefore(premierLundi) ? premierLundi : debut;
        LocalDate lundiDepart = depart.with(previousOrSame(MONDAY));
        List<Cellule> cellules = new ArrayList<>();
        long total = 0;
        for (LocalDate jour = depart; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            long nombre = parJour.getOrDefault(jour, 0L);
            total += nombre;
            int colonne = (int) ChronoUnit.WEEKS.between(lundiDepart, jour.with(previousOrSame(MONDAY)));
            cellules.add(new Cellule(colonne, jour.getDayOfWeek().getValue() - 1, niveau(nombre),
                    JOUR.format(jour) + " : " + nombre + (nombre > 1 ? " parties" : " partie")));
        }
        int colonnes = cellules.isEmpty() ? 0 : cellules.get(cellules.size() - 1).colonne() + 1;
        return new Calendrier(cellules, colonnes, total);
    }

    static int niveau(long parties) {
        if (parties <= 0) {
            return 0;
        }
        if (parties == 1) {
            return 1;
        }
        if (parties == 2) {
            return 2;
        }
        return parties <= 4 ? 3 : 4;
    }
}
