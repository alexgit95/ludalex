package com.alexgit95.ludalex.statistiques;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class CacheStatistiquesTest {

    private static final class HorlogeMutable extends Clock {
        private Instant instant = Instant.parse("2026-11-13T10:00:00Z");

        void avancer(Duration duree) {
            instant = instant.plus(duree);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private final HorlogeMutable horloge = new HorlogeMutable();
    private final CacheStatistiques cache = new CacheStatistiques(horloge, Duration.ofMinutes(10));
    private final AtomicInteger calculs = new AtomicInteger();

    private int calculer() {
        return calculs.incrementAndGet();
    }

    @Test
    void sertLeCacheValideSansNouveauCalcul() {
        int premier = cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer);
        horloge.avancer(Duration.ofMinutes(9).plusSeconds(59));
        int second = cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer);

        assertThat(premier).isEqualTo(1);
        assertThat(second).isEqualTo(1);
        assertThat(calculs).hasValue(1);
    }

    @Test
    void recalculeApresLesDixMinutes() {
        cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer);
        horloge.avancer(Duration.ofMinutes(10));

        assertThat((int) cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer)).isEqualTo(2);
        assertThat((int) cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer)).isEqualTo(2);
    }

    @Test
    void conserveUneEntreeParPeriode() {
        cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer);
        cache.obtenir(PeriodeStatistiques.ANNEE, this::calculer);
        cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer);
        cache.obtenir(PeriodeStatistiques.ANNEE, this::calculer);

        assertThat(calculs).hasValue(2);
    }

    @Test
    void invaliderVideToutesLesPeriodes() {
        cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer);
        cache.obtenir(PeriodeStatistiques.TOUT, this::calculer);

        cache.invalider();

        assertThat((int) cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer)).isEqualTo(3);
        assertThat((int) cache.obtenir(PeriodeStatistiques.TOUT, this::calculer)).isEqualTo(4);
    }

    @Test
    void neConservePasUnResultatCalculePendantUneInvalidation() {
        int resultat = cache.obtenir(PeriodeStatistiques.MOIS_12, () -> {
            cache.invalider();
            return calculer();
        });

        assertThat(resultat).isEqualTo(1);
        assertThat((int) cache.obtenir(PeriodeStatistiques.MOIS_12, this::calculer)).isEqualTo(2);
    }

    @Test
    void unSeulCalculPourDesAccesConcurrents() throws Exception {
        CountDownLatch depart = new CountDownLatch(1);
        try (ExecutorService execution = Executors.newFixedThreadPool(6)) {
            java.util.List<Future<Integer>> resultats = new java.util.ArrayList<>();
            for (int i = 0; i < 6; i++) {
                resultats.add(execution.submit(() -> {
                    depart.await();
                    return cache.<Integer>obtenir(PeriodeStatistiques.MOIS_12, () -> {
                        try {
                            Thread.sleep(150);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return calculer();
                    });
                }));
            }
            depart.countDown();
            for (Future<Integer> resultat : resultats) {
                assertThat(resultat.get(5, TimeUnit.SECONDS)).isEqualTo(1);
            }
        }

        assertThat(calculs).hasValue(1);
    }
}
