package com.alexgit95.ludalex.affichage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieRepository;
import com.alexgit95.ludalex.parties.StatutPartie;
import com.alexgit95.ludalex.statistiques.StatistiqueRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:affichagepostgres;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=SET TIME ZONE 'UTC'",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"
})
class HistoriquePostgresTest {

    @Autowired private EntityManager em;
    @Autowired private PartieRepository parties;
    @Autowired private StatistiqueRepository statistiques;

    @Test
    @Transactional
    void interrogeLesPartiesEtLesRangsAvecLeDialectePostgres() {
        Jeu jeu = new Jeu("Jeu PostgreSQL");
        Joueur gagnant = new Joueur("Gagnant PostgreSQL");
        Joueur second = new Joueur("Second PostgreSQL");
        em.persist(jeu);
        em.persist(gagnant);
        em.persist(second);
        Partie partie = new Partie(jeu, LocalDate.of(2026, 1, 1));
        em.persist(partie);
        Participation victoire = new Participation(partie, gagnant);
        victoire.setGagnant(true);
        em.persist(victoire);
        em.persist(new Participation(partie, second));
        em.flush();

        List<Long> scores = statistiques.scoresPodium(jeu.getId(), PageRequest.of(0, 3));

        assertThat(scores).containsExactly(1L, 0L);
        assertThat(statistiques.joueursPodium(jeu.getId(), 1L, PageRequest.of(0, 3)))
                .extracting(Joueur::getId).containsExactly(gagnant.getId());
        assertThat(statistiques.nombreJoueursPodium(jeu.getId(), 0L)).isEqualTo(1);
        assertThat(parties.findByStatutOrderByDatePartieDescIdDesc(StatutPartie.TERMINEE, PageRequest.of(0, 5)))
                .extracting(Partie::getId).contains(partie.getId());
    }
}