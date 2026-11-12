package com.alexgit95.ludalex.parties;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenFinPartieRepository extends JpaRepository<TokenFinPartie, Long> {

    @Query("select t from TokenFinPartie t join fetch t.partie where t.empreinte = :empreinte")
    Optional<TokenFinPartie> trouverParEmpreinte(@Param("empreinte") String empreinte);

    /** Consommation atomique : une seule requête concurrente obtient 1. */
    @Modifying(flushAutomatically = true)
    @Query("update TokenFinPartie t set t.utilise = true "
            + "where t.id = :id and t.utilise = false and t.expireLe > :maintenant")
    int consommer(@Param("id") Long id, @Param("maintenant") Instant maintenant);

    @Modifying(flushAutomatically = true)
    @Query("update TokenFinPartie t set t.utilise = true where t.partie.id = :partieId and t.utilise = false")
    int invaliderPourPartie(@Param("partieId") Long partieId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TokenFinPartie t where t.partie.id = :partieId")
    void supprimerParPartie(@Param("partieId") Long partieId);

    @Query("select t from TokenFinPartie t where t.partie.id = :partieId and t.utilise = false "
            + "and t.expireLe > :maintenant order by t.expireLe desc")
    List<TokenFinPartie> valides(@Param("partieId") Long partieId, @Param("maintenant") Instant maintenant);
}
