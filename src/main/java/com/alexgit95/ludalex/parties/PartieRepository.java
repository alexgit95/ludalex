package com.alexgit95.ludalex.parties;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartieRepository extends JpaRepository<Partie, Long> {

    @Override
    @EntityGraph(attributePaths = "jeu")
    Page<Partie> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "jeu")
    Page<Partie> findByJeuId(Long jeuId, Pageable pageable);

    @Query("select p from Partie p join fetch p.jeu where p.id = :id")
    Optional<Partie> trouverAvecJeu(@Param("id") Long id);

    boolean existsByJeuId(Long jeuId);

    /** Partie en cours ou en pause (au plus une). */
    @Query("select p from Partie p join fetch p.jeu where p.statut in "
            + "(com.alexgit95.ludalex.parties.StatutPartie.EN_COURS, com.alexgit95.ludalex.parties.StatutPartie.EN_PAUSE)")
    List<Partie> trouverActives();

    /** Dernières parties d'un statut donné pour un jeu (la taille de la page borne le résultat). */
    List<Partie> findByJeuIdAndStatutOrderByDatePartieDescIdDesc(Long jeuId, StatutPartie statut, Pageable pageable);

    /** Lieux déjà saisis commençant par le motif (déjà en minuscules et échappé avec « ! »). */
    @Query("select distinct p.lieu from Partie p where p.lieu is not null "
            + "and lower(p.lieu) like :motif escape '!' order by p.lieu")
    List<String> lieuxCommencantPar(@Param("motif") String motif, Pageable pageable);

    @Query("select p from Participation p join fetch p.joueur left join fetch p.equipe "
            + "where p.partie.id in :ids order by p.id")
    List<Participation> participationsDesParties(@Param("ids") Collection<Long> ids);
}
