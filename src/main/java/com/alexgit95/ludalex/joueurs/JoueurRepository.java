package com.alexgit95.ludalex.joueurs;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JoueurRepository extends JpaRepository<Joueur, Long> {

    Page<Joueur> findByAnonymiseFalse(Pageable pageable);

    List<Joueur> findByAnonymiseFalseOrderByNomNormaliseAsc();

    boolean existsByNomNormaliseAndAnonymiseFalse(String nomNormalise);

    boolean existsByNomNormaliseAndAnonymiseFalseAndIdNot(String nomNormalise, Long id);
}
