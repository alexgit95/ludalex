package com.alexgit95.ludalex.jeux;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alexgit95.ludalex.images.EntreeImage;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrence;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class PretConcurrenceTest {

    @Autowired
    private JeuService jeux;

    @Autowired
    private PretService prets;

    @Test
    void unSeulPretEnCoursMemeAvecDeuxDemandesSimultanees() throws Exception {
        ExecutorService executeur = Executors.newFixedThreadPool(2);
        try {
            for (int tour = 0; tour < 5; tour++) {
                Long jeuId = jeux.creer(new JeuSaisie("Jeu-" + UUID.randomUUID(), null, null, false, null,
                        EntreeImage.inchangee())).getId();
                CountDownLatch depart = new CountDownLatch(1);
                List<Future<Boolean>> resultats = new ArrayList<>();
                for (String emprunteur : new String[] { "Marie", "Paul" }) {
                    resultats.add(executeur.submit(() -> {
                        depart.await();
                        try {
                            prets.preter(jeuId, emprunteur, null);
                            return true;
                        } catch (PretInvalideException e) {
                            return false;
                        }
                    }));
                }
                depart.countDown();

                long acceptes = 0;
                for (Future<Boolean> resultat : resultats) {
                    if (resultat.get()) {
                        acceptes++;
                    }
                }

                assertThat(acceptes).isEqualTo(1);
                assertThat(jeux.fiche(jeuId).historique()).hasSize(1);
            }
        } finally {
            executeur.shutdownNow();
        }
    }
}
