package com.alexgit95.ludalex.statistiques;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.alexgit95.ludalex.donnees.DonneesModifiees;

/**
 * Un instantané par période, valable {@code ttl} (10 minutes) et vidé à toute modification des données. Un verrou
 * par période garantit un seul calcul à la fois ; un résultat calculé pendant une invalidation n'est pas conservé.
 */
@Component
public class CacheStatistiques {

    private record Entree(Object valeur, Instant calculeLe) {
    }

    private final Clock horloge;
    private final Duration ttl;
    private final Map<PeriodeStatistiques, Entree> entrees = new ConcurrentHashMap<>();
    private final Map<PeriodeStatistiques, ReentrantLock> verrous = new EnumMap<>(PeriodeStatistiques.class);
    private final AtomicLong generation = new AtomicLong();

    public CacheStatistiques(Clock horloge, @Value("${ludalex.statistiques.cache-ttl:10m}") Duration ttl) {
        this.horloge = horloge;
        this.ttl = ttl;
        for (PeriodeStatistiques periode : PeriodeStatistiques.values()) {
            verrous.put(periode, new ReentrantLock());
        }
    }

    /** Valeur en cache si elle est encore valable, sinon {@code calcul} est exécuté (une seule fois en concurrence). */
    @SuppressWarnings("unchecked")
    public <T> T obtenir(PeriodeStatistiques periode, Supplier<T> calcul) {
        T valeur = (T) valide(periode);
        if (valeur != null) {
            return valeur;
        }
        ReentrantLock verrou = verrous.get(periode);
        verrou.lock();
        try {
            valeur = (T) valide(periode);
            if (valeur != null) {
                return valeur;
            }
            long avant = generation.get();
            T calcule = calcul.get();
            if (generation.get() == avant) {
                entrees.put(periode, new Entree(calcule, Instant.now(horloge)));
            }
            return calcule;
        } finally {
            verrou.unlock();
        }
    }

    private Object valide(PeriodeStatistiques periode) {
        Entree entree = entrees.get(periode);
        if (entree == null) {
            return null;
        }
        if (Instant.now(horloge).isBefore(entree.calculeLe().plus(ttl))) {
            return entree.valeur();
        }
        entrees.remove(periode, entree);
        return null;
    }

    @EventListener(DonneesModifiees.class)
    public void invalider() {
        generation.incrementAndGet();
        entrees.clear();
    }
}
