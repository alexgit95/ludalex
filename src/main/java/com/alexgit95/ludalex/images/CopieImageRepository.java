package com.alexgit95.ludalex.images;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CopieImageRepository extends JpaRepository<CopieImage, String> {
    List<CopieImage> findByCleGreaterThanOrderByCle(String cle, Pageable page);
    boolean existsByFichier(String fichier);

    @Query("select c from CopieImage c where c.etat = :etat and (c.prochaineTentative is null or c.prochaineTentative <= :maintenant) order by c.cle")
    List<CopieImage> eligibles(CopieImage.Etat etat, Instant maintenant, Pageable page);

    @Query("select c.etat, count(c), coalesce(sum(c.taille), 0) from CopieImage c group by c.etat")
    List<Object[]> compteurs();
}