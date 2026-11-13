package com.alexgit95.ludalex.importmyludo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.JeuMyLudo;
import com.alexgit95.ludalex.importmyludo.ModeleMyLudo.PartieMyLudo;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

/** Import sur base réelle (H2) avec les exports d'exemple anonymisés de {@code docs/exemple-my-ludo}. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:importmyludo;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ImportMyLudoIntegrationTest {

    private static final Path EXEMPLES = Path.of("docs/exemple-my-ludo");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager gestionnaire;

    @Autowired
    private LecteurExportMyLudo lecteur;

    @Autowired
    private AnalyseurImport analyseur;

    @Autowired
    private ImportTransactionnel importTransactionnel;

    @Autowired
    private ServiceImportMyLudo service;

    private TransactionTemplate tx;
    private byte[] collection;
    private byte[] parties;
    private List<JeuMyLudo> jeuxFichier;
    private List<PartieMyLudo> partiesFichier;

    @BeforeEach
    void preparer() throws IOException {
        tx = new TransactionTemplate(gestionnaire);
        tx.executeWithoutResult(s -> {
            for (String entite : List.of("TokenFinPartie", "Participation", "Equipe", "Partie", "Pret", "Jeu",
                    "Joueur")) {
                em.createQuery("delete from " + entite).executeUpdate();
            }
        });
        service.abandonner();
        collection = exemple("collection_");
        parties = exemple("plays_");
        jeuxFichier = lecteur.lireCollection(collection, "collection.json");
        partiesFichier = lecteur.lireParties(parties, "plays.json");
    }

    private static byte[] exemple(String prefixe) throws IOException {
        try (Stream<Path> fichiers = Files.list(EXEMPLES)) {
            return Files.readAllBytes(fichiers.filter(p -> p.getFileName().toString().startsWith(prefixe))
                    .findFirst().orElseThrow());
        }
    }

    private RapportImport importer(Map<String, Long> associations) {
        return importTransactionnel.executer(jeuxFichier, partiesFichier, associations, new Progression());
    }

    private long compter(String entite) {
        return tx.execute(s -> em.createQuery("select count(x) from " + entite + " x", Long.class)
                .getSingleResult());
    }

    private long parties(String jeu, LocalDate date) {
        return tx.execute(s -> em.createQuery("select count(p) from Partie p where p.jeu.nom = :jeu "
                + "and p.datePartie = :date", Long.class).setParameter("jeu", jeu).setParameter("date", date)
                .getSingleResult());
    }

    private List<Object[]> lignes(String jpql) {
        return tx.execute(s -> em.createQuery(jpql, Object[].class).getResultList());
    }

    // ---- Import des fichiers réels ----

    @Test
    void importeLesJeuxLesJoueursEtToutesLesPartiesDesExports() {
        RapportImport rapport = importer(Map.of());

        assertThat(rapport.jeuxCrees()).isEqualTo(jeuxFichier.size());
        assertThat(rapport.partiesAjoutees()).isEqualTo(partiesFichier.size());
        assertThat(rapport.partiesDejaPresentes()).isZero();
        assertThat(rapport.totalIgnorees()).isZero();
        assertThat(compter("Jeu")).isEqualTo(jeuxFichier.size());
        assertThat(compter("Partie")).isEqualTo(partiesFichier.size());
        assertThat(lignes("select j.plusieursVariantes from Jeu j"))
            .allSatisfy(l -> assertThat(l[0]).isEqualTo(false));
        assertThat(lignes("select p.variante from Partie p")).allSatisfy(l -> assertThat(l[0]).isNull());
        assertThat(lignes("select j.nom from Joueur j")).extracting(l -> l[0]).containsExactlyInAnyOrder("Alice",
                "Bruno", "Camille", "David", "Emma", "Anonyme", "Anonyme 2");
        assertThat(rapport.joueursCrees()).isEqualTo(7);
    }

    @Test
    void gardeToutesLesMmanchesIdentiquesDUnMemeJour() {
        long attendu = partiesFichier.stream().filter(p -> p.titre().equals("Toy Battle")
                && p.date().equals(LocalDate.of(2025, 9, 7))).count();
        assertThat(attendu).isGreaterThan(1);

        importer(Map.of());

        assertThat(parties("Toy Battle", LocalDate.of(2025, 9, 7))).isEqualTo(attendu);
    }

    @Test
    void reprendJeuxPrixDatesLieuxScoresEtEquipes() {
        importer(Map.of());

        assertThat(lignes("select j.prix, j.dateAchat, j.cooperatif from Jeu j where j.nom = 'Toy Battle'"))
                .singleElement().satisfies(l -> {
                    assertThat((BigDecimal) l[0]).isEqualByComparingTo("25");
                    assertThat(l[1]).isEqualTo(LocalDate.of(2025, 8, 5));
                    assertThat(l[2]).isEqualTo(false);
                });
        assertThat(lignes("select j.prix, j.dateAchat from Jeu j where j.nom like 'Star Wars%'")).singleElement()
                .satisfies(l -> assertThat(l[0]).isNull());
        assertThat(lignes("select j.nom, j.cooperatif from Jeu j where j.nom in ('Pandemic', 'Citadelles', "
                + "'Ghost Fightin'' Treasure Hunters', 'Zombie Kidz Évolution')")).extracting(l -> l[0] + "=" + l[1])
                .containsExactlyInAnyOrder("Pandemic=true", "Citadelles=false",
                        "Ghost Fightin' Treasure Hunters=true", "Zombie Kidz Évolution=true");
        assertThat(lignes("select p.lieu from Partie p where p.jeu.nom = 'Zodianimo' "
                + "and p.datePartie = {d '2026-10-05'}")).singleElement().satisfies(l -> assertThat(l[0]).isNull());
        assertThat(lignes("select p.lieu from Partie p where p.jeu.nom = 'Pandemic'")).singleElement()
                .satisfies(l -> assertThat(l[0]).isEqualTo("Lieu A"));
        assertThat(lignes("select a.joueur.nom, a.score, a.gagnant from Participation a "
                + "where a.partie.jeu.nom = 'Zodianimo' and a.partie.datePartie = {d '2026-10-05'}"))
                .extracting(l -> l[0] + "|" + l[1] + "|" + l[2]).containsExactlyInAnyOrder("Alice|29|false",
                        "Bruno|29|false", "Emma|33|true");
        assertThat(lignes("select a.score from Participation a where a.partie.jeu.nom = 'Toy Battle'"))
                .allSatisfy(l -> assertThat(l[0]).isNull());
        assertThat(lignes("select e.nom from Equipe e where e.partie.jeu.nom = 'Toy Battle'"))
                .extracting(l -> l[0]).containsExactlyInAnyOrder("blue", "green", "blue", "green");
        assertThat(lignes("select a.joueur.nom, e.nom from Participation a join a.equipe e "
                + "where a.partie.jeu.nom = 'Toy Battle'")).extracting(l -> l[0] + "|" + l[1])
                .contains("Alice|blue", "Bruno|green", "Emma|green");
    }

    @Test
    void convertitLesPartiesCooperatives() {
        importer(Map.of());

        assertThat(lignes("select p.resultatCooperatif, count(p) from Partie p where p.jeu.nom = 'Pandemic' "
                + "group by p.resultatCooperatif")).singleElement().satisfies(l -> {
                    assertThat(l[0]).hasToString("DEFAITE");
                    assertThat(l[1]).isEqualTo(1L);
                });
        // Partie marquée non coopérative sans vainqueur dans un jeu coopératif : défaite, sans gagnant
        assertThat(lignes("select a.gagnant from Participation a where a.partie.jeu.nom like 'Ghost Fightin%' "
                + "and a.partie.datePartie = {d '2024-09-28'} and a.partie.resultatCooperatif = 'DEFAITE'"))
                .hasSize(4).allSatisfy(l -> assertThat(l[0]).isEqualTo(false));
        assertThat(lignes("select a.gagnant from Participation a where a.partie.jeu.nom = 'Zombie Kidz Évolution' "
                + "and a.partie.resultatCooperatif = 'VICTOIRE'")).hasSize(9)
                .allSatisfy(l -> assertThat(l[0]).isEqualTo(true));
    }

    @Test
    void distingueLesJoueursAnonymesDUneMemePartie() {
        importer(Map.of());

        assertThat(lignes("select a.joueur.nom from Participation a where a.partie.jeu.nom = 'Galèrapagos' "
                + "and a.partie.datePartie = {d '2026-02-15'}")).extracting(l -> l[0]).contains("Anonyme", "Anonyme 2");
        assertThat(lignes("select a.partie.id, count(a) from Participation a group by a.partie.id"))
                .allSatisfy(l -> assertThat((Long) l[1]).isBetween(2L, 5L));
    }

    @Test
    void unSecondImportDuMemeFichierNAjouteRien() {
        importer(Map.of());

        RapportImport second = importer(Map.of());

        assertThat(second.partiesAjoutees()).isZero();
        assertThat(second.partiesDejaPresentes()).isEqualTo(partiesFichier.size());
        assertThat(second.jeuxCrees()).isZero();
        assertThat(second.jeuxInchanges()).isEqualTo(jeuxFichier.size());
        assertThat(second.joueursCrees()).isZero();
        assertThat(compter("Partie")).isEqualTo(partiesFichier.size());
        assertThat(compter("Joueur")).isEqualTo(7);
        assertThat(compter("Participation")).isEqualTo(partiesFichier.stream().mapToLong(p -> p.joueurs().size()).sum());
    }

    @Test
    void ajouteSeulementLeDeltaPourUnExportPlusRecent() {
        List<PartieMyLudo> anciennes = partiesFichier.stream().filter(p -> p.date().isBefore(LocalDate.of(2025, 9, 1)))
                .toList();
        importTransactionnel.executer(jeuxFichier, anciennes, Map.of(), new Progression());
        assertThat(compter("Partie")).isEqualTo(anciennes.size());

        RapportImport rapport = importer(Map.of());

        assertThat(rapport.partiesDejaPresentes()).isEqualTo(anciennes.size());
        assertThat(rapport.partiesAjoutees()).isEqualTo(partiesFichier.size() - anciennes.size());
        assertThat(compter("Partie")).isEqualTo(partiesFichier.size());
    }

    // ---- Rien d'existant n'est écrasé ----

    @Test
    void neModifiePasLesPartiesExistantesEtNeCompleteQueLesChampsVides() {
        tx.executeWithoutResult(s -> {
            Joueur alice = new Joueur("alice");
            Joueur emma = new Joueur("Émma");
            Jeu toyBattle = new Jeu("TOY BATTLE");
            Jeu pandemic = new Jeu("Pandemic");
            pandemic.setPrix(new BigDecimal("10.00"));
            pandemic.setDateAchat(LocalDate.of(2020, 1, 1));
            pandemic.setCooperatif(true);
            em.persist(alice);
            em.persist(emma);
            em.persist(toyBattle);
            em.persist(pandemic);
            Partie existante = new Partie(toyBattle, LocalDate.of(2025, 9, 7));
            existante.setLieu("Maison");
            em.persist(existante);
            Participation pa = new Participation(existante, alice);
            pa.setScore(99);
            pa.setGagnant(true);
            em.persist(pa);
            em.persist(new Participation(existante, emma));
        });
        long manchesDuFichier = partiesFichier.stream().filter(p -> p.titre().equals("Toy Battle")
                && p.date().equals(LocalDate.of(2025, 9, 7))).count();

        RapportImport rapport = importer(Map.of());

        assertThat(rapport.partiesDejaPresentes()).isEqualTo(1);
        assertThat(parties("TOY BATTLE", LocalDate.of(2025, 9, 7))).isEqualTo(manchesDuFichier);
        assertThat(lignes("select p.lieu from Partie p where p.lieu = 'Maison'")).hasSize(1);
        assertThat(lignes("select a.score from Participation a where a.score = 99 and a.partie.lieu = 'Maison'")).hasSize(1);
        assertThat(compter("Joueur")).isEqualTo(7);
        assertThat(lignes("select j.nom, j.prix, j.dateAchat from Jeu j where j.nom = 'TOY BATTLE'")).singleElement()
                .satisfies(l -> {
                    assertThat((BigDecimal) l[1]).isEqualByComparingTo("25");
                    assertThat(l[2]).isEqualTo(LocalDate.of(2025, 8, 5));
                });
        assertThat(lignes("select j.prix, j.dateAchat from Jeu j where j.nom = 'Pandemic'")).singleElement()
                .satisfies(l -> {
                    assertThat((BigDecimal) l[0]).isEqualByComparingTo("10");
                    assertThat(l[1]).isEqualTo(LocalDate.of(2020, 1, 1));
                });
        assertThat(compter("Jeu")).isEqualTo(jeuxFichier.size());
        assertThat(rapport.jeuxCompletes()).isEqualTo(1);
        assertThat(rapport.joueursAssocies()).isEqualTo(2);
    }

    @Test
    void unePartieExistanteAvecUnJoueurDeMoinsResteUnConflit() {
        tx.executeWithoutResult(s -> {
            Joueur alice = new Joueur("Alice");
            Jeu jeu = new Jeu("Zodianimo");
            em.persist(alice);
            em.persist(jeu);
            Partie existante = new Partie(jeu, LocalDate.of(2026, 10, 5));
            em.persist(existante);
            em.persist(new Participation(existante, alice));
        });

        RapportImport rapport = importer(Map.of());

        assertThat(rapport.partiesDejaPresentes()).isEqualTo(1);
        assertThat(parties("Zodianimo", LocalDate.of(2026, 10, 5))).isEqualTo(1);
    }

    @Test
    void associeUnNomDeLExportAUneFicheExistanteChoisie() {
        Long robert = tx.execute(s -> {
            Joueur joueur = new Joueur("Robert");
            em.persist(joueur);
            return joueur.getId();
        });

        RapportImport rapport = importer(Map.of("bruno", robert));

        assertThat(lignes("select j.nom from Joueur j")).extracting(l -> l[0]).doesNotContain("Bruno")
                .contains("Robert");
        assertThat(lignes("select count(a) from Participation a where a.joueur.nom = 'Robert'").get(0)[0])
                .isNotEqualTo(0L);
        assertThat(rapport.joueursAssocies()).isEqualTo(1);
    }

    @Test
    void unEchecAuMilieuNEnregistreRien() {
        tx.executeWithoutResult(s -> em.persist(new Joueur("Emma")));

        assertThatThrownBy(() -> importer(java.util.Collections.singletonMap("emma", null)))
                .isInstanceOf(ImportMyLudoException.class).hasMessageContaining("existe déjà");

        assertThat(compter("Jeu")).isZero();
        assertThat(compter("Partie")).isZero();
        assertThat(compter("Joueur")).isEqualTo(1);
    }

    // ---- Analyse à blanc ----

    @Test
    void analyseSansRienEcrire() {
        PlanImport plan = analyseur.analyser(jeuxFichier, partiesFichier, Map.of(), new Progression());

        assertThat(plan.jeuxACreer()).isEqualTo(jeuxFichier.size());
        assertThat(plan.parties()).hasSize(partiesFichier.size());
        assertThat(plan.dejaPresentes()).isZero();
        assertThat(plan.joueurs()).hasSize(7);
        assertThat(plan.joueursACreer()).isEqualTo(7);
        assertThat(compter("Jeu")).isZero();
        assertThat(compter("Joueur")).isZero();
    }

    @Test
    void proposeLesFichesExistantesParNomNormalise() {
        tx.executeWithoutResult(s -> em.persist(new Joueur("ÉMMA")));

        PlanImport plan = analyseur.analyser(jeuxFichier, partiesFichier, Map.of(), null);

        assertThat(plan.joueurs()).filteredOn(j -> j.nom().equals("Emma")).singleElement()
                .satisfies(j -> assertThat(j.propose()).isNotNull().isEqualTo(j.effectif()));
        assertThat(plan.joueursACreer()).isEqualTo(6);
    }

    private static byte[] json(String texte) {
        return texte.getBytes(StandardCharsets.UTF_8);
    }

    private PlanImport analyserTexte(String collectionJson, String partiesJson) {
        List<JeuMyLudo> jeux = collectionJson == null ? List.of()
                : lecteur.lireCollection(json(collectionJson), "c.json");
        List<PartieMyLudo> liste = lecteur.lireParties(json(partiesJson), "p.json");
        return analyseur.analyser(jeux, liste, Map.of(), null);
    }

    private static String partie(String titre, String date, String lieu, String joueurs, String extras) {
        return "{\"Titre\":\"" + titre + "\",\"Date\":\"" + date + "\",\"Lieu\":\"" + lieu + "\",\"Joueurs\":["
                + joueurs + "]," + extras + "}";
    }

    private static String joueur(String nom, int score, boolean victoire) {
        return "{\"Nom\":\"" + nom + "\",\"Score\":" + score + ",\"Victoire\":" + victoire + "}";
    }

    @Test
    void ignoreLesPartiesInexploitablesAvecLeurRaison() {
        String alice = joueur("Alice", 0, true);
        String perdant = joueur("Bruno", 0, false);
        String parties = "[" + String.join(",",
                partie("Jeu A", "2025-01-01", "", alice, "\"Exclue\":\"Oui\""),
                partie("Jeu A", "2999-01-01", "", alice, "\"Exclue\":\"Non\""),
                partie("Jeu A", "2025-01-02", "", "", "\"Exclue\":\"Non\""),
                partie("Jeu A", "2025-01-03", "", perdant, "\"Coop\":\"Non\""),
                partie("Jeu A", "2025-01-04", "", alice, "\"Coop\":\"Non\"")) + "]";

        PlanImport plan = analyserTexte(null, parties);

        assertThat(plan.parties()).hasSize(1);
        assertThat(plan.totalIgnorees()).isEqualTo(4);
        assertThat(plan.ignoreesParRaison()).containsEntry("Partie exclue dans MyLudo", 1)
                .containsEntry("Date dans le futur", 1).containsEntry("Aucun joueur", 1)
                .containsEntry("Aucun gagnant dans un jeu non coopératif", 1);
        assertThat(plan.detailsIgnorees()).anyMatch(l -> l.contains("Entrée n°4") && l.contains("Jeu A, 2025-01-03"));
    }

    @Test
    void deduitLeCaractereCooperatifDuJeuEtLeResultat() {
        String perdants = joueur("Alice", 0, false) + "," + joueur("Bruno", 0, false);
        String parties = "[" + String.join(",",
                partie("Coopi", "2025-01-01", "", perdants, "\"Coop\":\"Oui\",\"Coop Victoire\":\"Oui\""),
                partie("Coopi", "2025-01-02", "", perdants, "\"Coop\":\"Oui\",\"Coop Victoire\":\"Non\""),
                partie("Coopi", "2025-01-03", "", perdants, "\"Coop\":\"Non\"")) + "]";

        PlanImport plan = analyserTexte(null, parties);

        assertThat(plan.parties()).extracting(p -> p.resultat().name()).containsExactly("VICTOIRE", "DEFAITE",
                "DEFAITE");
        assertThat(plan.parties().get(0).participants()).allMatch(p -> p.gagnant());
        assertThat(plan.parties().get(1).participants()).noneMatch(p -> p.gagnant());
        assertThat(plan.jeux()).singleElement().satisfies(j -> assertThat(j.cooperatif()).isTrue());
    }

    @Test
    void laMecaniqueCooperativeDeLaCollectionSuffit() {
        String collectionJson = "[{\"Titre\":\"Coopi\",\"Mécanisme(s)\":[\"Coopération\"]},"
                + "{\"Titre\":\"Semi\",\"Mécanisme(s)\":[\"Semi-coopération\"]}]";

        PlanImport plan = analyserTexte(collectionJson, "[]");

        assertThat(plan.jeux()).extracting(j -> j.nom() + "=" + j.cooperatif()).containsExactly("Coopi=true",
                "Semi=false");
    }

    @Test
    void signaleLesDonneesQuiSontAdapteesAvecUnAvertissement() {
        String joueurs = joueur("Alice", 0, true) + ",{\"Nom\":\"Bruno\",\"Score\":0,\"Victoire\":false}";
        String sixEquipes = IntStream5.joueursEnEquipes();
        String parties = "[" + String.join(",",
                partie("Jeu A", "2025-01-01", "L".repeat(120), joueurs, "\"Coop\":\"Non\""),
                partie("Jeu A", "2025-01-02", "", sixEquipes, "\"Coop\":\"Non\"")) + "]";
        String collectionJson = "[{\"Titre\":\"Jeu A\",\"Prix indicatif\":0,\"Date d\\u0027acquisition\":\"2999-01-01\"},"
                + "{\"Titre\":\"Jeu B\",\"Prix indicatif\":123456789}]";

        PlanImport plan = analyserTexte(collectionJson, parties);

        assertThat(plan.parties().get(0).lieu()).hasSize(100);
        assertThat(plan.parties().get(1).participants()).allMatch(p -> p.equipe() == null);
        assertThat(plan.avertissements()).anyMatch(l -> l.contains("lieu tronqué"))
                .anyMatch(l -> l.contains("plus de 4 équipes")).anyMatch(l -> l.contains("Jeu A : date d'achat"))
                .anyMatch(l -> l.contains("Jeu B : prix trop élevé"));
        assertThat(plan.jeux()).filteredOn(j -> j.nom().equals("Jeu A")).singleElement()
                .satisfies(j -> {
                    assertThat(j.dateAchat()).isNull();
                    assertThat(j.prix()).isNull();
                });
    }

    @Test
    void enregistrePasDeScoreQuandTousSontNulsEtGardeLesScoresSinon() {
        String nuls = joueur("Alice", 0, true) + "," + joueur("Bruno", 0, false);
        String reels = joueur("Alice", 12, true) + "," + joueur("Bruno", 0, false);
        String parties = "[" + String.join(",", partie("Jeu A", "2025-01-01", "", nuls, "\"Coop\":\"Non\""),
                partie("Jeu A", "2025-01-02", "", reels, "\"Coop\":\"Non\"")) + "]";

        PlanImport plan = analyserTexte(null, parties);

        assertThat(plan.parties().get(0).participants()).allMatch(p -> p.score() == null);
        assertThat(plan.parties().get(1).participants()).extracting(p -> p.score()).containsExactly(12, 0);
    }

    @Test
    void ignoreUnePartieOuDeuxNomsSontAssociesAuMemeJoueur() {
        Long id = tx.execute(s -> {
            Joueur joueur = new Joueur("Alice");
            em.persist(joueur);
            return joueur.getId();
        });
        String parties = "[" + partie("Jeu A", "2025-01-01", "",
                joueur("Alice", 0, true) + "," + joueur("Alicia", 0, false), "\"Coop\":\"Non\"") + "]";

        PlanImport plan = analyseur.analyser(List.of(), lecteur.lireParties(json(parties), "p.json"),
                Map.of("alicia", id), null);

        assertThat(plan.parties()).isEmpty();
        assertThat(plan.ignoreesParRaison()).containsEntry("Deux noms associés au même joueur", 1);
    }

    @Test
    void lesJoueursAnonymisesNeSontJamaisProposes() {
        tx.executeWithoutResult(s -> {
            Joueur ancien = new Joueur("Alice");
            ancien.anonymiser();
            em.persist(ancien);
        });

        PlanImport plan = analyserTexte(null, "[" + partie("Jeu A", "2025-01-01", "",
                joueur("Alice", 0, true), "\"Coop\":\"Non\"") + "]");

        assertThat(plan.joueurs()).singleElement().satisfies(j -> assertThat(j.propose()).isNull());
    }

    // ---- Orchestration asynchrone ----

    private ServiceImportMyLudo.Vue attendre(Predicate<ServiceImportMyLudo.Vue> condition) throws InterruptedException {
        long fin = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < fin) {
            ServiceImportMyLudo.Vue vue = service.vue();
            if (vue != null && condition.test(vue)) {
                return vue;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Délai dépassé : " + service.vue());
    }

    @Test
    void analysePuisImportSuiviParLaSession() throws Exception {
        service.demarrerAnalyse(collection, "collection.json", parties, "plays.json");

        ServiceImportMyLudo.Vue analyse = attendre(v -> v.etat() == ServiceImportMyLudo.Etat.ANALYSE_PRETE);
        assertThat(analyse.plan().parties()).hasSize(partiesFichier.size());
        assertThat(analyse.fichiers()).containsExactly("collection.json", "plays.json");
        assertThat(compter("Partie")).isZero();

        service.demarrerImport(Map.of());
        ServiceImportMyLudo.Vue fin = attendre(v -> !v.enCours());

        assertThat(fin.etat()).isEqualTo(ServiceImportMyLudo.Etat.TERMINE);
        assertThat(fin.rapport().partiesAjoutees()).isEqualTo(partiesFichier.size());
        assertThat(fin.progression().phase()).isEqualTo(Progression.Phase.IMPORT);
        assertThat(fin.progression().fait()).isEqualTo(fin.progression().total());
        assertThat(compter("Partie")).isEqualTo(partiesFichier.size());
    }

    @Test
    void appliqueLesChoixDeCorrespondanceDesJoueurs() throws Exception {
        Long robert = tx.execute(s -> {
            Joueur joueur = new Joueur("Robert");
            em.persist(joueur);
            return joueur.getId();
        });
        service.demarrerAnalyse(null, null, parties, "plays.json");
        ServiceImportMyLudo.Vue analyse = attendre(v -> v.etat() == ServiceImportMyLudo.Etat.ANALYSE_PRETE);
        int indexBruno = analyse.plan().joueurs().stream().filter(j -> j.nom().equals("Bruno")).findFirst()
                .orElseThrow().index();
        assertThat(analyse.joueursExistants()).extracting(ServiceImportMyLudo.JoueurChoix::nom)
                .containsExactly("Robert");

        service.demarrerImport(Map.of(indexBruno, String.valueOf(robert)));

        assertThat(attendre(v -> !v.enCours()).etat()).isEqualTo(ServiceImportMyLudo.Etat.TERMINE);
        assertThat(lignes("select j.nom from Joueur j")).extracting(l -> l[0]).doesNotContain("Bruno");
        assertThat(compter("Jeu")).isEqualTo(
                partiesFichier.stream().map(PartieMyLudo::titre).distinct().count());
    }

    @Test
    void rapporteUneErreurDeFichierAvecSonDiagnostic() throws Exception {
        service.demarrerAnalyse(null, null, json("[{\"Titre\":\"A\",\"Date\":\"pas une date\",\"Joueurs\":[]}]"),
                "plays.json");

        ServiceImportMyLudo.Vue vue = attendre(v -> v.etat() == ServiceImportMyLudo.Etat.ERREUR);

        assertThat(vue.erreur()).contains("plays.json").contains("entrée n°1").contains("champ « Date »");
        assertThat(vue.detailsErreur()).contains("Fichier : plays.json", "Entrée n°1", "Champ : Date");
        assertThat(compter("Partie")).isZero();
    }

    @Test
    void refuseUnImportInvalideEtUneAnalyseSansFichier() throws Exception {
        assertThatThrownBy(() -> service.demarrerAnalyse(null, null, new byte[0], null))
                .isInstanceOf(ImportMyLudoException.class).hasMessageContaining("au moins un fichier");
        assertThatThrownBy(() -> service.demarrerImport(Map.of())).isInstanceOf(ImportMyLudoException.class)
                .hasMessageContaining("Aucune analyse prête");

        service.demarrerAnalyse(null, null, parties, "plays.json");
        attendre(v -> v.etat() == ServiceImportMyLudo.Etat.ANALYSE_PRETE);
        assertThatThrownBy(() -> service.demarrerImport(Map.of(0, "999999")))
                .isInstanceOf(ImportMyLudoException.class).hasMessageContaining("Choix de joueur invalide");
        assertThatThrownBy(() -> service.demarrerImport(Map.of(0, "pas-un-nombre")))
                .isInstanceOf(ImportMyLudoException.class).hasMessageContaining("Choix de joueur invalide");
    }

    @Test
    void refuseUnFichierAuMauvaisEndroitEtUnFichierVide() throws Exception {
        service.demarrerAnalyse(parties, "plays.json", null, null);
        ServiceImportMyLudo.Vue vue = attendre(v -> v.etat() == ServiceImportMyLudo.Etat.ERREUR);
        assertThat(vue.erreur()).contains("export de parties");

        service.abandonner();
        service.demarrerAnalyse(json("[]"), "collection.json", null, null);
        assertThat(attendre(v -> v.etat() == ServiceImportMyLudo.Etat.ERREUR).erreur())
                .contains("aucun enregistrement");
    }

    @Test
    void annulerOubliesLAnalyse() throws Exception {
        service.demarrerAnalyse(null, null, parties, "plays.json");
        attendre(v -> v.etat() == ServiceImportMyLudo.Etat.ANALYSE_PRETE);

        service.abandonner();

        assertThat(service.vue()).isNull();
    }

    @Test
    void compteLesEntreesDesExportsDExemple() {
        assertThat(JSON.readTree(collection).size()).isEqualTo(jeuxFichier.size());
        assertThat(JSON.readTree(parties).size()).isEqualTo(partiesFichier.size());
    }

    /** Six joueurs répartis en cinq équipes. */
    private static final class IntStream5 {
        static String joueursEnEquipes() {
            StringBuilder texte = new StringBuilder();
            String[] noms = { "A", "B", "C", "D", "E" };
            for (int i = 0; i < noms.length; i++) {
                texte.append(i == 0 ? "" : ",").append("{\"Nom\":\"").append(noms[i]).append("\",\"Score\":0,")
                        .append("\"Victoire\":").append(i == 0).append(",\"Équipe\":\"eq").append(i).append("\"}");
            }
            return texte.toString();
        }
    }
}
