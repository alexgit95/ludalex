package com.alexgit95.ludalex.images;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class CopiesImages {
    private final CopieImageRepository copies;
    private final ReferencesImages references;
    private final StockageImages stockage;
    private final TelechargeurImage telechargeur;
    private final CoordinationImages coordination;
    private final Clock horloge;
    private final TransactionTemplate transaction;
    private final AtomicBoolean occupe = new AtomicBoolean();
    private final AtomicBoolean aReconcilier = new AtomicBoolean();

    public CopiesImages(CopieImageRepository copies, ReferencesImages references, StockageImages stockage,
            TelechargeurImage telechargeur, CoordinationImages coordination, Clock horloge,
            PlatformTransactionManager transactions) {
        this.copies = copies;
        this.references = references;
        this.stockage = stockage;
        this.telechargeur = telechargeur;
        this.coordination = coordination;
        this.horloge = horloge;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public record Referencee(String url) { }

    @TransactionalEventListener(fallbackExecution = true)
    public void referencee(Referencee evenement) {
        aReconcilier.set(true);
    }

    private void inscrire(String url) {
        CopieImage copie = copies.findById(CopieImage.cle(url)).orElse(null);
        if (copie == null) {
            copies.save(new CopieImage(url));
        } else if (!copie.getUrl().equals(url)) {
            throw new IllegalStateException("Collision d'empreinte d'image");
        }
    }

    public void initialiser() {
        coordination.verrou().lock();
        try {
            stockage.nettoyerTemporairesCopies();
            parcourir(copie -> {
                if (copie.getEtat() == CopieImage.Etat.EN_COURS) { copie.reprendre(); copies.save(copie); }
            });
        } finally { coordination.verrou().unlock(); }
        reconcilier();
    }

    public void reconcilier() {
        coordination.verrou().lock();
        try {
            references.parcourir(url -> transaction.executeWithoutResult(statut -> inscrire(url)));
            parcourir(copie -> {
                if (!references.existe(copie.getUrl(), TypeImage.URL)) {
                    copies.delete(copie);
                    supprimerFichier(copie);
                } else if (copie.getEtat() == CopieImage.Etat.DISPONIBLE
                        && stockage.charger(copie.getFichier()).isEmpty()) {
                    copie.relancer();
                    copies.save(copie);
                }
            });
            nettoyerOrphelins();
        } finally { coordination.verrou().unlock(); }
    }

    public void apresRestauration() {
        parcourir(copie -> {
            if (copie.getEtat() == CopieImage.Etat.EN_COURS) { copie.reprendre(); copies.save(copie); }
            if (copie.getEtat() == CopieImage.Etat.DISPONIBLE && copie.getTaille() == 0) {
                try {
                    copie.disponible(copie.getFichier(), Files.size(stockage.racine().resolve(copie.getFichier())));
                    copies.save(copie);
                } catch (IOException exception) { copie.relancer(); copies.save(copie); }
            }
        });
    }

    private void parcourir(java.util.function.Consumer<CopieImage> action) {
        String dernier = "";
        while (true) {
            List<CopieImage> lot = copies.findByCleGreaterThanOrderByCle(dernier, PageRequest.of(0, 100));
            for (CopieImage copie : lot) { dernier = copie.getCle(); action.accept(copie); }
            if (lot.size() < 100) { break; }
        }
    }

    private void nettoyerOrphelins() {
        if (!Files.isDirectory(stockage.racine())) { return; }
        try (var fichiers = Files.list(stockage.racine())) {
            fichiers.filter(Files::isRegularFile).filter(chemin -> StockageImages.estNomCopie(chemin.getFileName().toString()))
                    .forEach(chemin -> {
                        String nom = chemin.getFileName().toString();
                        if (!copies.existsByFichier(nom) && !references.existe(nom, TypeImage.UPLOAD)) {
                            stockage.supprimer(ImageSource.upload(nom));
                        }
                    });
        } catch (IOException exception) {
            LoggerFactory.getLogger(getClass()).warn("Nettoyage des copies orphelines impossible");
        }
    }

    public void traiterUne() {
        if (!occupe.compareAndSet(false, true)) { return; }
        Path temporaire = null;
        ImageSource publiee = null;
        try {
            if (aReconcilier.getAndSet(false)) { reconcilier(); }
            Travail travail;
            coordination.verrou().lock();
            try {
                travail = transaction.execute(statut -> {
                    List<CopieImage> lot = copies.eligibles(CopieImage.Etat.EN_ATTENTE, horloge.instant(), PageRequest.of(0, 1));
                    if (lot.isEmpty()) { return null; }
                    CopieImage copie = lot.getFirst();
                    copie.commencer(horloge.instant());
                    return new Travail(copie.getCle(), copie.getUrl(), coordination.generation());
                });
            } finally { coordination.verrou().unlock(); }
            if (travail == null) { return; }
            try {
                temporaire = stockage.temporaireCopie();
                telechargeur.telecharger(travail.url(), temporaire);
                coordination.verrou().lock();
                try {
                    if (travail.generation() != coordination.generation()) { return; }
                    if (!references.existe(travail.url(), TypeImage.URL)) {
                        copies.deleteById(travail.cle());
                        return;
                    }
                    publiee = stockage.publierCopie(temporaire);
                    long taille = Files.size(stockage.racine().resolve(publiee.getReference()));
                    String nom = publiee.getReference();
                    transaction.executeWithoutResult(statut -> copies.findById(travail.cle()).ifPresent(copie ->
                            copie.disponible(nom, taille)));
                    publiee = null;
                } finally { coordination.verrou().unlock(); }
            } catch (IOException | RuntimeException exception) {
                coordination.verrou().lock();
                try {
                    if (travail.generation() == coordination.generation()) {
                        transaction.executeWithoutResult(statut -> copies.findById(travail.cle()).ifPresent(copie ->
                                copie.echouer(horloge.instant(), exception instanceof CopieRefuseeException,
                                        exception instanceof CopieRefuseeException ? exception.getMessage() : "Image inaccessible ou stockage indisponible")));
                    }
                } finally { coordination.verrou().unlock(); }
            }
        } finally {
            if (temporaire != null) { stockage.supprimerTemporaire(temporaire); }
            if (publiee != null) { stockage.supprimer(publiee); }
            occupe.set(false);
        }
    }

    private void supprimerFichier(CopieImage copie) {
        if (copie.getFichier() != null) { stockage.supprimer(ImageSource.upload(copie.getFichier())); }
    }

    public void relancer(String cle) {
        coordination.verrou().lock();
        try {
            transaction.executeWithoutResult(statut -> copies.findById(cle).filter(copie -> copie.getEtat() == CopieImage.Etat.ECHEC)
                    .ifPresent(CopieImage::relancer));
        } finally { coordination.verrou().unlock(); }
    }

    public void relancerEchecs() {
        coordination.verrou().lock();
        try { parcourir(copie -> { if (copie.getEtat() == CopieImage.Etat.ECHEC) { relancer(copie.getCle()); } }); }
        finally { coordination.verrou().unlock(); }
    }

    public Map<String, String> disponibles(List<String> urls) {
        Map<String, String> resultat = new LinkedHashMap<>();
        if (urls.isEmpty()) { return resultat; }
        for (CopieImage copie : copies.findAllById(urls.stream().map(CopieImage::cle).distinct().toList())) {
            if (copie.getEtat() == CopieImage.Etat.DISPONIBLE && urls.contains(copie.getUrl())) {
                resultat.put(copie.getUrl(), "/images/" + copie.getFichier());
            }
        }
        return resultat;
    }

    private record Travail(String cle, String url, long generation) { }
}