package com.alexgit95.ludalex.statistiques;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.alexgit95.ludalex.parties.ResultatCooperatif;
import com.alexgit95.ludalex.parties.StatutPartie;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Requêtes agrégées du tableau de bord : seules des agrégations sont lues (aucune partie, aucun jeu n'est chargé
 * comme entité). Les parties non terminées sont exclues ; les dates sont comptées par jour puis regroupées en Java
 * pour rester indépendant des fonctions de date de SQLite et de PostgreSQL.
 */
@Repository
public class StatistiquesGlobalesRepository {

    public record JourCompte(LocalDate jour, long nombre) {
    }

    public record NomCompte(String nom, long nombre) {
    }

    public record NomDate(String nom, LocalDate date) {
    }

    public record Ludotheque(long jeux, long cooperatifs, BigDecimal valeur, long avecPrix, long avecDate) {
    }

    public record TauxCooperatif(String nom, long parties, long victoires) {
    }

    public record CoutJeu(String nom, BigDecimal prix, long parties) {
    }

    public record JoueurResultat(String nom, long parties, long victoires, long jeux) {
    }

    public record RecordScore(String jeu, int maximum, double moyenne, long scores) {
    }

    public record Duo(String premier, String second, long parties) {
    }

    public record AchatJour(LocalDate jour, long jeux, BigDecimal montant) {
    }

    public record NomPrix(String nom, BigDecimal prix) {
    }

    public record DureePret(LocalDate debut, LocalDate fin, long nombre) {
    }

    public record PretEnCours(String jeu, String emprunteur, LocalDate depuis) {
    }

    public record Cooperatif(boolean cooperatif, long parties) {
    }

    private static final LocalDate DEBUT_MIN = LocalDate.of(1900, 1, 1);

    private final EntityManager em;

    public StatistiquesGlobalesRepository(EntityManager em) {
        this.em = em;
    }

    private static LocalDate debut(LocalDate debut) {
        return debut == null ? DEBUT_MIN : debut;
    }

    private <T> TypedQuery<T> requete(String jpql, Class<T> type) {
        return em.createQuery(jpql, type);
    }

    private TypedQuery<Object[]> terminees(String jpql) {
        return requete(jpql, Object[].class).setParameter("terminee", StatutPartie.TERMINEE);
    }

    private TypedQuery<Object[]> sur(String jpql, LocalDate debut, LocalDate fin) {
        return terminees(jpql).setParameter("debut", debut(debut)).setParameter("fin", fin);
    }

    private static long nombre(Object valeur) {
        return valeur == null ? 0 : ((Number) valeur).longValue();
    }

    // ---- Activité ----

    /** Nombre de parties terminées par jour, sur tout l'historique. */
    public List<JourCompte> partiesParJour() {
        return terminees("select p.datePartie, count(p) from Partie p where p.statut = :terminee "
                + "group by p.datePartie").getResultList().stream()
                .map(l -> new JourCompte((LocalDate) l[0], nombre(l[1]))).toList();
    }

    public List<NomCompte> lieux(LocalDate debut, LocalDate fin, int limite) {
        return sur("select p.lieu, count(p) from Partie p where p.statut = :terminee "
                + "and p.datePartie between :debut and :fin group by p.lieu order by count(p) desc, p.lieu", debut, fin)
                .setMaxResults(limite).getResultList().stream()
                .map(l -> new NomCompte((String) l[0], nombre(l[1]))).toList();
    }

    // ---- Jeux ----

    public Ludotheque ludotheque() {
        Object[] l = requete("select count(j), sum(case when j.cooperatif = true then 1 else 0 end), sum(j.prix), "
                + "count(j.prix), count(j.dateAchat) from Jeu j", Object[].class).getSingleResult();
        return new Ludotheque(nombre(l[0]), nombre(l[1]), (BigDecimal) l[2], nombre(l[3]), nombre(l[4]));
    }

    public long jeuxDistinctsJoues(LocalDate debut, LocalDate fin) {
        return nombre(sur("select count(distinct p.jeu.id) from Partie p where p.statut = :terminee "
                + "and p.datePartie between :debut and :fin", debut, fin).getSingleResult()[0]);
    }

    public List<NomCompte> jeuxLesPlusJoues(LocalDate debut, LocalDate fin, int limite) {
        return sur("select j.nom, count(p) from Partie p join p.jeu j where p.statut = :terminee "
                + "and p.datePartie between :debut and :fin group by j.id, j.nom order by count(p) desc, j.nom",
                debut, fin).setMaxResults(limite).getResultList().stream()
                .map(l -> new NomCompte((String) l[0], nombre(l[1]))).toList();
    }

    /** Jeux coopératifs classés par nombre de parties gagnées. */
    public List<NomCompte> jeuxLesPlusGagnes(LocalDate debut, LocalDate fin, int limite) {
        return sur("select j.nom, count(p) from Partie p join p.jeu j where p.statut = :terminee "
                + "and j.cooperatif = true and p.resultatCooperatif = :victoire "
                + "and p.datePartie between :debut and :fin group by j.id, j.nom order by count(p) desc, j.nom",
                debut, fin).setParameter("victoire", ResultatCooperatif.VICTOIRE).setMaxResults(limite)
                .getResultList().stream().map(l -> new NomCompte((String) l[0], nombre(l[1]))).toList();
    }

    public long jeuxJamaisJoues() {
        return nombre(terminees("select count(j) from Jeu j where not exists "
                + "(select 1 from Partie p where p.jeu = j and p.statut = :terminee)").getSingleResult()[0]);
    }

    public List<String> nomsJeuxJamaisJoues(int limite) {
        return requete("select j.nom from Jeu j where not exists "
                + "(select 1 from Partie p where p.jeu = j and p.statut = :terminee) order by j.nom", String.class)
                .setParameter("terminee", StatutPartie.TERMINEE).setMaxResults(limite).getResultList();
    }

    /** Jeux joués dont la dernière partie terminée est antérieure à {@code avant}. */
    public long jeuxDormants(LocalDate avant) {
        return nombre(terminees("select count(j) from Jeu j where "
                + "(select max(p.datePartie) from Partie p where p.jeu = j and p.statut = :terminee) < :avant")
                .setParameter("avant", avant).getSingleResult()[0]);
    }

    public List<NomDate> dormants(LocalDate avant, int limite) {
        return terminees("select j.nom, max(p.datePartie) from Partie p join p.jeu j where p.statut = :terminee "
                + "group by j.id, j.nom having max(p.datePartie) < :avant order by max(p.datePartie), j.nom")
                .setParameter("avant", avant).setMaxResults(limite).getResultList().stream()
                .map(l -> new NomDate((String) l[0], (LocalDate) l[1])).toList();
    }

    public List<Cooperatif> repartitionCooperatif(LocalDate debut, LocalDate fin) {
        return sur("select j.cooperatif, count(p) from Partie p join p.jeu j where p.statut = :terminee "
                + "and p.datePartie between :debut and :fin group by j.cooperatif", debut, fin).getResultList()
                .stream().map(l -> new Cooperatif((Boolean) l[0], nombre(l[1]))).toList();
    }

    public List<TauxCooperatif> tauxCooperatifParJeu(LocalDate debut, LocalDate fin) {
        return sur("select j.nom, count(p), sum(case when p.resultatCooperatif = :victoire then 1 else 0 end) "
                + "from Partie p join p.jeu j where p.statut = :terminee and j.cooperatif = true "
                + "and p.datePartie between :debut and :fin group by j.id, j.nom", debut, fin)
                .setParameter("victoire", ResultatCooperatif.VICTOIRE).getResultList().stream()
                .map(l -> new TauxCooperatif((String) l[0], nombre(l[1]), nombre(l[2]))).toList();
    }

    public List<NomCompte> variantes(LocalDate debut, LocalDate fin, int limite) {
        return sur("select p.variante, count(p) from Partie p where p.statut = :terminee and p.variante is not null "
                + "and p.datePartie between :debut and :fin group by p.variante order by count(p) desc, p.variante",
                debut, fin).setMaxResults(limite).getResultList().stream()
                .map(l -> new NomCompte((String) l[0], nombre(l[1]))).toList();
    }

    /** Jeux ayant un prix et au moins une partie terminée, sur tout l'historique. */
    public List<CoutJeu> coutsParJeu() {
        return terminees("select j.nom, j.prix, count(p) from Partie p join p.jeu j where p.statut = :terminee "
                + "and j.prix is not null and j.prix > 0 group by j.id, j.nom, j.prix").getResultList().stream()
                .map(l -> new CoutJeu((String) l[0], (BigDecimal) l[1], nombre(l[2]))).toList();
    }

    // ---- Joueurs (hors joueurs anonymisés) ----

    public List<JoueurResultat> joueurs(LocalDate debut, LocalDate fin) {
        return sur("select j.nom, count(pa), sum(case when pa.gagnant = true then 1 else 0 end), "
                + "count(distinct p.jeu.id) from Participation pa join pa.partie p join pa.joueur j "
                + "where p.statut = :terminee and j.anonymise = false and p.datePartie between :debut and :fin "
                + "group by j.id, j.nom", debut, fin).getResultList().stream()
                .map(l -> new JoueurResultat((String) l[0], nombre(l[1]), nombre(l[2]), nombre(l[3]))).toList();
    }

    public List<RecordScore> recordsScores(LocalDate debut, LocalDate fin, int limite) {
        return sur("select j.nom, max(pa.score), avg(pa.score), count(pa.score) from Participation pa "
                + "join pa.partie p join p.jeu j where p.statut = :terminee and pa.score is not null "
                + "and p.datePartie between :debut and :fin group by j.id, j.nom "
                + "order by max(pa.score) desc, j.nom", debut, fin).setMaxResults(limite).getResultList().stream()
                .map(l -> new RecordScore((String) l[0], ((Number) l[1]).intValue(), ((Number) l[2]).doubleValue(),
                        nombre(l[3])))
                .toList();
    }

    public List<Duo> duos(LocalDate debut, LocalDate fin, int limite) {
        return sur("select ja.nom, jb.nom, count(p) from Participation a join a.partie p join a.joueur ja "
                + "join Participation b on b.partie = p join b.joueur jb "
                + "where p.statut = :terminee and ja.id < jb.id and ja.anonymise = false and jb.anonymise = false "
                + "and p.datePartie between :debut and :fin group by ja.id, ja.nom, jb.id, jb.nom "
                + "order by count(p) desc, ja.nom, jb.nom", debut, fin).setMaxResults(limite).getResultList().stream()
                .map(l -> new Duo((String) l[0], (String) l[1], nombre(l[2]))).toList();
    }

    // ---- Ludothèque ----

    public List<AchatJour> achatsParJour(LocalDate debut, LocalDate fin) {
        return requete("select j.dateAchat, count(j), sum(j.prix) from Jeu j where j.dateAchat is not null "
                + "and j.dateAchat between :debut and :fin group by j.dateAchat", Object[].class)
                .setParameter("debut", debut(debut)).setParameter("fin", fin).getResultList().stream()
                .map(l -> new AchatJour((LocalDate) l[0], nombre(l[1]), (BigDecimal) l[2])).toList();
    }

    public List<NomPrix> jeuxLesPlusChers(int limite) {
        return requete("select j.nom, j.prix from Jeu j where j.prix is not null order by j.prix desc, j.nom",
                Object[].class).setMaxResults(limite).getResultList().stream()
                .map(l -> new NomPrix((String) l[0], (BigDecimal) l[1])).toList();
    }

    // ---- Prêts ----

    public List<JourCompte> pretsParJour(LocalDate debut, LocalDate fin) {
        return requete("select r.datePret, count(r) from Pret r where r.datePret between :debut and :fin "
                + "group by r.datePret", Object[].class).setParameter("debut", debut(debut))
                .setParameter("fin", fin).getResultList().stream()
                .map(l -> new JourCompte((LocalDate) l[0], nombre(l[1]))).toList();
    }

    public List<NomCompte> emprunteurs(LocalDate debut, LocalDate fin, int limite) {
        return requete("select r.emprunteur, count(r) from Pret r where r.datePret between :debut and :fin "
                + "group by r.emprunteur order by count(r) desc, r.emprunteur", Object[].class)
                .setParameter("debut", debut(debut)).setParameter("fin", fin).setMaxResults(limite)
                .getResultList().stream().map(l -> new NomCompte((String) l[0], nombre(l[1]))).toList();
    }

    /** Prêts rendus pendant la période, regroupés par couple de dates. */
    public List<DureePret> pretsTermines(LocalDate debut, LocalDate fin) {
        return requete("select r.datePret, r.dateRetour, count(r) from Pret r where r.dateRetour is not null "
                + "and r.dateRetour between :debut and :fin group by r.datePret, r.dateRetour", Object[].class)
                .setParameter("debut", debut(debut)).setParameter("fin", fin).getResultList().stream()
                .map(l -> new DureePret((LocalDate) l[0], (LocalDate) l[1], nombre(l[2]))).toList();
    }

    public long pretsEnCours() {
        return nombre(requete("select count(r) from Pret r where r.dateRetour is null", Long.class)
                .getSingleResult());
    }

    public List<PretEnCours> pretsEnCours(int limite) {
        return requete("select j.nom, r.emprunteur, r.datePret from Pret r join r.jeu j where r.dateRetour is null "
                + "order by r.datePret, j.nom", Object[].class).setMaxResults(limite).getResultList().stream()
                .map(l -> new PretEnCours((String) l[0], (String) l[1], (LocalDate) l[2])).toList();
    }
}
