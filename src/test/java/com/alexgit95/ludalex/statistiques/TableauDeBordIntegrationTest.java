package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.alexgit95.ludalex.images.EntreeImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuSaisie;
import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.jeux.PretService;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurService;
import com.alexgit95.ludalex.parties.PartieSaisie;
import com.alexgit95.ludalex.parties.PartieService;
import com.alexgit95.ludalex.parties.ResultatCooperatif;
import com.alexgit95.ludalex.reglages.ReglageCle;
import com.alexgit95.ludalex.reglages.ReglagesService;
import com.alexgit95.ludalex.statistiques.InstantaneStatistiques.Kpi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/** Tableau de bord sur base réelle (H2) avec une horloge figée au 13/11/2026. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tableaubord;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class TableauDeBordIntegrationTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 13);
    private static final Clock HORLOGE = Clock.fixed(AUJOURDHUI.atTime(14, 32).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    @Autowired
    private JeuService jeux;
    @Autowired
    private JoueurService joueurs;
    @Autowired
    private PartieService parties;
    @Autowired
    private PretService prets;
    @Autowired
    private ReglagesService reglages;
    @Autowired
    private StatistiquesGlobalesRepository depot;
    @Autowired
    private PlatformTransactionManager gestionnaire;
    @Autowired
    private EntityManager em;
    @Autowired
    private EntityManagerFactory fabrique;

    private StatistiquesGlobalesService service;

    private Jeu catan;
    private Jeu pandemic;
    private Jeu azul;
    private Jeu ancien;
    private Joueur a;
    private Joueur b;
    private Joueur c;

    @BeforeEach
    void preparer() {
        new TransactionTemplate(gestionnaire).executeWithoutResult(s -> {
            for (String entite : List.of("CopieImage", "TokenFinPartie", "Participation", "Equipe", "Partie", "Pret",
                    "Jeu", "Joueur", "Reglage")) {
                em.createQuery("delete from " + entite).executeUpdate();
            }
        });
        service = nouveauService();
        catan = jeu("Catan", "40.00", AUJOURDHUI.minusDays(100), false);
        pandemic = jeu("Pandemic", "30.00", LocalDate.of(2025, 12, 1), true);
        azul = jeu("Azul", null, null, false);
        ancien = jeu("Vieux jeu", "20.00", LocalDate.of(2024, 3, 1), false);
        a = joueur("Alice");
        b = joueur("Bruno");
        c = joueur("Claire");

        int[] joursCatan = { 5, 10, 40, 60, 90, 120 };
        for (int i = 0; i < joursCatan.length; i++) {
            Joueur gagnant = i < 4 ? a : b;
            parties.declarer(new PartieSaisie(catan.getId(), AUJOURDHUI.minusDays(joursCatan[i]), i < 4 ? "Salon" : "Bureau",
                    List.of(a.getId(), b.getId()), Map.of(a.getId(), 10 + i, b.getId(), 8), List.of(gagnant.getId()),
                    null, null, null, null));
        }
        int[] joursPandemic = { 3, 15, 45, 80 };
        for (int i = 0; i < joursPandemic.length; i++) {
            parties.declarer(new PartieSaisie(pandemic.getId(), AUJOURDHUI.minusDays(joursPandemic[i]), null,
                    List.of(a.getId(), b.getId(), c.getId()), null, null, null, null, null,
                    i == 3 ? ResultatCooperatif.DEFAITE : ResultatCooperatif.VICTOIRE, "Campagne"));
        }
        parties.declarer(new PartieSaisie(ancien.getId(), AUJOURDHUI.minusDays(400), "Salon",
                List.of(a.getId(), b.getId()), null, List.of(a.getId()), null, null, null, null));

        prets.preter(catan.getId(), "Voisin", AUJOURDHUI.minusDays(50));
        prets.preter(pandemic.getId(), "Cousine", AUJOURDHUI.minusDays(20));
        prets.retourner(pandemic.getId(), AUJOURDHUI.minusDays(10));
    }

    private StatistiquesGlobalesService nouveauService() {
        return new StatistiquesGlobalesService(depot, reglages,
                new CacheStatistiques(HORLOGE, Duration.ofMinutes(10)), HORLOGE);
    }

    private Jeu jeu(String nom, String prix, LocalDate achat, boolean cooperatif) {
        return jeux.creer(new JeuSaisie(nom, prix == null ? null : new BigDecimal(prix), achat, cooperatif, null,
                EntreeImage.inchangee(), cooperatif));
    }

    private Joueur joueur(String nom) {
        return joueurs.creer(nom, EntreeImage.inchangee(), EntreeImage.inchangee());
    }

    private static Kpi kpi(InstantaneStatistiques stats, String etiquette) {
        return stats.kpis().stream().filter(k -> k.etiquette().equals(etiquette)).findFirst().orElseThrow();
    }

    @Test
    void chiffresClesSurDouzeMois() {
        InstantaneStatistiques stats = service.instantane(PeriodeStatistiques.MOIS_12);

        assertThat(stats.calculeLe()).isEqualTo(AUJOURDHUI.atTime(14, 32));
        assertThat(kpi(stats, "Parties").valeur()).isEqualTo("10");
        assertThat(kpi(stats, "Jeux joués").valeur()).isEqualTo("2 / 4");
        assertThat(kpi(stats, "Joueurs actifs").valeur()).isEqualTo("3");
        assertThat(kpi(stats, "Ludothèque").valeur()).isEqualTo("4");
        assertThat(kpi(stats, "Ludothèque").detail()).isEqualTo("dont 1 coopératif");
        assertThat(kpi(stats, "Valeur").valeur()).contains("90,00");
        assertThat(kpi(stats, "Valeur").detail()).contains("prix moyen").contains("30,00").contains("1 jeu sans prix");
        assertThat(kpi(stats, "Prêts").valeur()).isEqualTo("1 en cours");
        assertThat(kpi(stats, "Prêts").detail()).isEqualTo("2 sur la période");
        assertThat(kpi(stats, "Série").valeur()).isEqualTo("3 semaines");
    }

    @Test
    void evolutionSurTrenteJours() {
        InstantaneStatistiques stats = service.instantane(PeriodeStatistiques.JOURS_30);

        assertThat(kpi(stats, "Parties").valeur()).isEqualTo("4");
        assertThat(kpi(stats, "Parties").detail()).startsWith("+100 %");
        assertThat(stats.activite().titreChronologie()).isEqualTo("Parties par jour");
        assertThat(stats.activite().chronologie()).hasSize(30);
    }

    @Test
    void toutLHistoriqueInclutLesPartiesAnciennes() {
        InstantaneStatistiques stats = service.instantane(PeriodeStatistiques.TOUT);

        assertThat(kpi(stats, "Parties").valeur()).isEqualTo("11");
        assertThat(kpi(stats, "Parties").detail()).isEqualTo("tout l'historique");
        assertThat(stats.jeux().dormants()).isEqualTo(1);
    }

    @Test
    void activiteParMoisJourDeSemaineEtLieu() {
        InstantaneStatistiques.Activite activite = service.instantane(PeriodeStatistiques.MOIS_12).activite();

        assertThat(activite.parties()).isEqualTo(10);
        assertThat(activite.titreChronologie()).isEqualTo("Parties par mois");
        assertThat(activite.chronologie()).hasSize(13);
        assertThat(activite.chronologie().stream().mapToLong(InstantaneStatistiques.Barre::valeur).sum()).isEqualTo(10);
        assertThat(activite.parJourSemaine()).hasSize(7);
        assertThat(activite.parJourSemaine().stream().mapToLong(InstantaneStatistiques.Barre::valeur).sum()).isEqualTo(10);
        assertThat(activite.calendrier().total()).isEqualTo(10);
        assertThat(activite.cumul().total()).isEqualTo(10);
        assertThat(activite.lieux().tranches()).extracting(InstantaneStatistiques.Tranche::libelle)
                .contains("Non renseigné", "Salon", "Bureau");
        assertThat(activite.lieux().total()).isEqualTo(10);
    }

    @Test
    void statistiquesDesJeux() {
        InstantaneStatistiques.Jeux stats = service.instantane(PeriodeStatistiques.MOIS_12).jeux();

        assertThat(stats.plusJoues()).extracting(InstantaneStatistiques.Barre::libelle).containsExactly("Catan",
                "Pandemic");
        assertThat(stats.plusJoues()).extracting(InstantaneStatistiques.Barre::valeur).containsExactly(6L, 4L);
        assertThat(stats.plusGagnes()).extracting(InstantaneStatistiques.Barre::libelle).containsExactly("Pandemic");
        assertThat(stats.plusGagnes().get(0).valeur()).isEqualTo(3);
        assertThat(stats.jamaisJoues()).isEqualTo(1);
        assertThat(stats.jamaisJouesListe()).containsExactly("Azul");
        assertThat(stats.dormants()).isEqualTo(1);
        assertThat(stats.dormantsListe()).extracting(InstantaneStatistiques.Ligne::libelle).containsExactly("Vieux jeu");
        assertThat(stats.modes().total()).isEqualTo(10);
        assertThat(stats.tauxCooperatif()).isEqualTo(75);
        assertThat(stats.jeuDifficile()).isNull();
        assertThat(stats.variantes()).extracting(InstantaneStatistiques.Barre::libelle).containsExactly("Campagne");
        assertThat(stats.coutParPartie()).extracting(InstantaneStatistiques.Ligne::libelle)
                .containsExactly("Catan", "Pandemic", "Vieux jeu");
    }

    @Test
    void leJeuCooperatifLePlusDifficileDependDuMinimumDeParties() {
        reglages.enregistrer(Map.of(ReglageCle.CLASSEMENT_PARTIES_MIN, "3", ReglageCle.JEU_DORMANT_MOIS, "6",
                ReglageCle.PRET_LONG_JOURS, "30"));

        InstantaneStatistiques.Jeux stats = nouveauService().instantane(PeriodeStatistiques.MOIS_12).jeux();

        assertThat(stats.jeuDifficile().libelle()).isEqualTo("Pandemic");
        assertThat(stats.jeuDifficile().valeur()).isEqualTo("75 %");
    }

    @Test
    void leSeuilDeJeuDormantEstModifiable() {
        reglages.enregistrer(Map.of(ReglageCle.JEU_DORMANT_MOIS, "120", ReglageCle.CLASSEMENT_PARTIES_MIN, "5",
                ReglageCle.PRET_LONG_JOURS, "30"));

        InstantaneStatistiques.Jeux stats = nouveauService().instantane(PeriodeStatistiques.MOIS_12).jeux();

        assertThat(stats.moisDormant()).isEqualTo(120);
        assertThat(stats.dormants()).isZero();
    }

    @Test
    void classementDesJoueursAvecMinimumDePartiesEtDuos() {
        InstantaneStatistiques.Joueurs stats = service.instantane(PeriodeStatistiques.MOIS_12).joueurs();

        assertThat(stats.classement()).extracting(InstantaneStatistiques.Ligne::libelle).containsExactly("Alice", "Bruno");
        assertThat(stats.classement().get(0).valeur()).isEqualTo("70 %");
        assertThat(stats.classement().get(1).valeur()).isEqualTo("50 %");
        assertThat(stats.minimumParties()).isEqualTo(5);
        assertThat(stats.partiesParJoueur()).extracting(InstantaneStatistiques.Barre::libelle)
                .containsExactly("Alice", "Bruno", "Claire");
        assertThat(stats.polyvalence().get(0).valeur()).isEqualTo(2);
        assertThat(stats.duos().get(0).libelle()).isEqualTo("Alice & Bruno");
        assertThat(stats.duos().get(0).valeur()).isEqualTo("10 parties");
        assertThat(stats.records().get(0).libelle()).isEqualTo("Catan");
        assertThat(stats.records().get(0).valeur()).isEqualTo("max 15");
    }

    @Test
    void unJoueurAnonymiseResteDansLesTotauxMaisPasDansLesClassements() {
        joueurs.supprimer(a.getId());

        InstantaneStatistiques stats = nouveauService().instantane(PeriodeStatistiques.MOIS_12);

        assertThat(kpi(stats, "Parties").valeur()).isEqualTo("10");
        assertThat(stats.joueurs().classement()).extracting(InstantaneStatistiques.Ligne::libelle)
                .containsExactly("Bruno");
        assertThat(stats.joueurs().partiesParJoueur()).extracting(InstantaneStatistiques.Barre::libelle)
                .doesNotContain(Joueur.NOM_ANONYME);
        assertThat(stats.joueurs().duos()).extracting(InstantaneStatistiques.Ligne::libelle)
                .containsExactly("Bruno & Claire");
    }

    @Test
    void ludothequeEtPrets() {
        InstantaneStatistiques stats = service.instantane(PeriodeStatistiques.TOUT);

        assertThat(stats.ludotheque().sansPrix()).isEqualTo(1);
        assertThat(stats.ludotheque().sansDate()).isEqualTo(1);
        assertThat(stats.ludotheque().plusChers()).extracting(InstantaneStatistiques.Ligne::libelle)
                .containsExactly("Catan", "Pandemic", "Vieux jeu");
        assertThat(stats.ludotheque().achatsParAnnee()).extracting(InstantaneStatistiques.Barre::libelle)
                .containsExactly("2024", "2025", "2026");
        assertThat(stats.ludotheque().investissement().total()).isEqualTo(90);

        assertThat(stats.prets().enCours()).isEqualTo(1);
        assertThat(stats.prets().dureeMoyenne()).isEqualTo("10 jours");
        assertThat(stats.prets().enCoursListe()).singleElement().satisfies(p -> {
            assertThat(p.jeu()).isEqualTo("Catan");
            assertThat(p.jours()).isEqualTo(50);
            assertThat(p.longue()).isTrue();
        });
        assertThat(stats.prets().emprunteurs()).extracting(InstantaneStatistiques.Barre::libelle)
                .containsExactlyInAnyOrder("Voisin", "Cousine");
    }

    @Test
    void leSeuilDePretLongEstModifiable() {
        reglages.enregistrer(Map.of(ReglageCle.PRET_LONG_JOURS, "60", ReglageCle.CLASSEMENT_PARTIES_MIN, "5",
                ReglageCle.JEU_DORMANT_MOIS, "6"));

        InstantaneStatistiques stats = nouveauService().instantane(PeriodeStatistiques.TOUT);

        assertThat(stats.prets().enCoursListe().get(0).longue()).isFalse();
        assertThat(stats.prets().seuilJours()).isEqualTo(60);
    }

    @Test
    void lesPartiesNonTermineesSontIgnorees() {
        parties.declarer(new PartieSaisie(catan.getId(), AUJOURDHUI.minusDays(1), null,
                List.of(a.getId(), b.getId()), null, List.of(a.getId()), null, null, null, null));
        new TransactionTemplate(gestionnaire).executeWithoutResult(s -> em
                .createQuery("update Partie p set p.statut = com.alexgit95.ludalex.parties.StatutPartie.EN_COURS "
                        + "where p.datePartie = :jour").setParameter("jour", AUJOURDHUI.minusDays(1)).executeUpdate());

        InstantaneStatistiques stats = nouveauService().instantane(PeriodeStatistiques.MOIS_12);

        assertThat(kpi(stats, "Parties").valeur()).isEqualTo("10");
    }

    @Test
    void lePremierAccesCalculeLesSuivantsUtilisentLeCache() {
        Statistics suivi = fabrique.unwrap(SessionFactory.class).getStatistics();
        suivi.setStatisticsEnabled(true);
        try {
            suivi.clear();
            InstantaneStatistiques premier = service.instantane(PeriodeStatistiques.MOIS_12);
            long requetes = suivi.getPrepareStatementCount();
            InstantaneStatistiques second = service.instantane(PeriodeStatistiques.MOIS_12);

            assertThat(second).isSameAs(premier);
            assertThat(suivi.getPrepareStatementCount()).isEqualTo(requetes);
        } finally {
            suivi.setStatisticsEnabled(false);
        }
    }

    @Test
    void leNombreDeRequetesNeDependPasDuVolume() {
        Statistics suivi = fabrique.unwrap(SessionFactory.class).getStatistics();
        suivi.setStatisticsEnabled(true);
        try {
            suivi.clear();
            nouveauService().calculer(PeriodeStatistiques.TOUT);
            long petit = suivi.getPrepareStatementCount();

            for (int i = 0; i < 40; i++) {
                Jeu jeu = jeu("Extra " + i, "10.00", AUJOURDHUI.minusDays(i), false);
                parties.declarer(new PartieSaisie(jeu.getId(), AUJOURDHUI.minusDays(i), "Lieu " + i,
                        List.of(a.getId(), b.getId()), Map.of(a.getId(), i, b.getId(), 3), List.of(a.getId()), null,
                        null, null, null));
                prets.preter(jeu.getId(), "Emprunteur " + i, AUJOURDHUI.minusDays(i));
            }
            suivi.clear();
            nouveauService().calculer(PeriodeStatistiques.TOUT);
            long grand = suivi.getPrepareStatementCount();

            assertThat(grand).isEqualTo(petit);
        } finally {
            suivi.setStatisticsEnabled(false);
        }
    }

    @Test
    void uneBaseVideDonneDesBlocsVides() {
        new TransactionTemplate(gestionnaire).executeWithoutResult(s -> {
            for (String entite : List.of("Participation", "Equipe", "Partie", "Pret", "Jeu", "Joueur")) {
                em.createQuery("delete from " + entite).executeUpdate();
            }
        });

        InstantaneStatistiques stats = nouveauService().instantane(PeriodeStatistiques.TOUT);

        assertThat(kpi(stats, "Parties").valeur()).isEqualTo("0");
        assertThat(kpi(stats, "Valeur").detail()).contains("prix moyen indisponible");
        assertThat(stats.activite().vide()).isTrue();
        assertThat(stats.jeux().tauxCooperatif()).isNull();
        assertThat(stats.jeux().modes().vide()).isTrue();
        assertThat(stats.joueurs().classement()).isEmpty();
        assertThat(stats.prets().dureeMoyenne()).isNull();
        assertThat(stats.ludotheque().investissement().vide()).isTrue();
    }
}
