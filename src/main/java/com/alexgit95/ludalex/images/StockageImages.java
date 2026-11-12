package com.alexgit95.ludalex.images;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Stockage des images uploadées sur le volume, sous des noms générés par le serveur. */
@Service
public class StockageImages {

    private static final Logger LOG = LoggerFactory.getLogger(StockageImages.class);
    private static final Pattern NOM_VALIDE = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|gif|webp)$");

    private final Path racine;

    public StockageImages(ImagesProperties proprietes) {
        this.racine = Path.of(proprietes.repertoire()).toAbsolutePath().normalize();
    }

    public Path racine() {
        return racine;
    }

    /** Vrai pour un nom de fichier tel que l'application les génère (UUID et extension d'un format autorisé). */
    public static boolean estNomValide(String nom) {
        return nom != null && NOM_VALIDE.matcher(nom).matches();
    }

    public ImageSource enregistrer(byte[] octets) {
        FormatImage format = FormatImage.detecter(octets).orElseThrow(() -> new ImageInvalideException(
                "Format d'image non autorisé (JPEG, PNG, GIF ou WebP uniquement)"));
        String nom = UUID.randomUUID() + "." + format.extension();
        try {
            Files.createDirectories(racine);
            Files.write(racine.resolve(nom), octets, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new ImageInvalideException("Impossible d'enregistrer l'image", e);
        }
        return ImageSource.upload(nom);
    }

    /** Résout un fichier du volume ; vide pour tout nom qui n'est pas un fichier généré par l'application. */
    public Optional<Resource> charger(String nom) {
        if (nom == null || !NOM_VALIDE.matcher(nom).matches()) {
            return Optional.empty();
        }
        Path chemin = racine.resolve(nom).normalize();
        if (!chemin.startsWith(racine) || !Files.isRegularFile(chemin)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(chemin));
    }

    public void supprimer(ImageSource image) {
        if (image == null || image.getType() != TypeImage.UPLOAD || !NOM_VALIDE.matcher(image.getReference()).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(racine.resolve(image.getReference()));
        } catch (IOException e) {
            LOG.warn("Suppression impossible d'une image du volume : {}", e.getClass().getSimpleName());
        }
    }

    /** Supprime l'ancien fichier une fois la transaction validée (ou tout de suite hors transaction). */
    public void supprimerApresCommit(ImageSource image) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    supprimer(image);
                }
            });
        } else {
            supprimer(image);
        }
    }

    /** Supprime le nouveau fichier si la transaction est annulée. */
    public void supprimerSiAnnulation(ImageSource image) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int statut) {
                    if (statut != STATUS_COMMITTED) {
                        supprimer(image);
                    }
                }
            });
        }
    }
}
