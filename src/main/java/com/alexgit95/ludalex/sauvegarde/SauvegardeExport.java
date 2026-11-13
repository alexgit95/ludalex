package com.alexgit95.ludalex.sauvegarde;

import static com.alexgit95.ludalex.sauvegarde.FormatSauvegarde.COMPTEUR_IMAGES;
import static com.alexgit95.ludalex.sauvegarde.FormatSauvegarde.DOSSIER_IMAGES;
import static com.alexgit95.ludalex.sauvegarde.FormatSauvegarde.MAPPER;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.alexgit95.ludalex.images.StockageImages;
import com.alexgit95.ludalex.images.TypeImage;
import com.alexgit95.ludalex.images.CopiesImages;
import com.alexgit95.ludalex.images.CoordinationImages;
import org.springframework.beans.factory.annotation.Autowired;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.EquipeDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.ImageDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.JeuDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.JoueurDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.Manifeste;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.ParticipationDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.PartieDto;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.PretDto;

import jakarta.persistence.EntityManager;
import tools.jackson.core.JsonGenerator;

/**
 * Écrit la sauvegarde en flux dans un ZIP : lecture par lots de 500 enregistrements (pagination par identifiant,
 * sans entité gérée donc sans contexte de persistance qui grossit) et images copiées par flux.
 * Le manifeste est écrit en dernier pour que ses compteurs reflètent exactement ce qui a été exporté.
 */
@Service
public class SauvegardeExport {

    static final int LOT = 500;

    private static final Logger LOG = LoggerFactory.getLogger(SauvegardeExport.class);

    private final EntityManager em;
    private final StockageImages stockage;
    private final Clock horloge;
    private final ObjectProvider<BuildProperties> build;
    private CopiesImages copies;
    private CoordinationImages coordination = new CoordinationImages();

    @Autowired
    void copies(CopiesImages copies, CoordinationImages coordination) {
        this.copies = copies;
        this.coordination = coordination;
    }

    public SauvegardeExport(EntityManager em, StockageImages stockage, Clock horloge,
            ObjectProvider<BuildProperties> build) {
        this.em = em;
        this.stockage = stockage;
        this.horloge = horloge;
        this.build = build;
    }

    /** Écrit le ZIP complet sur {@code sortie} sans la fermer. */
    public Manifeste ecrire(OutputStream sortie) throws IOException {
        coordination.verrou().lock();
        try { return ecrireSousVerrou(sortie); }
        finally { coordination.verrou().unlock(); }
    }

    private Manifeste ecrireSousVerrou(OutputStream sortie) throws IOException {
        Set<String> images = imagesPresentes();
        ZipOutputStream zip = new ZipOutputStream(sortie);
        zip.setLevel(Deflater.BEST_SPEED);
        Map<String, Long> compteurs = new LinkedHashMap<>();

        compteurs.put("joueurs", tableau(zip, FormatSauvegarde.JOUEURS,
                "select x.id, x.nom, x.avatar.type, x.avatar.reference, x.imageVictoire.type, "
                        + "x.imageVictoire.reference, x.anonymise from Joueur x where x.id > :apres order by x.id",
                (l, secours) -> new JoueurDto((Long) l[0], (String) l[1], image(l[2], l[3], images, secours), image(l[4], l[5], images, secours),
                        (Boolean) l[6])));
        compteurs.put("jeux", tableau(zip, FormatSauvegarde.JEUX,
                "select x.id, x.nom, x.image.type, x.image.reference, x.couleurFond, x.prix, x.dateAchat, "
                    + "x.cooperatif, x.plusieursVariantes from Jeu x where x.id > :apres order by x.id",
                (l, secours) -> new JeuDto((Long) l[0], (String) l[1], image(l[2], l[3], images, secours), (String) l[4],
                    texte(l[5]), texte(l[6]), (Boolean) l[7], (Boolean) l[8])));
        compteurs.put("parties", tableau(zip, FormatSauvegarde.PARTIES,
                "select x.id, x.jeu.id, x.datePartie, x.lieu, x.statut, x.resultatCooperatif, "
                    + "x.joueurSuivant.id, x.termineeLe, x.variante from Partie x where x.id > :apres order by x.id",
                (l, secours) -> new PartieDto((Long) l[0], (Long) l[1], texte(l[2]), (String) l[3], texte(l[4]), texte(l[5]),
                    (Long) l[6], texte(l[7]), (String) l[8])));
        compteurs.put("equipes", tableau(zip, FormatSauvegarde.EQUIPES,
                "select x.id, x.partie.id, x.nom from Equipe x where x.id > :apres order by x.id",
                (l, secours) -> new EquipeDto((Long) l[0], (Long) l[1], (String) l[2])));
        compteurs.put("participations", tableau(zip, FormatSauvegarde.PARTICIPATIONS,
                "select x.id, x.partie.id, x.joueur.id, x.score, x.gagnant, x.equipe.id "
                        + "from Participation x where x.id > :apres order by x.id",
                (l, secours) -> new ParticipationDto((Long) l[0], (Long) l[1], (Long) l[2], (Integer) l[3], (Boolean) l[4],
                        (Long) l[5])));
        compteurs.put("prets", tableau(zip, FormatSauvegarde.PRETS,
                "select x.id, x.jeu.id, x.emprunteur, x.datePret, x.dateRetour from Pret x "
                        + "where x.id > :apres order by x.id",
                (l, secours) -> new PretDto((Long) l[0], (Long) l[1], (String) l[2], texte(l[3]), texte(l[4]))));

        for (String nom : images) {
            copierImage(zip, nom);
        }
        compteurs.put(COMPTEUR_IMAGES, (long) images.size());

        Manifeste manifeste = new Manifeste(FormatSauvegarde.VERSION, versionApplication(),
                LocalDateTime.now(horloge).toString(), compteurs);
        zip.putNextEntry(new ZipEntry(FormatSauvegarde.MANIFESTE));
        zip.write(MAPPER.writeValueAsBytes(manifeste));
        zip.closeEntry();
        zip.finish();
        return manifeste;
    }

    private long tableau(ZipOutputStream zip, String fichier, String jpql, BiFunction<Object[], Map<String, String>, ?> convertir)
            throws IOException {
        zip.putNextEntry(new ZipEntry(fichier));
        long total = 0;
        try (JsonGenerator generateur = MAPPER.createGenerator(new SansFermeture(zip))) {
            generateur.writeStartArray();
            long dernier = 0;
            while (true) {
                List<Object[]> lot = em.createQuery(jpql, Object[].class).setParameter("apres", dernier)
                        .setMaxResults(LOT).getResultList();
                java.util.List<String> urls = new java.util.ArrayList<>();
                for (Object[] ligne : lot) {
                    for (int colonne = 0; colonne + 1 < ligne.length; colonne++) {
                        if (ligne[colonne] == TypeImage.URL) { urls.add((String) ligne[colonne + 1]); }
                    }
                }
                Map<String, String> secours = new LinkedHashMap<>();
                for (int debut = 0; copies != null && debut < urls.size(); debut += 100) {
                    copies.disponibles(urls.subList(debut, Math.min(debut + 100, urls.size()))).forEach((url, chemin) -> {
                        String nom = chemin.substring("/images/".length());
                        if (stockage.charger(nom).isPresent()) { secours.put(url, nom); }
                    });
                }
                for (Object[] ligne : lot) {
                    MAPPER.writeValue(generateur, convertir.apply(ligne, secours));
                    dernier = (Long) ligne[0];
                }
                total += lot.size();
                if (lot.size() < LOT) {
                    break;
                }
            }
            generateur.writeEndArray();
        }
        zip.closeEntry();
        return total;
    }

    private void copierImage(ZipOutputStream zip, String nom) throws IOException {
        Resource ressource = stockage.charger(nom)
                .orElseThrow(() -> new IOException("Image du volume disparue pendant l'export"));
        zip.putNextEntry(new ZipEntry(DOSSIER_IMAGES + nom));
        try (InputStream entree = ressource.getInputStream()) {
            entree.transferTo(zip);
        }
        zip.closeEntry();
    }

    /** Images uploadées référencées par les données et réellement présentes sur le volume. */
    private Set<String> imagesPresentes() {
        Set<String> noms = new TreeSet<>();
        for (String requete : List.of(
                "select x.avatar.reference from Joueur x where x.avatar.type = :type",
                "select x.imageVictoire.reference from Joueur x where x.imageVictoire.type = :type",
                "select x.image.reference from Jeu x where x.image.type = :type")) {
            for (String nom : em.createQuery(requete, String.class).setParameter("type", TypeImage.UPLOAD)
                    .getResultList()) {
                if (stockage.charger(nom).isPresent()) {
                    noms.add(nom);
                } else {
                    LOG.warn("Image uploadée absente du volume : elle n'est pas exportée");
                }
            }
        }
        return noms;
    }

    private static ImageDto image(Object type, Object reference, Set<String> presentes, Map<String, String> secours) {
        if (type == null || reference == null) {
            return null;
        }
        if (type == TypeImage.UPLOAD) {
            return presentes.contains(reference) ? new ImageDto(TypeImage.UPLOAD.name(), (String) reference) : null;
        }
        String copie = secours.get(reference);
        if (copie != null) { presentes.add(copie); }
        return new ImageDto(TypeImage.URL.name(), (String) reference, copie);
    }

    private static String texte(Object valeur) {
        if (valeur == null) {
            return null;
        }
        return valeur instanceof BigDecimal decimal ? decimal.toPlainString() : valeur.toString();
    }

    private String versionApplication() {
        BuildProperties proprietes = build.getIfAvailable();
        return proprietes == null ? "inconnue" : proprietes.getVersion();
    }

    /** Empêche le générateur JSON de fermer le ZIP à la fin d'une entrée. */
    private static final class SansFermeture extends FilterOutputStream {

        SansFermeture(OutputStream sortie) {
            super(sortie);
        }

        @Override
        public void write(byte[] octets, int debut, int longueur) throws IOException {
            out.write(octets, debut, longueur);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
