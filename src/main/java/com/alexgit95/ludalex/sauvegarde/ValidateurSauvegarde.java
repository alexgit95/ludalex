package com.alexgit95.ludalex.sauvegarde;

import static com.alexgit95.ludalex.sauvegarde.FormatSauvegarde.COMPTEUR_IMAGES;
import static com.alexgit95.ludalex.sauvegarde.FormatSauvegarde.DOSSIER_DONNEES;
import static com.alexgit95.ludalex.sauvegarde.FormatSauvegarde.DOSSIER_IMAGES;
import static com.alexgit95.ludalex.sauvegarde.FormatSauvegarde.MAPPER;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.springframework.stereotype.Component;

import com.alexgit95.ludalex.images.CalculateurCouleurFond;
import com.alexgit95.ludalex.images.FormatImage;
import com.alexgit95.ludalex.images.ImagesProperties;
import com.alexgit95.ludalex.images.StockageImages;
import com.alexgit95.ludalex.images.TypeImage;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.Pret;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Equipe;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.ResultatCooperatif;
import com.alexgit95.ludalex.parties.StatutPartie;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.EquipeDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.ImageDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.JeuDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.JoueurDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.Manifeste;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.ParticipationDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.PartieDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.PretDto;

/**
 * Valide entièrement un ZIP de sauvegarde avant toute modification : chemins, limites de taille, images,
 * manifeste, format des enregistrements, cohérence des références et compteurs. Rien n'est extrait.
 */
@Component
public class ValidateurSauvegarde {

    /** @param images noms des images uploadées référencées par les données (toutes présentes dans le ZIP) */
    record Resultat(Manifeste manifeste, Set<String> images, long octetsImages) {
    }

    private static final int MANIFESTE_MAX = 64 * 1024;
    private static final int NOM_ENTREE_MAX = 200;
    private static final long SEUIL_RATIO = 1024 * 1024;

    private final SauvegardeProperties proprietes;
    private final ImagesProperties imagesProprietes;

    public ValidateurSauvegarde(SauvegardeProperties proprietes, ImagesProperties imagesProprietes) {
        this.proprietes = proprietes;
        this.imagesProprietes = imagesProprietes;
    }

    Resultat valider(Path fichier) {
        try (ZipFile zip = new ZipFile(fichier.toFile())) {
            Budget budget = new Budget(proprietes.tailleDecompresseeMax().toBytes());
            Set<String> entrees = verifierEntrees(zip);
            Manifeste manifeste = lireManifeste(zip, budget);
            Map<String, Long> imagesDuZip = verifierImages(zip, entrees, budget);
            Etat etat = verifierDonnees(zip, entrees, budget);
            verifierCompteurs(manifeste, etat, imagesDuZip.size());
            long octetsImages = 0;
            for (String reference : etat.uploads) {
                Long taille = imagesDuZip.get(reference);
                if (taille == null) {
                    throw refus("Une image référencée par les données est absente de la sauvegarde");
                }
                octetsImages += taille;
            }
            return new Resultat(manifeste, etat.uploads, octetsImages);
        } catch (ZipException e) {
            throw new SauvegardeInvalideException("Le fichier n'est pas une sauvegarde ZIP valide (archive corrompue ou "
                    + "incomplète)", e);
        } catch (IOException e) {
            throw new SauvegardeInvalideException("Le fichier de sauvegarde est illisible", e);
        }
    }

    // ---- Entrées ----

    private Set<String> verifierEntrees(ZipFile zip) {
        Set<String> noms = new HashSet<>();
        Enumeration<? extends ZipEntry> liste = zip.entries();
        while (liste.hasMoreElements()) {
            ZipEntry entree = liste.nextElement();
            if (noms.size() >= proprietes.entreesMax()) {
                throw refus("La sauvegarde contient trop d'entrées (maximum " + proprietes.entreesMax() + ")");
            }
            String nom = entree.getName();
            verifierNom(nom);
            if (entree.isDirectory()) {
                if (!nom.equals(DOSSIER_DONNEES) && !nom.equals(DOSSIER_IMAGES)) {
                    throw refus("Entrée inattendue dans la sauvegarde");
                }
                continue;
            }
            if (!estEntreeAutorisee(nom)) {
                throw refus("Entrée inattendue dans la sauvegarde");
            }
            if (!noms.add(nom)) {
                throw refus("Entrée en double dans la sauvegarde");
            }
        }
        return noms;
    }

    private static void verifierNom(String nom) {
        boolean dangereux = nom.isEmpty() || nom.length() > NOM_ENTREE_MAX || nom.startsWith("/")
                || nom.indexOf('\\') >= 0 || nom.indexOf('\0') >= 0 || nom.indexOf(':') >= 0;
        if (!dangereux) {
            for (String segment : nom.split("/", -1)) {
                if (segment.equals("..") || segment.equals(".") || (segment.isEmpty() && !nom.endsWith("/"))) {
                    dangereux = true;
                }
            }
        }
        if (dangereux) {
            throw refus("Chemin dangereux dans la sauvegarde");
        }
    }

    private static boolean estEntreeAutorisee(String nom) {
        return nom.equals(FormatSauvegarde.MANIFESTE) || FormatSauvegarde.FICHIERS_DONNEES.contains(nom)
                || (nom.startsWith(DOSSIER_IMAGES) && StockageImages.estNomValide(nom.substring(DOSSIER_IMAGES.length())));
    }

    // ---- Manifeste ----

    private Manifeste lireManifeste(ZipFile zip, Budget budget) throws IOException {
        ZipEntry entree = zip.getEntry(FormatSauvegarde.MANIFESTE);
        if (entree == null) {
            throw refus("Manifeste manquant dans la sauvegarde");
        }
        Manifeste manifeste;
        try (InputStream flux = new FluxBorne(zip.getInputStream(entree), budget, entree, MANIFESTE_MAX)) {
            manifeste = MAPPER.readValue(flux, Manifeste.class);
        } catch (tools.jackson.core.JacksonException e) {
            throw new SauvegardeInvalideException("Le manifeste est illisible ou mal formé", e);
        }
        if (manifeste == null || manifeste.formatVersion() == null
                || !FormatSauvegarde.VERSIONS_ACCEPTEES.contains(manifeste.formatVersion())) {
            throw refus("Version de format non prise en charge (versions acceptées : "
                    + FormatSauvegarde.VERSIONS_ACCEPTEES + ")");
        }
        if (manifeste.compteurs() == null) {
            throw refus("Le manifeste ne contient pas de compteurs");
        }
        return manifeste;
    }

    // ---- Images ----

    private Map<String, Long> verifierImages(ZipFile zip, Set<String> entrees, Budget budget) throws IOException {
        long max = imagesProprietes.tailleMax().toBytes();
        Map<String, Long> tailles = new HashMap<>();
        for (String entree : entrees) {
            if (!entree.startsWith(DOSSIER_IMAGES)) {
                continue;
            }
            ZipEntry zipEntry = zip.getEntry(entree);
            String nom = entree.substring(DOSSIER_IMAGES.length());
            try (InputStream flux = new FluxBorne(zip.getInputStream(zipEntry), budget, zipEntry, max)) {
                byte[] entete = flux.readNBytes(12);
                FormatImage format = FormatImage.detecter(entete).orElseThrow(
                        () -> refus("Une image de la sauvegarde n'est pas un PNG, JPEG, GIF ou WebP valide"));
                if (FormatImage.depuisNom(nom).orElse(null) != format) {
                    throw refus("Une image de la sauvegarde ne correspond pas à son extension");
                }
                tailles.put(nom, entete.length + flux.transferTo(OutputStream.nullOutputStream()));
            }
        }
        return tailles;
    }

    // ---- Données ----

    private static final class Etat {
        final Set<Long> joueurs = new HashSet<>();
        final Set<Long> jeux = new HashSet<>();
        final Set<Long> parties = new HashSet<>();
        final Map<Long, Long> equipes = new HashMap<>();
        final Set<Long> participations = new HashSet<>();
        final Set<Long> prets = new HashSet<>();
        final Set<String> couples = new HashSet<>();
        final Set<String> uploads = new TreeSet<>();
    }

    private Etat verifierDonnees(ZipFile zip, Set<String> entrees, Budget budget) {
        for (String fichier : FormatSauvegarde.FICHIERS_DONNEES) {
            if (!entrees.contains(fichier)) {
                throw refus("Fichier manquant dans la sauvegarde : " + fichier);
            }
        }
        Etat etat = new Etat();
        lire(zip, FormatSauvegarde.JOUEURS, JoueurDto.class, budget, d -> joueur(d, etat));
        lire(zip, FormatSauvegarde.JEUX, JeuDto.class, budget, d -> jeu(d, etat));
        lire(zip, FormatSauvegarde.PARTIES, PartieDto.class, budget, d -> partie(d, etat));
        lire(zip, FormatSauvegarde.EQUIPES, EquipeDto.class, budget, d -> equipe(d, etat));
        lire(zip, FormatSauvegarde.PARTICIPATIONS, ParticipationDto.class, budget, d -> participation(d, etat));
        lire(zip, FormatSauvegarde.PRETS, PretDto.class, budget, d -> pret(d, etat));
        return etat;
    }

    private <T> void lire(ZipFile zip, String fichier, Class<T> type, Budget budget,
            java.util.function.Consumer<T> action) {
        ZipEntry entree = zip.getEntry(fichier);
        LectureSauvegarde.lireTableau(zip, fichier, type, flux -> new FluxBorne(flux, budget, entree,
                proprietes.tailleDecompresseeMax().toBytes()), action);
    }

    private void joueur(JoueurDto d, Etat etat) {
        identifiantUnique(d.idSauvegarde(), etat.joueurs, "joueur");
        texte(d.nom(), Joueur.NOM_MAX, "Nom de joueur");
        image(d.avatar(), etat);
        image(d.imageVictoire(), etat);
    }

    private void jeu(JeuDto d, Etat etat) {
        identifiantUnique(d.idSauvegarde(), etat.jeux, "jeu");
        texte(d.nom(), Jeu.NOM_MAX, "Nom de jeu");
        image(d.image(), etat);
        if (d.couleurFond() != null && !CalculateurCouleurFond.estCouleurValide(d.couleurFond())) {
            throw refus("Couleur de fond invalide dans les jeux");
        }
        if (d.prix() != null) {
            try {
                BigDecimal prix = new BigDecimal(d.prix());
                if (prix.signum() < 0 || prix.compareTo(Jeu.PRIX_MAX) > 0) {
                    throw refus("Prix invalide dans les jeux");
                }
            } catch (NumberFormatException e) {
                throw refus("Prix invalide dans les jeux");
            }
        }
        date(d.dateAchat(), false, "Date d'achat");
    }

    private void partie(PartieDto d, Etat etat) {
        identifiantUnique(d.idSauvegarde(), etat.parties, "partie");
        reference(d.jeuId(), etat.jeux, "jeu d'une partie");
        date(d.datePartie(), true, "Date de partie");
        if (d.lieu() != null && d.lieu().length() > Partie.LIEU_MAX) {
            throw refus("Lieu de partie trop long");
        }
        enumeration(StatutPartie.class, d.statut(), true, "Statut de partie");
        enumeration(ResultatCooperatif.class, d.resultatCooperatif(), false, "Résultat coopératif");
        if (d.joueurSuivantId() != null) {
            reference(d.joueurSuivantId(), etat.joueurs, "joueur suivant");
        }
        if (d.termineeLe() != null) {
            try {
                Instant.parse(d.termineeLe());
            } catch (DateTimeParseException e) {
                throw refus("Instant de fin de partie invalide");
            }
        }
    }

    private void equipe(EquipeDto d, Etat etat) {
        if (d.idSauvegarde() == null || etat.equipes.containsKey(d.idSauvegarde())) {
            throw refus("Identifiant d'équipe absent ou en double");
        }
        reference(d.partieId(), etat.parties, "partie d'une équipe");
        texte(d.nom(), Equipe.NOM_MAX, "Nom d'équipe");
        etat.equipes.put(d.idSauvegarde(), d.partieId());
    }

    private void participation(ParticipationDto d, Etat etat) {
        identifiantUnique(d.idSauvegarde(), etat.participations, "participation");
        reference(d.partieId(), etat.parties, "partie d'une participation");
        reference(d.joueurId(), etat.joueurs, "joueur d'une participation");
        if (!etat.couples.add(d.partieId() + ":" + d.joueurId())) {
            throw refus("Un joueur participe deux fois à la même partie");
        }
        if (d.equipeId() != null && !d.partieId().equals(etat.equipes.get(d.equipeId()))) {
            throw refus("Une participation référence une équipe absente ou d'une autre partie");
        }
    }

    private void pret(PretDto d, Etat etat) {
        identifiantUnique(d.idSauvegarde(), etat.prets, "prêt");
        reference(d.jeuId(), etat.jeux, "jeu d'un prêt");
        texte(d.emprunteur(), Pret.EMPRUNTEUR_MAX, "Emprunteur");
        date(d.datePret(), true, "Date de prêt");
        date(d.dateRetour(), false, "Date de retour");
    }

    // ---- Contrôles unitaires ----

    private void image(ImageDto image, Etat etat) {
        if (image == null) {
            return;
        }
        if (image.type() == null || image.reference() == null || image.reference().isBlank()
                || image.reference().length() > 2048) {
            throw refus("Image mal décrite dans les données");
        }
        if (TypeImage.UPLOAD.name().equals(image.type())) {
            if (!StockageImages.estNomValide(image.reference())) {
                throw refus("Nom de fichier d'image invalide dans les données");
            }
            etat.uploads.add(image.reference());
        } else if (TypeImage.URL.name().equals(image.type())) {
            adresseWeb(image.reference());
        } else {
            throw refus("Type d'image inconnu dans les données");
        }
    }

    private static void adresseWeb(String adresse) {
        try {
            URI uri = new URI(adresse);
            String schema = uri.getScheme();
            if (!"http".equalsIgnoreCase(schema) && !"https".equalsIgnoreCase(schema) || uri.getHost() == null) {
                throw refus("Adresse d'image invalide dans les données");
            }
        } catch (java.net.URISyntaxException e) {
            throw refus("Adresse d'image invalide dans les données");
        }
    }

    private static void identifiantUnique(Long id, Set<Long> connus, String type) {
        if (id == null || !connus.add(id)) {
            throw refus("Identifiant de " + type + " absent ou en double");
        }
    }

    private static void reference(Long id, Set<Long> connus, String description) {
        if (id == null || !connus.contains(id)) {
            throw refus("Référence incohérente : " + description + " est absent des données");
        }
    }

    private static void texte(String valeur, int max, String libelle) {
        if (valeur == null || valeur.isBlank() || valeur.length() > max) {
            throw refus(libelle + " absent ou trop long");
        }
    }

    private static void date(String valeur, boolean obligatoire, String libelle) {
        if (valeur == null) {
            if (obligatoire) {
                throw refus(libelle + " absente");
            }
            return;
        }
        try {
            LocalDate.parse(valeur);
        } catch (DateTimeParseException e) {
            throw refus(libelle + " invalide");
        }
    }

    private static <E extends Enum<E>> void enumeration(Class<E> type, String valeur, boolean obligatoire,
            String libelle) {
        if (valeur == null) {
            if (obligatoire) {
                throw refus(libelle + " absent");
            }
            return;
        }
        try {
            Enum.valueOf(type, valeur);
        } catch (IllegalArgumentException e) {
            throw refus(libelle + " inconnu");
        }
    }

    private static void verifierCompteurs(Manifeste manifeste, Etat etat, int images) {
        Map<String, Long> attendus = new LinkedHashMap<>();
        attendus.put("joueurs", (long) etat.joueurs.size());
        attendus.put("jeux", (long) etat.jeux.size());
        attendus.put("parties", (long) etat.parties.size());
        attendus.put("equipes", (long) etat.equipes.size());
        attendus.put("participations", (long) etat.participations.size());
        attendus.put("prets", (long) etat.prets.size());
        attendus.put(COMPTEUR_IMAGES, (long) images);
        for (Map.Entry<String, Long> attendu : attendus.entrySet()) {
            if (!attendu.getValue().equals(manifeste.compteurs().get(attendu.getKey()))) {
                throw refus("Le contenu ne correspond pas au manifeste (" + attendu.getKey() + ")");
            }
        }
    }

    private static SauvegardeInvalideException refus(String message) {
        return new SauvegardeInvalideException(message);
    }

    // ---- Limites de décompression ----

    private static final class Budget {
        long restant;

        Budget(long restant) {
            this.restant = restant;
        }
    }

    /** Lecture bornée : taille totale décompressée, taille de l'entrée et taux de compression. */
    private final class FluxBorne extends FilterInputStream {

        private final Budget budget;
        private final long compresse;
        private final long maxEntree;
        private long lus;

        FluxBorne(InputStream flux, Budget budget, ZipEntry entree, long maxEntree) {
            super(flux);
            this.budget = budget;
            this.compresse = Math.max(entree.getCompressedSize(), 1);
            this.maxEntree = maxEntree;
        }

        @Override
        public int read() throws IOException {
            int octet = super.read();
            if (octet >= 0) {
                compter(1);
            }
            return octet;
        }

        @Override
        public int read(byte[] tampon, int debut, int longueur) throws IOException {
            int lu = super.read(tampon, debut, longueur);
            if (lu > 0) {
                compter(lu);
            }
            return lu;
        }

        private void compter(int octets) {
            lus += octets;
            budget.restant -= octets;
            if (budget.restant < 0) {
                throw refus("Le contenu décompressé dépasse la taille maximale autorisée");
            }
            if (lus > maxEntree) {
                throw refus("Une entrée de la sauvegarde dépasse la taille maximale autorisée");
            }
            if (lus > SEUIL_RATIO && lus > (long) proprietes.ratioMax() * compresse) {
                throw refus("Taux de compression anormal : sauvegarde refusée");
            }
        }
    }
}
