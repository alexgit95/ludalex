package com.alexgit95.ludalex.images;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "ludalex.images.copies.actif", havingValue = "true", matchIfMissing = true)
public class PlanificationCopies {
    private final CopiesImages copies;
    public PlanificationCopies(CopiesImages copies) { this.copies = copies; }

    @EventListener(ApplicationReadyEvent.class)
    public void initialiser() { copies.initialiser(); }

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    public void traiter() {
        try { copies.traiterUne(); }
        catch (RuntimeException exception) { LoggerFactory.getLogger(getClass()).warn("Traitement des copies temporairement indisponible"); }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void reconcilier() {
        try { copies.reconcilier(); }
        catch (RuntimeException exception) { LoggerFactory.getLogger(getClass()).warn("Reconciliation des copies temporairement indisponible"); }
    }
}