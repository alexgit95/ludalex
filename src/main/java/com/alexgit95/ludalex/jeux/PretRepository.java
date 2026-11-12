package com.alexgit95.ludalex.jeux;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PretRepository extends JpaRepository<Pret, Long> {

    @Query("select p from Pret p where p.jeu.id = :jeuId and p.dateRetour is null")
    Optional<Pret> trouverEnCours(@Param("jeuId") Long jeuId);

    @Query("select p from Pret p where p.jeu.id = :jeuId order by p.datePret desc, p.id desc")
    List<Pret> historique(@Param("jeuId") Long jeuId);

    /** Emprunteurs courants d'une page de jeux, en une seule requête. */
    @Query("select new com.alexgit95.ludalex.jeux.EmprunteurJeu(p.jeu.id, p.emprunteur) "
            + "from Pret p where p.jeu.id in :jeuIds and p.dateRetour is null")
    List<EmprunteurJeu> emprunteursEnCours(@Param("jeuIds") Collection<Long> jeuIds);

    @Modifying
    @Query("delete from Pret p where p.jeu.id = :jeuId")
    void supprimerParJeu(@Param("jeuId") Long jeuId);
}
