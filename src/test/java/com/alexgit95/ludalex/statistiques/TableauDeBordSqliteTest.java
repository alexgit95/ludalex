package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.Pret;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.ResultatCooperatif;
import com.alexgit95.ludalex.reglages.ReglagesService;

import jakarta.persistence.EntityManager;

/** Les requêtes agrégées doivent aussi s'exécuter sur SQLite (dates stockées en texte, agrégats, jointures). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect"
})
class TableauDeBordSqliteTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 13);
    private static final Clock HORLOGE = Clock.fixed(AUJOURDHUI.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Autowired
    private EntityManager em;
    @Autowired
    private StatistiquesGlobalesRepository depot;
    @Autowired
    private ReglagesService reglages;

    @Test
    @Transactional
    void calculeToutesLesPeriodesSurSqlite() {
        Jeu catan = new Jeu("Catan");
        catan.setPrix(new BigDecimal("40.00"));
        catan.setDateAchat(AUJOURDHUI.minusDays(30));
        Jeu pandemic = new Jeu("Pandemic");
        pandemic.setCooperatif(true);
        pandemic.setPrix(new BigDecimal("30.00"));
        Jeu inconnu = new Jeu("Jamais joue");
        em.persist(catan);
        em.persist(pandemic);
        em.persist(inconnu);
        Joueur alice = new Joueur("Alice");
        Joueur bruno = new Joueur("Bruno");
        em.persist(alice);
        em.persist(bruno);

        Partie p1 = new Partie(catan, AUJOURDHUI.minusDays(2));
        p1.setLieu("Salon");
        em.persist(p1);
        Participation a1 = new Participation(p1, alice);
        a1.setGagnant(true);
        a1.setScore(12);
        em.persist(a1);
        em.persist(new Participation(p1, bruno));
        Partie p2 = new Partie(pandemic, AUJOURDHUI.minusDays(40));
        p2.setResultatCooperatif(ResultatCooperatif.VICTOIRE);
        p2.setVariante("Campagne");
        em.persist(p2);
        em.persist(new Participation(p2, alice));
        em.persist(new Participation(p2, bruno));
        Pret pret = new Pret(catan, "Voisin", AUJOURDHUI.minusDays(10));
        em.persist(pret);
        Pret rendu = new Pret(pandemic, "Cousine", AUJOURDHUI.minusDays(20));
        rendu.clore(AUJOURDHUI.minusDays(15));
        em.persist(rendu);
        em.flush();
        em.clear();

        for (PeriodeStatistiques periode : PeriodeStatistiques.values()) {
            StatistiquesGlobalesService service = new StatistiquesGlobalesService(depot, reglages,
                    new CacheStatistiques(HORLOGE, Duration.ofMinutes(10)), HORLOGE);
            InstantaneStatistiques stats = service.calculer(periode);
            assertThat(stats.kpis()).hasSize(7);
        }

        StatistiquesGlobalesService service = new StatistiquesGlobalesService(depot, reglages,
                new CacheStatistiques(HORLOGE, Duration.ofMinutes(10)), HORLOGE);
        InstantaneStatistiques stats = service.calculer(PeriodeStatistiques.TOUT);
        assertThat(stats.kpis().get(0).valeur()).isEqualTo("2");
        assertThat(stats.jeux().jamaisJoues()).isEqualTo(1);
        assertThat(stats.jeux().plusJoues()).hasSize(2);
        assertThat(stats.jeux().tauxCooperatif()).isEqualTo(100);
        assertThat(stats.jeux().variantes()).hasSize(1);
        assertThat(stats.joueurs().duos().get(0).valeur()).isEqualTo("2 parties");
        assertThat(stats.joueurs().records().get(0).valeur()).isEqualTo("max 12");
        assertThat(stats.prets().dureeMoyenne()).isEqualTo("5 jours");
        assertThat(stats.prets().enCours()).isEqualTo(1);
        assertThat(stats.ludotheque().investissement().total()).isEqualTo(40);
        assertThat(stats.activite().calendrier().total()).isEqualTo(2);
    }
}
