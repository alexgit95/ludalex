package com.alexgit95.ludalex.jeux;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface JeuRepository extends JpaRepository<Jeu, Long> {

    /** Verrou en écriture : sérialise les prêts concurrents d'un même jeu. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Jeu j where j.id = :id")
    Optional<Jeu> trouverPourMiseAJour(@Param("id") Long id);
}
