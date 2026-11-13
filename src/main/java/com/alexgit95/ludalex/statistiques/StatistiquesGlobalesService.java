package com.alexgit95.ludalex.statistiques;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.alexgit95.ludalex.reglages.ReglageCle;
import com.alexgit95.ludalex.reglages.ReglagesService;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Activite;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Barre;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Jeux;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Joueurs;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Kpi;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Ligne;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.LignePret;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Ludotheque;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Prets;
import com.alexgit95.ludalex.statistiques.PeriodeStatistiques.Fenetre;
import com.alexgit95.ludalex.statistiques.StatistiquesGlobalesRepository.CoutJeu;
import com.alexgit95.ludalex.statistiques.StatistiquesGlobalesRepository.JourCompte;
import com.alexgit95.ludalex.statistiques.StatistiquesGlobalesRepository.NomCompte;
import com.alexgit95.ludalex.statistiques.StatistiquesGlobalesRepository.TauxCooperatif;

/**
 * Tableau de bord global : un instantané complet par période, calculé en un nombre constant de requêtes agrégées
 * puis conservé par {@link CacheStatistiques}.
 */
@Service
public class StatistiquesGlobalesService {

    static final int TOP = 10;
    static final int TOP_COURT = 5;
    static final int LIGNES_LIEUX = 6;
    static final int MOIS_MAX = 36;

    private static final DateTimeFormatter FORMAT_MOIS = DateTimeFormatter.ofPattern("MMM yy", Locale.FRANCE);
    private static final DateTimeFormatter FORMAT_JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);

    private final StatistiquesGlobalesRepository depot;
    private final ReglagesService reglages;
    private final CacheStatistiques cache;
    private final Clock horloge;

    public StatistiquesGlobalesService(StatistiquesGlobalesRepository depot, ReglagesService reglages,
            CacheStatistiques cache, Clock horloge) {
        this.depot = depot;
        this.reglages = reglages;
        this.cache = cache;
        this.horloge = horloge;
    }

    public InstantaneStatistiques instantane(PeriodeStatistiques periode) {
        return cache.obtenir(periode, () -> calculer(periode));
    }

    InstantaneStatistiques calculer(PeriodeStatistiques periode) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        LocalDate aujourdhui = maintenant.toLocalDate();
        Fenetre fenetre = periode.fenetre(aujourdhui);
        Map<ReglageCle, Integer> seuils = reglages.tous();

        Map<LocalDate, Long> partiesParJour = new TreeMap<>();
        for (JourCompte jour : depot.partiesParJour()) {
            partiesParJour.put(jour.jour(), jour.nombre());
        }
        long parties = somme(partiesParJour, fenetre.debut(), fenetre.fin());
        Long precedentes = fenetre.precedente() == null ? null
                : somme(partiesParJour, fenetre.precedente().debut(), fenetre.precedente().fin());

        StatistiquesGlobalesRepository.Ludotheque ludotheque = depot.ludotheque();
        List<StatistiquesGlobalesRepository.JoueurResultat> joueurs = depot.joueurs(fenetre.debut(), fenetre.fin());
        List<JourCompte> pretsParJour = depot.pretsParJour(fenetre.debut(), fenetre.fin());
        long pretsEnCours = depot.pretsEnCours();

        List<Kpi> kpis = kpis(parties, precedentes, ludotheque, joueurs.size(), pretsEnCours,
                pretsParJour.stream().mapToLong(JourCompte::nombre).sum(),
                depot.jeuxDistinctsJoues(fenetre.debut(), fenetre.fin()), serieSemaines(partiesParJour, aujourdhui));

        return new InstantaneStatistiques(periode, maintenant, kpis,
                activite(periode, fenetre, partiesParJour, parties),
                jeux(fenetre, aujourdhui, seuils), joueurs(fenetre, joueurs, seuils),
                ludotheque(periode, fenetre, ludotheque),
                prets(periode, fenetre, aujourdhui, pretsParJour, pretsEnCours, seuils));
    }

    // ---- Chiffres clés ----

    private List<Kpi> kpis(long parties, Long precedentes, StatistiquesGlobalesRepository.Ludotheque ludotheque,
            long joueursActifs, long pretsEnCours, long pretsPeriode, long jeuxJoues, long semaines) {
        NumberFormat euros = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        BigDecimal valeur = ludotheque.valeur() == null ? BigDecimal.ZERO : ludotheque.valeur();
        String moyenne = ludotheque.avecPrix() == 0 ? "prix moyen indisponible"
                : "prix moyen " + euros.format(valeur.divide(BigDecimal.valueOf(ludotheque.avecPrix()), 2,
                        RoundingMode.HALF_UP));
        long sansPrix = ludotheque.jeux() - ludotheque.avecPrix();
        List<Kpi> kpis = new ArrayList<>();
        kpis.add(new Kpi("Parties", Long.toString(parties), evolution(parties, precedentes)));
        kpis.add(new Kpi("Jeux joués", jeuxJoues + " / " + ludotheque.jeux(), "jeux distincts / ludothèque"));
        kpis.add(new Kpi("Joueurs actifs", Long.toString(joueursActifs), "ayant joué sur la période"));
        kpis.add(new Kpi("Ludothèque", Long.toString(ludotheque.jeux()), "dont " + ludotheque.cooperatifs()
                + " coopératif" + (ludotheque.cooperatifs() > 1 ? "s" : "")));
        kpis.add(new Kpi("Valeur", euros.format(valeur), moyenne + (sansPrix > 0 ? " · " + sansPrix + " jeu"
                + (sansPrix > 1 ? "x" : "") + " sans prix" : "")));
        kpis.add(new Kpi("Prêts", pretsEnCours + " en cours", pretsPeriode + " sur la période"));
        kpis.add(new Kpi("Série", semaines + " semaine" + (semaines > 1 ? "s" : ""),
                "consécutives avec au moins une partie"));
        return kpis;
    }

    static String evolution(long courant, Long precedent) {
        if (precedent == null) {
            return "tout l'historique";
        }
        if (precedent == 0) {
            return courant == 0 ? "stable par rapport à la période précédente" : "aucune partie la période précédente";
        }
        long pourcent = Math.round((courant - precedent) * 100.0 / precedent);
        return (pourcent > 0 ? "+" : "") + pourcent + " % par rapport à la période précédente";
    }

    /** Semaines consécutives (lundi à dimanche) avec au moins une partie, en remontant depuis la semaine courante. */
    static long serieSemaines(Map<LocalDate, Long> partiesParJour, LocalDate aujourdhui) {
        Set<LocalDate> semaines = new HashSet<>();
        for (LocalDate jour : partiesParJour.keySet()) {
            semaines.add(jour.with(DayOfWeek.MONDAY));
        }
        LocalDate semaine = aujourdhui.with(DayOfWeek.MONDAY);
        if (!semaines.contains(semaine)) {
            semaine = semaine.minusWeeks(1);
        }
        long serie = 0;
        while (semaines.contains(semaine)) {
            serie++;
            semaine = semaine.minusWeeks(1);
        }
        return serie;
    }

    // ---- Activité ----

    private Activite activite(PeriodeStatistiques periode, Fenetre fenetre, Map<LocalDate, Long> partiesParJour,
            long parties) {
        LocalDate debut = debutEffectif(fenetre, partiesParJour.keySet());
        Serie serie = serie(periode, partiesParJour, debut, fenetre.fin());

        long[] semaine = new long[7];
        partiesParJour.forEach((jour, nombre) -> {
            if (!jour.isBefore(debut) && !jour.isAfter(fenetre.fin())) {
                semaine[jour.getDayOfWeek().getValue() - 1] += nombre;
            }
        });
        List<String> jours = new ArrayList<>();
        List<Long> valeursJours = new ArrayList<>();
        for (DayOfWeek jour : DayOfWeek.values()) {
            jours.add(capitale(jour.getDisplayName(TextStyle.SHORT, Locale.FRANCE)));
            valeursJours.add(semaine[jour.getValue() - 1]);
        }

        List<NomCompte> lieux = depot.lieux(fenetre.debut(), fenetre.fin(), LIGNES_LIEUX);
        List<String> libellesLieux = new ArrayList<>();
        List<Long> valeursLieux = new ArrayList<>();
        long listes = 0;
        for (NomCompte lieu : lieux) {
            libellesLieux.add(lieu.nom() == null ? "Non renseigné" : lieu.nom());
            valeursLieux.add(lieu.nombre());
            listes += lieu.nombre();
        }
        if (parties > listes) {
            libellesLieux.add("Autres");
            valeursLieux.add(parties - listes);
        }

        return new Activite(periode == PeriodeStatistiques.JOURS_30 ? "Parties par jour" : "Parties par mois",
                Diagrammes.barres(serie.libelles(), serie.valeurs()),
                Diagrammes.barres(jours, valeursJours),
                Diagrammes.calendrier(partiesParJour, debut, fenetre.fin()),
                Diagrammes.courbe(cumuler(serie.valeurs())), Diagrammes.anneau(libellesLieux, valeursLieux), parties);
    }

    // ---- Jeux ----

    private Jeux jeux(Fenetre fenetre, LocalDate aujourdhui, Map<ReglageCle, Integer> seuils) {
        int moisDormant = seuils.get(ReglageCle.JEU_DORMANT_MOIS);
        int minimum = seuils.get(ReglageCle.CLASSEMENT_PARTIES_MIN);
        LocalDate avant = aujourdhui.minusMonths(moisDormant);

        List<Ligne> dormants = depot.dormants(avant, TOP).stream()
                .map(d -> new Ligne(d.nom(), FORMAT_JOUR.format(d.date()), "dernière partie")).toList();

        List<StatistiquesGlobalesRepository.Cooperatif> modes = depot.repartitionCooperatif(fenetre.debut(),
                fenetre.fin());
        long competitives = modes.stream().filter(m -> !m.cooperatif()).mapToLong(m -> m.parties()).sum();
        long cooperatives = modes.stream().filter(m -> m.cooperatif()).mapToLong(m -> m.parties()).sum();

        List<TauxCooperatif> taux = depot.tauxCooperatifParJeu(fenetre.debut(), fenetre.fin());
        long partiesCoop = taux.stream().mapToLong(TauxCooperatif::parties).sum();
        long victoires = taux.stream().mapToLong(TauxCooperatif::victoires).sum();
        Integer tauxGlobal = partiesCoop == 0 ? null : (int) Math.round(victoires * 100.0 / partiesCoop);
        Ligne difficile = taux.stream().filter(t -> t.parties() >= minimum)
                .min(Comparator.comparingDouble((TauxCooperatif t) -> (double) t.victoires() / t.parties())
                        .thenComparing(TauxCooperatif::nom))
                .map(t -> new Ligne(t.nom(), Math.round(t.victoires() * 100.0 / t.parties()) + " %",
                        t.victoires() + " victoire" + (t.victoires() > 1 ? "s" : "") + " sur " + t.parties() + " parties"))
                .orElse(null);

        return new Jeux(barres(depot.jeuxLesPlusJoues(fenetre.debut(), fenetre.fin(), TOP)),
                barres(depot.jeuxLesPlusGagnes(fenetre.debut(), fenetre.fin(), TOP)),
                depot.jeuxJamaisJoues(), depot.nomsJeuxJamaisJoues(TOP), depot.jeuxDormants(avant), dormants,
                moisDormant,
                Diagrammes.anneau(List.of("Compétitif", "Coopératif"), List.of(competitives, cooperatives)),
                tauxGlobal, difficile, barres(depot.variantes(fenetre.debut(), fenetre.fin(), TOP)),
                coutParPartie(depot.coutsParJeu()));
    }

    static List<Ligne> coutParPartie(List<CoutJeu> couts) {
        NumberFormat euros = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        return couts.stream()
                .map(c -> Map.entry(c, c.prix().divide(BigDecimal.valueOf(c.parties()), 2, RoundingMode.HALF_UP)))
                .sorted(Map.Entry.<CoutJeu, BigDecimal>comparingByValue().thenComparing(e -> e.getKey().nom()))
                .limit(TOP)
                .map(e -> new Ligne(e.getKey().nom(), euros.format(e.getValue()), euros.format(e.getKey().prix())
                        + " pour " + e.getKey().parties() + " partie" + (e.getKey().parties() > 1 ? "s" : "")))
                .toList();
    }

    // ---- Joueurs ----

    private Joueurs joueurs(Fenetre fenetre, List<StatistiquesGlobalesRepository.JoueurResultat> joueurs,
            Map<ReglageCle, Integer> seuils) {
        int minimum = seuils.get(ReglageCle.CLASSEMENT_PARTIES_MIN);

        List<Ligne> classement = joueurs.stream().filter(j -> j.parties() >= minimum)
                .sorted(Comparator.comparingDouble((StatistiquesGlobalesRepository.JoueurResultat j) ->
                        -(double) j.victoires() / j.parties())
                        .thenComparing(StatistiquesGlobalesRepository.JoueurResultat::parties, Comparator.reverseOrder())
                        .thenComparing(StatistiquesGlobalesRepository.JoueurResultat::nom))
                .limit(TOP)
                .map(j -> new Ligne(j.nom(), Math.round(j.victoires() * 100.0 / j.parties()) + " %",
                        j.victoires() + " victoire" + (j.victoires() > 1 ? "s" : "") + " sur " + j.parties()
                                + " parties"))
                .toList();

        List<Barre> parties = barresJoueurs(joueurs,
                Comparator.comparingLong(StatistiquesGlobalesRepository.JoueurResultat::parties).reversed(),
                StatistiquesGlobalesRepository.JoueurResultat::parties, TOP);
        List<Barre> polyvalence = barresJoueurs(joueurs,
                Comparator.comparingLong(StatistiquesGlobalesRepository.JoueurResultat::jeux).reversed(),
                StatistiquesGlobalesRepository.JoueurResultat::jeux, TOP_COURT);

        NumberFormat decimal = NumberFormat.getNumberInstance(Locale.FRANCE);
        decimal.setMaximumFractionDigits(1);
        List<Ligne> records = depot.recordsScores(fenetre.debut(), fenetre.fin(), TOP).stream()
                .map(r -> new Ligne(r.jeu(), "max " + r.maximum(), "moyenne " + decimal.format(r.moyenne())
                        + " sur " + r.scores() + " score" + (r.scores() > 1 ? "s" : "")))
                .toList();
        List<Ligne> duos = depot.duos(fenetre.debut(), fenetre.fin(), TOP_COURT).stream()
                .map(d -> new Ligne(d.premier() + " & " + d.second(), d.parties() + " parties", null)).toList();

        return new Joueurs(classement, parties, records, duos, polyvalence, minimum);
    }

    private static List<Barre> barresJoueurs(List<StatistiquesGlobalesRepository.JoueurResultat> joueurs,
            Comparator<StatistiquesGlobalesRepository.JoueurResultat> ordre,
            java.util.function.ToLongFunction<StatistiquesGlobalesRepository.JoueurResultat> valeur, int limite) {
        List<StatistiquesGlobalesRepository.JoueurResultat> tries = joueurs.stream()
                .sorted(ordre.thenComparing(StatistiquesGlobalesRepository.JoueurResultat::nom)).limit(limite).toList();
        return Diagrammes.barres(tries.stream().map(StatistiquesGlobalesRepository.JoueurResultat::nom).toList(),
                tries.stream().map(j -> valeur.applyAsLong(j)).toList());
    }

    // ---- Ludothèque ----

    private Ludotheque ludotheque(PeriodeStatistiques periode, Fenetre fenetre,
            StatistiquesGlobalesRepository.Ludotheque ludotheque) {
        Map<LocalDate, Long> achats = new TreeMap<>();
        Map<LocalDate, Long> montants = new TreeMap<>();
        Map<Integer, Long> parAnnee = new TreeMap<>();
        for (StatistiquesGlobalesRepository.AchatJour achat : depot.achatsParJour(fenetre.debut(), fenetre.fin())) {
            achats.put(achat.jour(), achat.jeux());
            montants.put(achat.jour(), achat.montant() == null ? 0 : achat.montant().setScale(0, RoundingMode.HALF_UP)
                    .longValue());
            parAnnee.merge(achat.jour().getYear(), achat.jeux(), Long::sum);
        }
        LocalDate debut = debutEffectif(fenetre, achats.keySet());
        Serie mois = serie(periode, achats, debut, fenetre.fin());
        Serie euros = serie(periode, montants, debut, fenetre.fin());
        NumberFormat prix = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        List<Ligne> plusChers = depot.jeuxLesPlusChers(TOP).stream()
                .map(j -> new Ligne(j.nom(), prix.format(j.prix()), null)).toList();
        List<String> annees = parAnnee.keySet().stream().map(String::valueOf).toList();
        return new Ludotheque(Diagrammes.barres(mois.libelles(), mois.valeurs()),
                Diagrammes.barres(annees, new ArrayList<>(parAnnee.values())),
                Diagrammes.courbe(cumuler(euros.valeurs())), plusChers, ludotheque.jeux() - ludotheque.avecPrix(),
                ludotheque.jeux() - ludotheque.avecDate());
    }

    // ---- Prêts ----

    private Prets prets(PeriodeStatistiques periode, Fenetre fenetre, LocalDate aujourdhui, List<JourCompte> parJour,
            long enCours, Map<ReglageCle, Integer> seuils) {
        int seuil = seuils.get(ReglageCle.PRET_LONG_JOURS);
        Map<LocalDate, Long> prets = new TreeMap<>();
        parJour.forEach(p -> prets.put(p.jour(), p.nombre()));
        Serie mois = serie(periode, prets, debutEffectif(fenetre, prets.keySet()), fenetre.fin());

        long nombre = 0;
        long jours = 0;
        for (StatistiquesGlobalesRepository.DureePret pret : depot.pretsTermines(fenetre.debut(), fenetre.fin())) {
            nombre += pret.nombre();
            jours += ChronoUnit.DAYS.between(pret.debut(), pret.fin()) * pret.nombre();
        }
        NumberFormat decimal = NumberFormat.getNumberInstance(Locale.FRANCE);
        decimal.setMaximumFractionDigits(1);
        String moyenne = nombre == 0 ? null : decimal.format((double) jours / nombre) + " jours";

        List<LignePret> liste = depot.pretsEnCours(TOP).stream().map(p -> {
            long duree = ChronoUnit.DAYS.between(p.depuis(), aujourdhui);
            return new LignePret(p.jeu(), p.emprunteur(), duree, duree >= seuil);
        }).toList();

        return new Prets(Diagrammes.barres(mois.libelles(), mois.valeurs()),
                barres(depot.emprunteurs(fenetre.debut(), fenetre.fin(), TOP_COURT)), moyenne, enCours, liste, seuil);
    }

    // ---- Outils ----

    private record Serie(List<String> libelles, List<Long> valeurs) {
    }

    private static List<Barre> barres(List<NomCompte> valeurs) {
        return Diagrammes.barres(valeurs.stream().map(v -> v.nom() == null ? "Non renseigné" : v.nom()).toList(),
                valeurs.stream().map(NomCompte::nombre).toList());
    }

    private static long somme(Map<LocalDate, Long> parJour, LocalDate debut, LocalDate fin) {
        return parJour.entrySet().stream()
                .filter(e -> (debut == null || !e.getKey().isBefore(debut)) && !e.getKey().isAfter(fin))
                .mapToLong(Map.Entry::getValue).sum();
    }

    /** Début de la période, ou première date connue (ou fin) pour tout l'historique. */
    private static LocalDate debutEffectif(Fenetre fenetre, Set<LocalDate> dates) {
        if (fenetre.debut() != null) {
            return fenetre.debut();
        }
        return dates.stream().min(Comparator.naturalOrder()).orElse(fenetre.fin());
    }

    /** Une valeur par jour sur 30 jours, sinon une valeur par mois (36 mois au plus, les plus récents). */
    private static Serie serie(PeriodeStatistiques periode, Map<LocalDate, Long> parJour, LocalDate debut,
            LocalDate fin) {
        Map<String, Long> valeurs = new LinkedHashMap<>();
        if (periode == PeriodeStatistiques.JOURS_30) {
            for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
                valeurs.put(String.format(Locale.FRANCE, "%02d", jour.getDayOfMonth()), parJour.getOrDefault(jour, 0L));
            }
            return new Serie(new ArrayList<>(valeurs.keySet()), new ArrayList<>(valeurs.values()));
        }
        YearMonth dernier = YearMonth.from(fin);
        YearMonth premier = YearMonth.from(debut);
        if (ChronoUnit.MONTHS.between(premier, dernier) >= MOIS_MAX) {
            premier = dernier.minusMonths(MOIS_MAX - 1L);
        }
        Map<YearMonth, Long> parMois = new LinkedHashMap<>();
        for (YearMonth mois = premier; !mois.isAfter(dernier); mois = mois.plusMonths(1)) {
            parMois.put(mois, 0L);
        }
        for (Map.Entry<LocalDate, Long> entree : parJour.entrySet()) {
            parMois.computeIfPresent(YearMonth.from(entree.getKey()), (m, total) -> total + entree.getValue());
        }
        List<String> libelles = parMois.keySet().stream().map(m -> m.atDay(1).format(FORMAT_MOIS)).toList();
        return new Serie(libelles, new ArrayList<>(parMois.values()));
    }

    private static List<Long> cumuler(List<Long> valeurs) {
        List<Long> cumuls = new ArrayList<>();
        long total = 0;
        for (long valeur : valeurs) {
            total += valeur;
            cumuls.add(total);
        }
        return cumuls;
    }

    private static String capitale(String texte) {
        return texte.isEmpty() ? texte : Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }
}
