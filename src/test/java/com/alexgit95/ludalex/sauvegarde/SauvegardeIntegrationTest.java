package com.alexgit95.ludalex.sauvegarde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.alexgit95.ludalex.images.ImageSource;
import com.alexgit95.ludalex.images.StockageImages;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.Pret;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Equipe;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.ResultatCooperatif;
import com.alexgit95.ludalex.parties.StatutPartie;
import com.alexgit95.ludalex.parties.TokenFinPartie;
import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.Manifeste;

import jakarta.persistence.EntityManager;

/** Export puis restauration sur une base réelle (H2) : contenu, relations, images, tokens, atomicité, volume. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sauvegarde;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "ludalex.images.repertoire=target/test-sauvegarde/volume"
})
class SauvegardeIntegrationTest {

    private static final Path VOLUME = Path.of("target/test-sauvegarde/volume").toAbsolutePath();

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager gestionnaire;

    @Autowired
    private StockageImages stockage;

    @Autowired
    private SauvegardeExport export;

    @Autowired
    private RestaurationSauvegarde restauration;

    @Autowired
    private ImportDonnees importDonnees;

    @TempDir
    Path dossier;

    private TransactionTemplate tx;

    @BeforeEach
    void vider() throws IOException {
        tx = new TransactionTemplate(gestionnaire);
        viderBase();
        if (Files.exists(VOLUME)) {
            try (Stream<Path> fichiers = Files.list(VOLUME)) {
                for (Path fichier : fichiers.toList()) {
                    Files.deleteIfExists(fichier);
                }
            }
        }
    }

    // ---- Données de test ----

    private byte[] octetsPng(int cote) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(cote, cote,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", sortie);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }

    private record Jeux(ImageSource imageCatan, ImageSource avatarAlice) {
    }

    /** Un jeu de données qui exerce chaque champ et chaque relation. */
    private Jeux creerDonnees() {
        ImageSource imageCatan = stockage.enregistrer(octetsPng(3));
        ImageSource avatarAlice = stockage.enregistrer(octetsPng(5));
        tx.executeWithoutResult(s -> {
            Jeu catan = new Jeu("Catan");
            catan.setImage(imageCatan);
            catan.setCouleurFond("#112233");
            catan.setPrix(new java.math.BigDecimal("39.90"));
            catan.setDateAchat(LocalDate.of(2024, 5, 1));
            Jeu pandemic = new Jeu("Pandemic");
            pandemic.setImage(ImageSource.url("https://exemple.org/p.png"));
            pandemic.setCooperatif(true);
            em.persist(catan);
            em.persist(pandemic);

            Joueur alice = new Joueur("Alice");
            alice.setAvatar(avatarAlice);
            Joueur bruno = new Joueur("Bruno");
            bruno.setImageVictoire(ImageSource.url("https://exemple.org/v.png"));
            Joueur ancien = new Joueur("Ancien");
            ancien.anonymiser();
            em.persist(alice);
            em.persist(bruno);
            em.persist(ancien);

            Partie terminee = new Partie(catan, LocalDate.of(2025, 3, 1));
            terminee.setLieu("Salon");
            em.persist(terminee);
            Equipe rouge = new Equipe(terminee, "Rouge");
            Equipe bleue = new Equipe(terminee, "Bleue");
            em.persist(rouge);
            em.persist(bleue);
            Participation pa = new Participation(terminee, alice);
            pa.setScore(10);
            pa.setGagnant(true);
            pa.setEquipe(rouge);
            Participation pb = new Participation(terminee, bruno);
            pb.setScore(5);
            pb.setEquipe(bleue);
            em.persist(pa);
            em.persist(pb);

            Partie enPause = new Partie(pandemic, LocalDate.of(2026, 11, 1));
            enPause.setStatut(StatutPartie.EN_PAUSE);
            enPause.setJoueurSuivant(bruno);
            em.persist(enPause);
            em.persist(new Participation(enPause, alice));
            em.persist(new Participation(enPause, bruno));
            em.persist(new TokenFinPartie(enPause, "empreinte-secrete-a-ne-jamais-exporter",
                    Instant.parse("2030-01-01T00:00:00Z")));

            Partie perdue = new Partie(pandemic, LocalDate.of(2026, 1, 1));
            perdue.setResultatCooperatif(ResultatCooperatif.DEFAITE);
            perdue.setTermineeLe(Instant.parse("2026-01-01T10:00:00Z"));
            em.persist(perdue);
            em.persist(new Participation(perdue, ancien));

            Pret enCours = new Pret(catan, "Voisin", LocalDate.of(2025, 2, 1));
            Pret clos = new Pret(pandemic, "Cousine", LocalDate.of(2025, 1, 1));
            clos.clore(LocalDate.of(2025, 1, 15));
            em.persist(enCours);
            em.persist(clos);
        });
        return new Jeux(imageCatan, avatarAlice);
    }

    /** Description des données sans aucun identifiant technique : comparable avant et après restauration. */
    private List<String> instantane() {
        return tx.execute(s -> {
            List<String> r = new ArrayList<>();
            lignes(r, "joueur", "select j.nom, j.anonymise, j.avatar.type, j.avatar.reference, j.imageVictoire.type, "
                    + "j.imageVictoire.reference from Joueur j");
            lignes(r, "jeu", "select j.nom, j.couleurFond, j.prix, j.dateAchat, j.cooperatif, j.image.type, "
                    + "j.image.reference from Jeu j");
            lignes(r, "partie", "select p.jeu.nom, p.datePartie, p.lieu, p.statut, p.resultatCooperatif, s.nom, "
                    + "p.termineeLe from Partie p left join p.joueurSuivant s");
            lignes(r, "equipe", "select e.partie.jeu.nom, e.partie.datePartie, e.nom from Equipe e");
            lignes(r, "participation", "select a.partie.jeu.nom, a.partie.datePartie, a.joueur.nom, a.score, "
                    + "a.gagnant, e.nom from Participation a left join a.equipe e");
            lignes(r, "pret", "select p.jeu.nom, p.emprunteur, p.datePret, p.dateRetour from Pret p");
            lignes(r, "token", "select t.partie.jeu.nom from TokenFinPartie t");
            Collections.sort(r);
            return r;
        });
    }

    private void lignes(List<String> sortie, String type, String jpql) {
        for (Object[] ligne : em.createQuery(jpql, Object[].class).getResultList()) {
            sortie.add(type + "|" + Arrays.toString(ligne));
        }
    }

    private byte[] exporter() throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        export.ecrire(sortie);
        return sortie.toByteArray();
    }

    private static Map<String, byte[]> lireZip(byte[] zip) throws IOException {
        Map<String, byte[]> entrees = new LinkedHashMap<>();
        try (ZipInputStream flux = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entree = flux.getNextEntry(); entree != null; entree = flux.getNextEntry()) {
                entrees.put(entree.getName(), flux.readAllBytes());
            }
        }
        return entrees;
    }

    private long compter(String entite) {
        return tx.execute(s -> em.createQuery("select count(x) from " + entite + " x", Long.class).getSingleResult());
    }

    // ---- Export ----

    @Test
    void exporteUnZipCompletSansTokensNiImagesParUrl() throws IOException {
        Jeux images = creerDonnees();
        Files.write(VOLUME.resolve("99999999-9999-4999-8999-999999999999.png"), octetsPng(2));

        Map<String, byte[]> zip = lireZip(exporter());

        assertThat(zip.keySet()).containsExactlyInAnyOrder("manifest.json", "donnees/joueurs.json",
                "donnees/jeux.json", "donnees/parties.json", "donnees/equipes.json", "donnees/participations.json",
                "donnees/prets.json", "images/" + images.imageCatan().getReference(),
                "images/" + images.avatarAlice().getReference());
        String tout = zip.entrySet().stream().filter(e -> e.getKey().endsWith(".json"))
                .map(e -> new String(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("\n"));
        assertThat(tout).doesNotContain("empreinte").doesNotContain("empreinte-secrete").contains("https://exemple.org/p.png")
                .contains("\"anonymise\":true").contains("Joueur supprimé");
        assertThat(new String(zip.get("manifest.json"), StandardCharsets.UTF_8)).contains("\"formatVersion\":1")
                .contains("\"joueurs\":3").contains("\"jeux\":2").contains("\"parties\":3").contains("\"equipes\":2")
                .contains("\"participations\":5").contains("\"prets\":2").contains("\"images\":2");
        assertThat(zip.get("images/" + images.avatarAlice().getReference())).isEqualTo(octetsPng(5));
    }

    @Test
    void exporteUneImageManquanteSurLeVolumeSansImage() throws IOException {
        tx.executeWithoutResult(s -> {
            Joueur joueur = new Joueur("Sans fichier");
            joueur.setAvatar(ImageSource.upload(UUID.randomUUID() + ".png"));
            em.persist(joueur);
        });

        Map<String, byte[]> zip = lireZip(exporter());

        assertThat(zip.keySet()).noneMatch(nom -> nom.startsWith("images/"));
        assertThat(new String(zip.get("donnees/joueurs.json"), StandardCharsets.UTF_8)).contains("\"avatar\":null");
        assertThat(new String(zip.get("manifest.json"), StandardCharsets.UTF_8)).contains("\"images\":0");
    }

    @Test
    void exporteUneBaseVideEnUnZipRestaurable() throws IOException {
        byte[] zip = exporter();

        assertThat(lireZip(zip)).containsKey("manifest.json");
        restauration.restaurer(new ByteArrayInputStream(zip));
        assertThat(instantane()).isEmpty();
    }

    @Test
    void chaqueEntiteJpaEstExporteeOuExplicitementExclue() {
        Set<Class<?>> entites = em.getMetamodel().getEntities().stream().map(e -> (Class<?>) e.getJavaType())
                .collect(Collectors.toSet());
        Set<Class<?>> couvertes = new HashSet<>(FormatSauvegarde.ENTITES_EXPORTEES);
        couvertes.addAll(FormatSauvegarde.ENTITES_EXCLUES);

        assertThat(entites).isEqualTo(couvertes);
        assertThat(FormatSauvegarde.ENTITES_EXPORTEES).doesNotContainAnyElementsOf(FormatSauvegarde.ENTITES_EXCLUES);
        assertThat(FormatSauvegarde.FICHIERS_DONNEES).hasSameSizeAs(FormatSauvegarde.ENTITES_EXPORTEES);
    }

    // ---- Aller-retour ----

    private static final String ORPHELINE = "99999999-9999-4999-8999-999999999999.png";

    private void viderBase() {
        tx.executeWithoutResult(s -> {
            for (String entite : List.of("TokenFinPartie", "Participation", "Equipe", "Partie", "Pret", "Jeu",
                    "Joueur")) {
                em.createQuery("delete from " + entite).executeUpdate();
            }
        });
    }

    private static List<String> sansTokens(List<String> instantane) {
        return instantane.stream().filter(ligne -> !ligne.startsWith("token|")).toList();
    }

    private List<String> nomsDuVolume() throws IOException {
        try (Stream<Path> fichiers = Files.list(VOLUME)) {
            return fichiers.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void restaureExactementLesDonneesExporteesApresUnRemplacementComplet() throws IOException {
        Jeux images = creerDonnees();
        List<String> avant = instantane();
        byte[] sauvegarde = exporter();
        assertThat(avant).anyMatch(ligne -> ligne.startsWith("token|"));

        viderBase();
        tx.executeWithoutResult(s -> em.persist(new Jeu("Intrus")));
        Files.delete(VOLUME.resolve(images.imageCatan().getReference()));
        Files.write(VOLUME.resolve(ORPHELINE), octetsPng(2));

        Manifeste manifeste = restauration.restaurer(new ByteArrayInputStream(sauvegarde));

        assertThat(sansTokens(instantane())).containsExactlyElementsOf(sansTokens(avant));
        assertThat(compter("TokenFinPartie")).isZero();
        assertThat(manifeste.compteurs()).containsEntry("joueurs", 3L).containsEntry("participations", 5L);
        assertThat(nomsDuVolume()).containsExactlyInAnyOrder(images.imageCatan().getReference(),
                images.avatarAlice().getReference());
        assertThat(Files.readAllBytes(VOLUME.resolve(images.imageCatan().getReference()))).isEqualTo(octetsPng(3));
        assertThat(Files.readAllBytes(VOLUME.resolve(images.avatarAlice().getReference()))).isEqualTo(octetsPng(5));
        assertThat(stockage.charger(images.avatarAlice().getReference())).isPresent();
    }

    @Test
    void uneSecondeSauvegardeApresRestaurationRedonneLesMemesDonnees() throws IOException {
        creerDonnees();
        restauration.restaurer(new ByteArrayInputStream(exporter()));
        List<String> premiere = sansTokens(instantane());

        restauration.restaurer(new ByteArrayInputStream(exporter()));

        assertThat(sansTokens(instantane())).containsExactlyElementsOf(premiere);
    }

    @Test
    void restaureUneArchiveIndependanteDuMoteurAvecDesIdentifiantsQuelconques() throws IOException {
        creerDonnees();

        restauration.restaurer(new ByteArrayInputStream(ArchiveDeTest.valide().octets()));

        assertThat(instantane()).containsExactlyInAnyOrder(
                "joueur|[Alice, false, UPLOAD, " + ArchiveDeTest.IMAGE + ", null, null]",
                "joueur|[Bruno, false, URL, https://exemple.org/b.png, null, null]",
                "jeu|[Catan, #112233, 39.90, 2024-05-01, false, null, null]",
                "partie|[Catan, 2025-01-02, Salon, TERMINEE, null, null, null]",
                "equipe|[Catan, 2025-01-02, Rouge]",
                "participation|[Catan, 2025-01-02, Alice, 10, true, Rouge]",
                "participation|[Catan, 2025-01-02, Bruno, null, false, null]",
                "pret|[Catan, Voisin, 2025-02-01, null]");
        assertThat(nomsDuVolume()).containsExactly(ArchiveDeTest.IMAGE);
        assertThat(Files.readAllBytes(VOLUME.resolve(ArchiveDeTest.IMAGE))).isEqualTo(ArchiveDeTest.png());
    }

    @Test
    void uneArchiveInvalideNeModifieNiLesDonneesNiLesImages() throws IOException {
        creerDonnees();
        List<String> avant = instantane();
        List<String> volumeAvant = nomsDuVolume();

        assertThatThrownBy(() -> restauration
                .restaurer(new ByteArrayInputStream(ArchiveDeTest.valide().avec("../evil", "x").octets())))
                .isInstanceOf(SauvegardeInvalideException.class);

        assertThat(instantane()).isEqualTo(avant);
        assertThat(nomsDuVolume()).isEqualTo(volumeAvant);
    }

    @Test
    void unEchecEnCoursDImportAnnuleToutLeRemplacement() throws IOException {
        creerDonnees();
        List<String> avant = instantane();
        Path invalide = ArchiveDeTest.valide()
                .modifier("donnees/participations.json", "\"joueurId\":9002", "\"joueurId\":9999").ecrire(dossier);

        try (ZipFile zip = new ZipFile(invalide.toFile())) {
            assertThatThrownBy(() -> importDonnees.remplacer(zip)).isInstanceOf(IllegalStateException.class);
        }

        assertThat(instantane()).isEqualTo(avant);
    }

    @Test
    void exporteEtRestaureParLotsUnGrandNombreDEnregistrements() throws IOException {
        int total = 1100;
        tx.executeWithoutResult(s -> {
            Jeu jeu = new Jeu("Jeu");
            em.persist(jeu);
            for (int i = 0; i < total; i++) {
                Joueur joueur = new Joueur(String.format("J%04d", i));
                em.persist(joueur);
                Partie partie = new Partie(jeu, LocalDate.of(2025, 1, 1).plusDays(i % 300));
                em.persist(partie);
                em.persist(new Participation(partie, joueur));
            }
        });
        byte[] sauvegarde = exporter();
        Map<String, byte[]> zip = lireZip(sauvegarde);
        assertThat(new String(zip.get("manifest.json"), StandardCharsets.UTF_8)).contains("\"joueurs\":1100")
                .contains("\"parties\":1100").contains("\"participations\":1100");

        viderBase();
        restauration.restaurer(new ByteArrayInputStream(sauvegarde));

        assertThat(compter("Joueur")).isEqualTo(total);
        assertThat(compter("Partie")).isEqualTo(total);
        assertThat(compter("Participation")).isEqualTo(total);
        Long liens = tx.execute(s -> em.createQuery("select count(p) from Participation p "
                + "where p.joueur.nom = 'J1099' and p.partie.datePartie = :date", Long.class)
                .setParameter("date", LocalDate.of(2025, 1, 1).plusDays(1099 % 300)).getSingleResult());
        assertThat(liens).isEqualTo(1);
    }
}
