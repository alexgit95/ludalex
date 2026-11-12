package com.alexgit95.ludalex.statistiques;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.alexgit95.ludalex.parties.Participation;

/** Requêtes agrégées : aucune partie n'est chargée en mémoire. */
public interface StatistiqueRepository extends Repository<Participation, Long> {

    @Query("select new com.alexgit95.ludalex.statistiques.ResultatJoueur("
            + "p.joueur.id, count(p), sum(case when p.gagnant = true then 1 else 0 end)) "
            + "from Participation p "
            + "where p.partie.jeu.id = :jeuId and p.partie.statut = com.alexgit95.ludalex.parties.StatutPartie.TERMINEE "
            + "group by p.joueur.id")
    List<ResultatJoueur> classement(@Param("jeuId") Long jeuId);

    /** Victoires des joueurs actifs pour plusieurs jeux à la fois (symbole de champion de la liste). */
    @Query("select new com.alexgit95.ludalex.statistiques.VictoiresJeuJoueur(p.partie.jeu.id, p.joueur.id, count(p)) "
            + "from Participation p "
            + "where p.partie.jeu.id in :jeuIds and p.partie.statut = com.alexgit95.ludalex.parties.StatutPartie.TERMINEE "
            + "and p.gagnant = true and p.joueur.anonymise = false "
            + "group by p.partie.jeu.id, p.joueur.id")
    List<VictoiresJeuJoueur> victoires(@Param("jeuIds") Collection<Long> jeuIds);
}
