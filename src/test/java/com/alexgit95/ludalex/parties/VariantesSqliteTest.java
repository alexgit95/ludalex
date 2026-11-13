package com.alexgit95.ludalex.parties;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.images.CopieImage;
import java.time.Instant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect"
})
class VariantesSqliteTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void persisteLesVariantesAvecSQLite() {
        Jeu jeu = new Jeu("Arkham Horror");
        assertThat(jeu.isPlusieursVariantes()).isFalse();
        jeu.setPlusieursVariantes(true);
        entityManager.persist(jeu);

        Partie partie = new Partie(jeu, LocalDate.of(2026, 11, 13));
        partie.setVariante("Le cycle de Dunwich");
        entityManager.persist(partie);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Jeu.class, jeu.getId()).isPlusieursVariantes()).isTrue();
        assertThat(entityManager.find(Partie.class, partie.getId()).getVariante()).isEqualTo("Le cycle de Dunwich");
    }

    @Test
    @Transactional
    void persisteUneCopieAvecSesEcheancesSQLite() {
        CopieImage copie = new CopieImage("https://example.org/sqlite.png");
        Instant maintenant = Instant.parse("2026-11-13T10:00:00Z");
        copie.commencer(maintenant);
        copie.echouer(maintenant, false, "Indisponible");
        entityManager.persist(copie);
        entityManager.flush();
        entityManager.clear();
        CopieImage relue = entityManager.find(CopieImage.class, copie.getCle());
        assertThat(relue.getEtat()).isEqualTo(CopieImage.Etat.EN_ATTENTE);
        assertThat(relue.getDerniereTentative()).isEqualTo(maintenant);
        assertThat(relue.getProchaineTentative()).isEqualTo(maintenant.plusSeconds(60));
    }
}