package com.alexgit95.ludalex.sauvegarde;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.alexgit95.ludalex.images.StockageImages;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.Manifeste;

/**
 * Restauration complète : réception en fichier temporaire, validation intégrale, puis remplacement des données
 * (une transaction) et des images. Les images sont préparées sur le volume avant le commit et installées après :
 * un échec avant le commit laisse données et images intactes.
 */
@Service
public class RestaurationSauvegarde {

    static final String PREFIXE_PREPARATION = ".restauration-";

    private static final Logger LOG = LoggerFactory.getLogger(RestaurationSauvegarde.class);
    private static final long MARGE_DISQUE = 10L * 1024 * 1024;
    private static final int TAMPON = 64 * 1024;

    private final SauvegardeProperties proprietes;
    private final ValidateurSauvegarde validateur;
    private final ImportDonnees importDonnees;
    private final StockageImages stockage;
    private final ReentrantLock verrou = new ReentrantLock();

    public RestaurationSauvegarde(SauvegardeProperties proprietes, ValidateurSauvegarde validateur,
            ImportDonnees importDonnees, StockageImages stockage) {
        this.proprietes = proprietes;
        this.validateur = validateur;
        this.importDonnees = importDonnees;
        this.stockage = stockage;
    }

    public long tailleMax() {
        return proprietes.tailleMax().toBytes();
    }

    /** @throws RestaurationEnCoursException si une autre restauration est en cours */
    public Manifeste restaurer(InputStream flux) {
        if (!verrou.tryLock()) {
            throw new RestaurationEnCoursException();
        }
        Path temporaire = null;
        try {
            temporaire = recevoir(flux);
            ValidateurSauvegarde.Resultat resultat = validateur.valider(temporaire);
            appliquer(temporaire, resultat);
            return resultat.manifeste();
        } catch (IOException e) {
            throw new UncheckedIOException("Restauration impossible", e);
        } finally {
            supprimerSansErreur(temporaire);
            verrou.unlock();
        }
    }

    private Path recevoir(InputStream flux) throws IOException {
        String dossier = proprietes.repertoireTemporaire();
        Path fichier;
        if (dossier == null || dossier.isBlank()) {
            fichier = Files.createTempFile("ludalex-restauration-", ".zip");
        } else {
            Files.createDirectories(Path.of(dossier));
            fichier = Files.createTempFile(Path.of(dossier), "ludalex-restauration-", ".zip");
        }
        try (OutputStream sortie = Files.newOutputStream(fichier)) {
            byte[] tampon = new byte[TAMPON];
            long total = 0;
            int lu;
            while ((lu = flux.read(tampon)) >= 0) {
                total += lu;
                if (total > tailleMax()) {
                    throw new SauvegardeTropGrosseException(tailleMax());
                }
                sortie.write(tampon, 0, lu);
            }
        } catch (IOException | RuntimeException e) {
            supprimerSansErreur(fichier);
            throw e;
        }
        return fichier;
    }

    private void appliquer(Path archive, ValidateurSauvegarde.Resultat resultat) throws IOException {
        Path racine = stockage.racine();
        Files.createDirectories(racine);
        if (Files.getFileStore(racine).getUsableSpace() < resultat.octetsImages() + MARGE_DISQUE) {
            throw new SauvegardeInvalideException("Espace disque insuffisant pour restaurer les images");
        }
        Path preparation = racine.resolve(PREFIXE_PREPARATION + UUID.randomUUID());
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Files.createDirectory(preparation);
            for (String nom : resultat.images()) {
                extraire(zip, nom, preparation);
            }
            importDonnees.remplacer(zip);
            installer(preparation, racine, resultat.images());
        } finally {
            supprimerArborescence(preparation);
        }
    }

    private static void extraire(ZipFile zip, String nom, Path preparation) throws IOException {
        ZipEntry entree = zip.getEntry(FormatSauvegarde.DOSSIER_IMAGES + nom);
        Path destination = preparation.resolve(nom).normalize();
        if (!destination.startsWith(preparation)) {
            throw new SauvegardeInvalideException("Chemin dangereux dans la sauvegarde");
        }
        try (InputStream flux = zip.getInputStream(entree)) {
            Files.copy(flux, destination);
        }
    }

    /** Après le commit : les images de la sauvegarde remplacent celles du volume, les autres sont supprimées. */
    private void installer(Path preparation, Path racine, Set<String> images) {
        try {
            for (String nom : images) {
                deplacer(preparation.resolve(nom), racine.resolve(nom));
            }
            try (DirectoryStream<Path> fichiers = Files.newDirectoryStream(racine)) {
                for (Path fichier : fichiers) {
                    String nom = fichier.getFileName().toString();
                    if (StockageImages.estNomValide(nom) && !images.contains(nom) && Files.isRegularFile(fichier)) {
                        Files.deleteIfExists(fichier);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Données restaurées mais installation des images incomplète : {}", e.toString());
            throw new IllegalStateException("Données restaurées, mais les images n'ont pas pu être toutes installées", e);
        }
    }

    private static void deplacer(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Supprime les répertoires de préparation laissés par un arrêt brutal pendant une restauration. */
    @EventListener(ApplicationReadyEvent.class)
    void nettoyerRestes() {
        Path racine = stockage.racine();
        if (!Files.isDirectory(racine)) {
            return;
        }
        try (DirectoryStream<Path> restes = Files.newDirectoryStream(racine, PREFIXE_PREPARATION + "*")) {
            for (Path reste : restes) {
                supprimerArborescence(reste);
            }
        } catch (IOException e) {
            LOG.warn("Nettoyage des restes de restauration impossible : {}", e.toString());
        }
    }

    private static void supprimerArborescence(Path dossier) {
        if (dossier == null || !Files.exists(dossier)) {
            return;
        }
        try (Stream<Path> arbre = Files.walk(dossier)) {
            arbre.sorted(Comparator.reverseOrder()).forEach(RestaurationSauvegarde::supprimerSansErreur);
        } catch (IOException e) {
            LOG.warn("Nettoyage impossible d'un répertoire temporaire : {}", e.toString());
        }
    }

    private static void supprimerSansErreur(Path chemin) {
        if (chemin == null) {
            return;
        }
        try {
            Files.deleteIfExists(chemin);
        } catch (IOException e) {
            LOG.warn("Suppression impossible d'un fichier temporaire : {}", e.toString());
        }
    }
}
