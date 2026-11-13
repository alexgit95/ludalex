package com.alexgit95.ludalex.donnees;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * Écouteur JPA des entités métier : tout chemin d'écriture (parties, jeux, prêts, joueurs, réglages, import,
 * restauration) signale le changement sans que chaque service ait à s'en charger. Le signal n'est publié qu'après
 * le commit, une seule fois par transaction.
 */
@Component
public class ChangementDonnees {

    private static final Object CLE = new Object();

    private final ApplicationEventPublisher publieur;

    public ChangementDonnees(ApplicationEventPublisher publieur) {
        this.publieur = publieur;
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    void apresEcriture(Object entite) {
        signaler(publieur);
    }

    /** Publie {@link DonneesModifiees} après le commit en cours, ou immédiatement hors transaction. */
    public static void signaler(ApplicationEventPublisher publieur) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publieur.publishEvent(new DonneesModifiees());
            return;
        }
        if (TransactionSynchronizationManager.hasResource(CLE)) {
            return;
        }
        TransactionSynchronizationManager.bindResource(CLE, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publieur.publishEvent(new DonneesModifiees());
            }

            @Override
            public void afterCompletion(int statut) {
                TransactionSynchronizationManager.unbindResourceIfPossible(CLE);
            }
        });
    }
}
